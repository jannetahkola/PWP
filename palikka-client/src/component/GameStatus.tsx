import {Box, CircularProgress, Divider, Grid, IconButton, Tooltip} from "@mui/material";
import {useAuthContext} from "../context/AuthContext";
import {useEffect, useState} from "react";
import GameStatusType from "../model/GameStatusType";
import {Check, Clear, Refresh} from "@mui/icons-material";
import {fakeDelay} from "../util/Utils";

export interface GameStatusProps {
    onError: (message: string) => void;
    gameStatus: GameStatusType;
    refreshGameStatus: () => Promise<void>;
}

// todo refresh icon should probably come from parent and update both game and process status
function GameStatus(props: Readonly<GameStatusProps>) {
    const { token } = useAuthContext();
    const [gameStatusLastUpdatedAt, setGameStatusLastUpdatedAt] = useState<null | Date>(null);
    const [loading, setLoading] = useState<boolean>(false);

    let gameStatus = props.gameStatus;

    const refreshGameStatus = () => {
        setLoading(true);
        setGameStatusLastUpdatedAt(new Date());
        props.refreshGameStatus()
            .finally(() => {
                fakeDelay(500)
                    .then(_ => setLoading(false));
            });
    };

    useEffect(() => {
        const fetchGameStatus = async () => {
            await props.refreshGameStatus()
                .then(_ => setGameStatusLastUpdatedAt(new Date()));
        };
        fetchGameStatus();
    }, []);

    return (
        <Grid item direction={"row"}>
            <Grid>
                <Grid
                    container
                    alignItems={"center"}>
                    <Grid marginRight={1}>
                        <img
                            src={gameStatus.favicon}
                            alt={""}
                            style={{
                                height: "32px",
                                width: "32px",
                                backgroundColor: "transparent"
                            }}
                        />
                    </Grid>
                    <Grid
                        item
                        style={{
                            fontSize: '22px',
                            fontWeight: '600'
                        }}>
                        { gameStatus?.host ?? 'N/A' }
                    </Grid>
                    <div style={{ width: 8 }}></div>
                    <Grid item>
                        {
                            gameStatus?.online &&
                            <Tooltip title={"Game is online"}>
                                <IconButton>
                                    <Check color={"success"}/>
                                </IconButton>
                            </Tooltip>
                        }
                        {
                            !gameStatus?.online &&
                            <Tooltip title={"Game is offline"}>
                                <IconButton>
                                    <Clear color={"error"}/>
                                </IconButton>
                            </Tooltip>
                        }
                    </Grid>
                    <Grid item>
                        <Tooltip title={"Refresh game status"}>
                            <IconButton onClick={_ => refreshGameStatus()}>
                                {
                                    loading
                                        ? <CircularProgress size={24}/>
                                        : <Refresh/>
                                }
                            </IconButton>
                        </Tooltip>
                    </Grid>
                </Grid>
                <Grid
                    container
                    direction={"column"}>
                    <Grid item>{gameStatus.description}</Grid>
                    <Grid item>Version {gameStatus?.online ? gameStatus?.version : 'N/A'}</Grid>
                    <Grid item>
                        Players - {
                        gameStatus?.players
                            ? `${gameStatus.players.online} / ${gameStatus.players.max}`
                            : 'N/A'
                    }
                    </Grid>
                </Grid>
                <Grid item>
                <span className={ "font-subtext" }>
                    Last updated { gameStatusLastUpdatedAt?.toLocaleString() ?? 'N/A' }
                </span>
                </Grid>
            </Grid>
        </Grid>
    );
}

export default GameStatus;