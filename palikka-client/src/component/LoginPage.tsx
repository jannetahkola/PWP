import {
    Alert,
    Button,
    Card, CircularProgress,
    FormControl,
    Grid,
    Snackbar, TextField,
    Typography
} from "@mui/material";
import React, {FormEvent, useEffect, useState} from "react";
import PalikkaAPI from "../api/PalikkaAPI";
import {useAuthContext} from "../context/AuthContext";
import {useNavigate} from "react-router-dom";
import User from "../model/User";
import PalikkaConstants from "../model/PalikkaConstants";
import {fakeDelay} from "../util/Utils";

const tokenKey = PalikkaConstants.storageKeys.token;
const tokenExpiresAt = PalikkaConstants.storageKeys.tokenExpiresAt;

function loadStoredExpiresAtDate() {
    let expiresAtString = localStorage.getItem(tokenExpiresAt);
    // Will be a local date
    return  new Date(Date.parse(expiresAtString!));
}

async function authenticateWithStoredToken(setToken: (token: string) => void,
                                           setTokenExpiresAt: (tokenExpiresAt: Date) => void,
                                           setUser: (user: User) => void): Promise<boolean> {
    console.debug('Authenticating..');
    let token = localStorage.getItem(tokenKey);
    if (!token) {
        console.debug('Abort, no stored authentication');
        return Promise.resolve(false);
    }
    console.debug('Stored authentication found, checking expiry..');
    let expiresAt = loadStoredExpiresAtDate();
    if (new Date() >= expiresAt) {
        console.debug('Stored token is expired');
        localStorage.clear(); // Clear expired token
        return Promise.resolve(false);
    }
    console.debug('Stored authentication still valid, fetching user..');
    return await PalikkaAPI.users.getCurrentUser(token)
        .then(async (res) => {
            if (res.status !== 200) {
                localStorage.clear(); // Clear expired token
                return false;
            }
            const user = await res.json() as User;
            if (!user.id || !user._links) {
                console.error('Server responded with an invalid user - missing required fields');
                localStorage.clear(); // Clear expired token
                return false;
            }
            setToken(token!);
            setTokenExpiresAt(loadStoredExpiresAtDate());
            setUser(user);
            return true;
        })
        .catch(_ => {
            localStorage.clear();
            return false;
        });
}

async function login(username: string, password: string): Promise<boolean> {
    console.debug('Logging in..');
    return await PalikkaAPI.users.login(username, password)
        .then(async (res) => {
            if (!res.ok) {
                return false;
            }
            const json = await res.json();
            localStorage.setItem(tokenKey, json['token']);
            localStorage.setItem(tokenExpiresAt, json['expires_at']);
            return true;
        })
        .catch(_ => false);
}

function LoginPage() {
    const navigate = useNavigate();

    const {token, setToken, user, setUser, setTokenExpiresAt} = useAuthContext();

    const [loading, setLoading] = useState(true);

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    const [usernameHint, setUsernameHint] = useState('');
    const [passwordHint, setPasswordHint] = useState('');

    const [loginInProgress, setLoginInProgress] = useState<boolean>(false);
    const [errorMessage, setErrorMessage] = useState('');

    const resetFormAndShowOptionalError = (errorMessage?: string) => {
        setUsername('');
        setPassword('');
        if (errorMessage) {
            setErrorMessage(errorMessage);
        }
    }

    const validateLoginForm = (): boolean => {
        if (username.trim() === '') {
            setUsernameHint('Invalid username');
        }
        if (password.trim() === '') {
            setPasswordHint('Invalid password');
        }
        return usernameHint === ''
            && passwordHint === ''
            && !loginInProgress;
    }

    const submitLoginForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (validateLoginForm()) {
            setLoginInProgress(true);
            await fakeDelay(500);
            await login(username, password)
                .then(success => {
                    if (!success) {
                        resetFormAndShowOptionalError('Login failed');
                    } else {
                        console.log('Login successful');
                        // Setting the token here to force re-render
                        setToken(localStorage.getItem(tokenKey));
                        setTokenExpiresAt(loadStoredExpiresAtDate());
                    }
                }).finally(() => setLoginInProgress(false));
        }
    }

    useEffect(() => {
        authenticateWithStoredToken(setToken, setTokenExpiresAt, setUser).then(success => {
            console.log('Authentication success: ', success);
            if (success) {
                navigate('/');
            } else {
                resetFormAndShowOptionalError();
            }
            setLoading(false);
        });
    }, [token, setToken, user, setUser, navigate]);

    let loadingComponent =
        <Grid item>
            <CircularProgress/>
        </Grid>;

    let loginComponent =
        <Card
            sx={{
                padding: 8
            }}>
           <Grid container direction={"column"} alignItems={"center"}>
               <Typography variant='h3'>PALIKKA</Typography>
               <Typography variant='h6'>Game Dashboard</Typography>
           </Grid>
            <Grid item>
                <form onSubmit={submitLoginForm}>
                    <Grid container direction='column'>
                        <FormControl
                            required={true}
                            variant={"standard"}>
                            <TextField
                                id={"username"}
                                onChange={e => setUsername(e.target.value)}
                                onFocus={() => setUsernameHint('')}
                                value={username}
                                error={usernameHint !== ''}
                                label={"Username"}
                                helperText={usernameHint}
                                variant={"standard"}
                                disabled={loginInProgress}
                            />
                        </FormControl>
                        <FormControl
                            required={true}
                            variant={"standard"}>
                            <TextField
                                id={"password"}
                                type={"password"}
                                onChange={e => setPassword(e.target.value)}
                                onFocus={() => setPasswordHint('')}
                                value={password}
                                error={passwordHint !== ''}
                                label={"Password"}
                                helperText={passwordHint}
                                variant={"standard"}
                                disabled={loginInProgress}
                            />
                        </FormControl>
                        <Grid item paddingTop={2}>
                            <Button
                                disabled={loginInProgress}
                                variant={"contained"}
                                type={"submit"}
                                fullWidth>
                                {loginInProgress ? "Logging in..." : 'Log In'}
                            </Button>
                        </Grid>
                    </Grid>
                </form>
            </Grid>
            <Snackbar
                open={errorMessage !== ''}
                onClose={_ => setErrorMessage('')}
                autoHideDuration={3000}
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
        </Card>;

    return (
        <Grid
            container
            paddingTop={8}
            paddingBottom={2}
            direction={"column"}
            alignItems={"center"}>
            {loading ? loadingComponent : loginComponent}
        </Grid>
    );
}

export default LoginPage;