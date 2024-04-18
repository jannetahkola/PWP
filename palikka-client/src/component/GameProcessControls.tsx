import {Button, CircularProgress, Grid, Tooltip} from "@mui/material";
import PalikkaAPI from "../api/PalikkaAPI";
import GameProcessControlRequestType from "../model/GameProcessControlRequestType";
import {useAuthContext} from "../context/AuthContext";
import {useEffect, useState} from "react";
import {fakeDelay} from "../util/Utils";
import {PlayArrow, Stop} from "@mui/icons-material";

// todo rename 'server' to 'process'
// todo make everything as "loading" while the initial requests are being made

const executableStatusStarting = 'starting';
const executableStatusUp = 'up';
const executableStatusStopping = 'stopping';
const executableStatusDown = 'down';
const executableStatusIntervalInMillis = 2000; // How often the status is fetched

export interface GameProcessControlsProps {
    onSuccess: (message: string) => void;
    onError: (message: string) => void;
    refreshGameStatus: () => Promise<void>;
}

interface ServerStatusTypeWrapper {
    status?: string,
    error?: string
}

async function handleErrorResponse(res: Response): Promise<string> {
    if (res.status === 400) {
        let json = await res.json();
        return Promise.reject(Error(json["message"]));
    }
    if (res.status === 403) {
        return Promise.reject(Error("You are not authorized to do this operation"));
    }
    return Promise.reject(Error("An error occurred"));
}

async function doProcessControlRequest(token: string, request: GameProcessControlRequestType): Promise<void | string> {
    return await PalikkaAPI.game.process.control(token, request)
        .then(async (res) => {
            if (!res.ok) {
                return await handleErrorResponse(res);
            }
            return Promise.resolve(); // Async operation so no response
        })
        .catch(e => Promise.reject(Error("An error occurred - " + e.message)));
}

async function startServer(token: string): Promise<void | string> {
    let request: GameProcessControlRequestType = { action: "start" };
    return await doProcessControlRequest(token, request);
}

async function stopServer(token: string): Promise<void | string> {
    let request: GameProcessControlRequestType = { action: "stop" };
    return await doProcessControlRequest(token, request);
}

async function getExecutableStatus(token: string): Promise<ServerStatusTypeWrapper> {
    return await PalikkaAPI.game.process.status(token)
        .then(async (res) => {
            if (!res.ok) {
                if (res.status === 403) {
                    // todo
                    //showErrorMessage("Session expired, please login again");
                }
                return {
                    status: executableStatusDown,
                    error: 'Failed to get status'
                };
            }
            const json = await res.json();
            return { status: json['status'] };
        })
        .catch(_ => {
            return {
                status: executableStatusDown,
                error: 'Failed to get status'
            };
        });
}

// todo this needs to also trigger game status fetch
// todo and this also needs to react to events when interval is not set (not game status, process is a deeper concept)
function GameProcessControls(props: Readonly<GameProcessControlsProps>) {
    const {token} = useAuthContext();

    const [loading, setLoading] = useState(false);

    const [executableStatus, setExecutableStatus] = useState<null | string>();

    const setExecutableStatusFetchInterval = (waitForStatus: string) => {
        console.debug(`Fetch executable status interval (${executableStatusIntervalInMillis} ms) SET`);
        const getStatusInterval = setInterval(() => {
          getExecutableStatus(token!)
              .then(async (status) => {
                  if (status?.status === waitForStatus) {
                      await props.refreshGameStatus();
                      clearInterval(getStatusInterval);
                      setLoading(false);
                      if (waitForStatus === executableStatusUp) {
                          props.onSuccess("Game started successfully");
                      } else if (waitForStatus === executableStatusDown) {
                          props.onSuccess("Game stopped successfully");
                      }
                  }
                  if (status?.status) {
                      setExecutableStatus(status.status);
                  }
                  // todo handle errors
              }).catch(_ => {
                  clearInterval(getStatusInterval);
                  setLoading(false);
              });
        }, executableStatusIntervalInMillis);
    };

    useEffect(() => {
        const fetchGameStatus = async () => {
            await getExecutableStatus(token!)
                .then(status => setExecutableStatus(status.status))
                .catch(e => {});
        };
        fetchGameStatus();
    }, []);

    return (
        <Grid item>
            <Grid
                container
                direction={"column"}>
                { (executableStatus === executableStatusDown || executableStatus === executableStatusStopping)
                    &&
                    <Tooltip title={"Start the game process"}>
                        <Button
                            size={"large"}
                            variant={"contained"}
                            color={"success"}
                            disabled={loading}
                            onClick={async (_) => {
                                // Loading status needs to be set immediately here
                                setLoading(true);
                                await fakeDelay(500);
                                startServer(token!)
                                    .then(_ => setExecutableStatusFetchInterval(executableStatusUp))
                                    .catch(e => {
                                        props.onError(e.message);
                                        setLoading(false);
                                    });
                            }}>
                            {loading ? <CircularProgress size={24}/> : <PlayArrow/>}
                        </Button>
                    </Tooltip>
                }
                { (executableStatus === executableStatusUp || executableStatus === executableStatusStarting)
                    &&
                    <Tooltip title={"Stop the game process"}>
                        <Button
                            size={"large"}
                            variant={"contained"}
                            color={"error"}
                            disabled={loading}
                            onClick={async (_) => {
                                setLoading(true);
                                await fakeDelay(500);
                                stopServer(token!)
                                    .then(_ => setExecutableStatusFetchInterval(executableStatusDown))
                                    .catch(e => {
                                        props.onError(e.message);
                                        setLoading(false);
                                    });
                            }}>
                            {loading ? <CircularProgress size={24}/> : <Stop/>}
                        </Button>
                    </Tooltip>
                }
            </Grid>
        </Grid>
    );
}

export default GameProcessControls;