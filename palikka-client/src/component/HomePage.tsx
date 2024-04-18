import React, {useState} from "react";
import {
    Alert,
    Grid,
    Snackbar
} from "@mui/material";
import GameConsole from "./GameConsole";
import GameStatus from "./GameStatus";
import GameProcessControls from "./GameProcessControls";
import GameFileControls from "./GameFileControls";
import PalikkaAPI from "../api/PalikkaAPI";
import GameStatusType from "../model/GameStatusType";
import {useAuthContext} from "../context/AuthContext";

function HomePage() {
    const { token } = useAuthContext();

    const [successMessage, setSuccessMessage] = useState<string>('');
    const [errorMessage, setErrorMessage] = useState<string>('');
    const [gameStatus, setGameStatus] = useState<GameStatusType>({ online: false });

    const onSuccess = (message: string) => {
        setSuccessMessage(message);
    }

    const onError = (message: string) => {
        setErrorMessage(message);
    }

    // Note that this will never reject promises, all errors are handled through
    // the error handlers in this component.
    const refreshGameStatus = async () => {
        return await PalikkaAPI.game.status(token!)
            .then(async (res) => {
                let gameStatus = res.ok
                    ? await res.json() as GameStatusType
                    : { online: false };
                if (!res.ok) {
                    if (res.status === 403) {
                        setErrorMessage("Session expired, please login again");
                    } else {
                        setErrorMessage("An error occurred, status " + res.status);
                    }
                }
                setGameStatus(gameStatus);
                return Promise.resolve();
            })
            .catch(e => {
                setErrorMessage("An error occurred - " + e.message);
                return Promise.resolve();
            });
    }

    const gameStatusProps = {
        onError: onError,
        gameStatus: gameStatus,
        refreshGameStatus: refreshGameStatus
    };

    const gameProcessControlsProps = {
        onSuccess: onSuccess,
        onError: onError,
        gameStatus: gameStatus,
        refreshGameStatus: refreshGameStatus
    };

    const gameConsoleProps = { onError: onError }

    const gameFileControlsProps =
        { onSuccess: onSuccess, onError: onError };

    return (
        <>
            <Grid
                container
                direction={"row"}
                padding={2}>
                <Grid container md={8} sm={12} alignContent={"start"}>
                    <Grid
                        container
                        padding={2}
                        direction={"row"}
                        justifyContent={"space-between"}>
                        <GameStatus { ...gameStatusProps }/>
                        <GameProcessControls { ...gameProcessControlsProps }/>
                    </Grid>
                    <GameConsole { ...gameConsoleProps }/>
                </Grid>
                <Grid container md={4} sm={12}>
                    <Grid>
                        <GameFileControls { ...gameFileControlsProps }/>
                    </Grid>
                </Grid>
            </Grid>
            <Grid>
                <Snackbar
                    open={errorMessage !== ''}
                    onClose={_ => setErrorMessage('')}
                    autoHideDuration={5000}
                    anchorOrigin={{
                        vertical: 'top',
                        horizontal: 'center'
                    }}>
                    <Alert
                        severity="error"
                        onClose={_ => setErrorMessage('')}>
                        {errorMessage}
                    </Alert>
                </Snackbar>
                <Snackbar
                    open={successMessage !== ''}
                    onClose={_ => setSuccessMessage('')}
                    autoHideDuration={3000}
                    anchorOrigin={{
                        vertical: 'top',
                        horizontal: 'center'
                    }}>
                    <Alert
                        severity="success"
                        onClose={_ => setSuccessMessage('')}>
                        {successMessage}
                    </Alert>
                </Snackbar>
            </Grid>
        </>
    );
}

export default HomePage;