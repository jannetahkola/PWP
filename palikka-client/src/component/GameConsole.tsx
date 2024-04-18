import {FormEvent, useEffect, useState} from "react";
import {Client, IFrame, IMessage, StompSubscription} from "@stomp/stompjs";
import {Box, Button, CircularProgress, Grid, IconButton, Tooltip, Typography, useTheme} from "@mui/material";
import {useAuthContext} from "../context/AuthContext";
import {useStompContext} from "../context/StompContext";
import {Check, Clear, InfoOutlined, Refresh} from "@mui/icons-material";
import {fakeDelay} from "../util/Utils";

const doLog: boolean = true;

export interface GameConsoleProps {
    onError: (message: string) => void;
}

interface GameUserReplyMessage {
    type: string,
    data: string
}

interface GameLogMessage {
    data: string
}

interface GameLifecycleMessage {
    data: string
}

interface ConsoleLine {
    color: string,
    data: string
}

function GameConsole(props: Readonly<GameConsoleProps>) {
    const theme = useTheme();
    const { token, user} = useAuthContext();
    const {
        stompClient, setStompClient,
        stompSubscriptions, setStompSubscriptions,
        resetStompContext
    } = useStompContext();

    const [loading, setLoading] = useState(false);

    const [gameUserReplyMessages, setGameUserReplyMessages] = useState<any[]>([]);
    const [gameLogMessages, setGameLogMessages] = useState<any[]>([]);
    const [gameLifecycleMessages, setGameLifecycleMessages] = useState<any[]>([]);

    const [consoleInput, setConsoleInput] = useState('');

    const canSubmitStompMessage = (): boolean => {
        return consoleInput.trim() !== '';
    }

    const submitStompMessage = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (canSubmitStompMessage()) {
            const input = consoleInput;
            setConsoleInput('');
            console.log("Sending message: ", input);
            stompClient?.publish({
                destination: '/app/game',
                body: JSON.stringify({ data: input })
            });
        }
    }

    // const addGameUserReplyMessage = (message: GameUserReplyMessage) => {
    //     if (message.type === 'hist') {
    //         let mappedConsoleLines = message.data.split("\n")
    //             .map((logLine, idx) => {
    //                 return {
    //                     color: "white",
    //                     data: logLine
    //                 };
    //             });
    //         setConsoleLines((currentLines) => ([
    //             ...consoleLines,
    //             ...mappedConsoleLines
    //         ]));
    //     } else {
    //         let color = "white";
    //         if (message.type === 'err') {
    //             color = "#ff7961";
    //         }
    //         setConsoleLines((currentLines) => ([
    //             ...consoleLines,
    //             { color: color, data: message.data }
    //         ]));
    //     }
    //     scrollConsole();
    // };
    //
    // const addGameLogMessage = (message: GameLogMessage) => {
    //     setConsoleLines((currentLines) => ([
    //         ...consoleLines,
    //         { color: "white", data: message.data }
    //     ]));
    //     scrollConsole();
    // }
    //
    // const addGameLifecycleMessage = (message: GameLifecycleMessage) =>
    //     setGameLifecycleMessages([...gameLifecycleMessages, message]);

    const scrollConsole = () => {
        // todo track scroll on the div so that if scrolled up manually, below isn't triggered until scrolled back down.
        let logContainerEl = document.querySelector("#log-container");
        if (logContainerEl) {
            const scroll = logContainerEl.scrollHeight - logContainerEl.clientHeight;
            //console.debug(`scrollHeight=${logContainerEl.scrollHeight}, clientHeight=${logContainerEl.clientHeight}, scroll=${scroll}`);
            logContainerEl.scrollTo({ top: scroll });
        }
    }

    const handleWebsocketUserReplyMessage = (rawMessage: IMessage) => {
        const json = JSON.parse(rawMessage.body);
        const message: any = {
            type: json['typ'] as string,
            data: json['data'] as string
        };
        if (message.type === 'hist') {
            if (message.data === '') {
                return;
            }
            const mappedLogMessages = message.data.split("\n")
                .map((data: string) => ({ data: data }));
            setGameLogMessages((currentMessages) => {
                let gameLogMessagesCopy = [...currentMessages];
                gameLogMessagesCopy.push(...mappedLogMessages);
                return gameLogMessagesCopy;
            });
        } else {
            const logMessage = {
                color: message.type === "err"
                    ? theme.palette.error.dark
                    : "white",
                data: message.data
            };
            console.log("messgae="+JSON.stringify(logMessage));
            setGameLogMessages((currentMessages) => {
                let gameLogMessagesCopy = [...currentMessages];
                gameLogMessagesCopy.push(logMessage);
                return gameLogMessagesCopy;
            });
        }
    }

    const handleWebsocketGameLogMessage = (rawMessage: IMessage) => {
        const json = JSON.parse(rawMessage.body);
        const message: any = { data: json['data'] as string }
        setGameLogMessages((currentMessages) => {
            let gameLogMessagesCopy = [...currentMessages];
            gameLogMessagesCopy.push(message);
            return gameLogMessagesCopy;
        });
    }

    const handleWebSocketLifecycleMessage = (rawMessage: IMessage) => {
        const json = JSON.parse(rawMessage.body);
        const message: any = { data: json['data'] as string };
        // setGameLifecycleMessages(() => ([
        //     ...gameLifecycleMessages,
        //     message
        // ]));
    }

    const handleWebsocketMessage = (rawMessage: IMessage) => {
        const destination = rawMessage.headers["destination"] as any;
        if (!destination) {
            console.error("STOMP message missing destination, skipping");
            return;
        }
        switch (destination as string) {
            case "/user/queue/reply":
                if (doLog) console.debug("Received message to /user/queue/reply, body=", rawMessage.body);
                handleWebsocketUserReplyMessage(rawMessage);
                break;
            case "/topic/game/logs":
                if (doLog) console.debug("Received message to /topic/game/logs, body=", rawMessage.body);
                handleWebsocketGameLogMessage(rawMessage);
                break;
            case "/topic/game/lifecycle":
                if (doLog) console.debug("Received message to /topic/game/lifecycle, body=", rawMessage.body);
                handleWebSocketLifecycleMessage(rawMessage);
                break;
            default:
        }
    }

    const createStompSubscriptions = (stompClient: Client): StompSubscription[] => {
        console.debug("STOMP: Creating new subscriptions");
        const gameUserReplySubscription = stompClient.subscribe(
            "/user/queue/reply",
            rawMessage => handleWebsocketMessage(rawMessage),
            { id: "user-reply-sub" });
        const gameLogsSubscription = stompClient.subscribe(
            "/topic/game/logs",
            rawMessage => handleWebsocketMessage(rawMessage),
            { id: "game-logs-sub" });
        const gameLifecycleSubscription = stompClient.subscribe(
            "/topic/game/lifecycle",
            rawMessage => handleWebsocketMessage(rawMessage),
            { id: "game-lifecycle-sub" });
        console.debug("STOMP: Subscriptions created")
        return [
            gameUserReplySubscription,
            gameLogsSubscription,
            gameLifecycleSubscription
        ];
    }

    const initStompClient = (): Client => {
        console.debug("STOMP: Creating new client");
        return new Client({
            brokerURL: "ws://localhost:8082/game-api/ws?token=" + token,
            connectionTimeout: 5000,
            // No reconnect for now
            reconnectDelay: 0
        });
    }

    useEffect(() => scrollConsole(), [gameLogMessages]);

    useEffect(() => {
        if (!stompClient) {
            const newClient = initStompClient();
            newClient.onConnect = () => {
                console.debug("STOMP: Connected");
                setStompSubscriptions(createStompSubscriptions(newClient));
            };
            newClient.onDisconnect = () => {
                console.debug("STOMP: Disconnected");
                stompSubscriptions.forEach(subscription => subscription.unsubscribe());
            };
            newClient.onWebSocketClose = () => {
                // todo status icon not updated when disconnected in the background
                console.debug("STOMP: WebSocket closed");
                // Here we only unsubscribe - no client reset to not trigger this effect infinitely
                stompSubscriptions.forEach(subscription => subscription.unsubscribe());
                console.debug("STOMP: Subscriptions cancelled due to disconnect");
            };
            newClient.onWebSocketError = (e) => {
                console.debug("STOMP: WebSocket error occurred", e);
                props.onError("An error occurred in console connection");
            };
            newClient.onStompError = (frame: IFrame) => {
                console.debug("STOMP: STOMP error occurred", frame.headers['message']);
            };
            newClient.activate();
            setStompClient(newClient);
        }
    }, []);

    useEffect(() => {
        return () => {
            // Not sure if this is needed.
            // STOMP client cannot be disconnected onUnmount because it is an async operation, but at least
            // we are not subscribed to the old session anymore.
            // todo disconnect client on navigation etc.
            // the only case where we leave the old subscription alive is when we navigate. Page refresh is ok!
            console.debug("CLEAN UP");
            resetStompContext();
            console.debug("CLEAN UP DONE");
        };
    }, []);

    return (
        <Grid
            xs={12}
            direction={"column"}
            padding={2}>
            <Grid item container direction={"row"}>
                <Grid item container direction={"row"} xs={11} alignItems={"center"}>
                    <Typography variant={"h5"}>Console</Typography>
                    <div style={{width: 8}}></div>
                    {
                        stompClient?.connected &&
                        <Tooltip title={"The console is connected successfully"}>
                            <IconButton>
                                <Check color={"success"}/>
                            </IconButton>
                        </Tooltip>
                    }
                    {
                        !stompClient?.connected &&
                        <Tooltip title={"The console is not connected"}>
                            <IconButton>
                                <Clear color={"error"}/>
                            </IconButton>
                        </Tooltip>
                    }
                    <Tooltip title={"Refresh the console's connection"}>
                        <IconButton onClick={async (_) => {
                            setLoading(true);
                            setGameLogMessages([]);
                            await fakeDelay(500);
                            // Trigger onWebSocketClosed callback and re-activate
                            stompClient?.deactivate()
                                .then(_ => stompClient?.activate())
                                .finally(() => setLoading(false));
                        }}>
                            { !loading && <Refresh/> }
                            { loading && <CircularProgress size={24}/> }
                        </IconButton>
                    </Tooltip>
                </Grid>
                <Grid item container direction={"row"} justifyContent={"end"} xs={1}>
                    <Tooltip title={
                        "The console provides a direct real-time connection to the game server's I/O streams." +
                        " If the server is not running, the console is empty and inputs do not have any effect"
                    }>
                        <IconButton size={"small"}>
                            <InfoOutlined/>
                        </IconButton>
                    </Tooltip>
                </Grid>
            </Grid>
            <Grid item xs={12}>
                <Grid item xs={12}>
                    <div
                        id={"log-container"}
                        className={"font-console bg-color-console"}
                        style={{
                            overflowY: "scroll",
                            minHeight: 400,
                            maxHeight: 400
                        }}>
                        {
                            gameLogMessages.map((line, idx) => (
                                <div
                                    key={idx}
                                    style={{
                                        color: line.color,
                                        width: "100%",
                                        display: "inline-block",
                                        paddingTop: "2px",
                                        paddingBottom: "2px"
                                    }}>
                                    {`${line.data}`}
                                </div>
                            ))
                        }
                        {
                            gameLogMessages.length === 0 &&
                            <div style={{
                                color: "#888"
                            }}>
                                No messages yet
                            </div>
                        }
                    </div>
                </Grid>
                <Grid item xs={12}>
                    <form
                        id={"form-console"}
                        onSubmit={submitStompMessage}
                        spellCheck={false}
                        autoCorrect={"false"}
                        autoComplete={"false"}
                        autoCapitalize={"false"}
                        noValidate>
                        <Grid
                            item
                            className={"font-console"}
                            xs={12}>
                            <div style={{ display: "flex" }}>
                                <div style={{marginRight: 8}}>
                                    {user?.username}@palikka:~$
                                </div>
                                <input
                                    form={"form-console"}
                                    className={"input-no-border font-console bg-color-console"}
                                    style={{width: "100%"}}
                                    value={consoleInput}
                                    onChange={e => setConsoleInput(e.target.value)}/>
                                <input
                                    form={"form-console"}
                                    disabled={consoleInput.trim() === ''}
                                    className={"font-console bg-color-console"}
                                    style={{fontSize: 12, padding: 2}}
                                    type={"submit"}/>
                            </div>
                        </Grid>
                    </form>
                </Grid>
            </Grid>
        </Grid>
    )
}

export default GameConsole;