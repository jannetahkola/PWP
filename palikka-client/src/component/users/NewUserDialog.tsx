import {
    Button, Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle, FormControl, FormControlLabel,
    Grid,
    TextField
} from "@mui/material";
import React, {FormEvent, useState} from "react";

export interface NewUser {
    username?: string,
    active?: boolean,
    password?: string
}

export interface NewUserDialogProps {
    open: boolean;
    onClose?: (user?: NewUser) => void;
}

export function NewUserDialog(props: Readonly<NewUserDialogProps>) {
    const { open, onClose } = props;
    const [ username, setUsername ] = useState<string>('');
    const [ password, setPassword ] = useState<string>('');
    const [ isActive, setIsActive] = useState<boolean>(true);
    const [ usernameError, setUsernameError ] = useState<string>('');
    const [ passwordError, setPasswordError ] = useState<string>('');

    const validateUsername = (): boolean => {
        if (!username) {
            setUsernameError('Must have a value');
            return false;
        }
        let trim = username.trim();
        if (trim.length < 3 || trim.length > 20) {
            setUsernameError('Must be 3 to 20 characters');
            return false;
        }
        setUsernameError('');
        return true;
    }

    const validatePassword = (): boolean => {
        if (!password) {
            setPasswordError('Must have a value');
            return false;
        }
        let trim = password.trim();
        if (trim.length < 6 || trim.length > 24) {
            setPasswordError('Must be 6 to 24 characters');
            return false;
        }
        setPasswordError('');
        return true;
    }

    const handleClose = (submit: boolean) => {
        if (onClose) {
            if (submit) {
                if (validateUsername() && validatePassword()) {
                    const user: NewUser = {
                        username: username.trim(),
                        password: password.trim(),
                        active: isActive
                    }
                    onClose(user);
                }
            } else {
                onClose(undefined);
            }
        }
    };

    const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        handleClose(true);
    }

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            aria-labelledby="alert-dialog-title"
            aria-describedby="alert-dialog-description"
        >
            <DialogTitle id="alert-dialog-title">
                {"Create a new user"}
            </DialogTitle>
            <DialogContent>
                <form
                    id={"download-form"}
                    onSubmit={handleSubmit}
                    spellCheck={false}
                    autoCorrect={"false"}
                    autoComplete={"false"}
                    autoCapitalize={"false"}
                    noValidate>
                    <Grid container direction={"column"}>
                        <Grid item marginBottom={1}>
                            <FormControl>
                                <TextField
                                    type="text"
                                    placeholder="Username*"
                                    value={username}
                                    onChange={e => {
                                        setUsername(e.target.value);
                                        validateUsername();
                                    }}
                                    required={true}
                                    error={usernameError !== ''}
                                    helperText={usernameError !== '' ? usernameError : '3 to 20 characters'}
                                />
                            </FormControl>
                        </Grid>
                        <Grid item>
                            <FormControl>
                                <TextField
                                    type="text"
                                    placeholder="Password*"
                                    value={password}
                                    onChange={e => {
                                        setPassword(e.target.value);
                                        validatePassword();
                                    }}
                                    required={true}
                                    error={passwordError !== ''}
                                    helperText={passwordError !== '' ? passwordError : '6 to 24 characters'}
                                />
                            </FormControl>
                        </Grid>
                        <Grid item>
                            <FormControlLabel
                                label={"Active"}
                                control={
                                    <Checkbox
                                        checked={isActive}
                                        onChange={() => setIsActive(!isActive)}
                                        tabIndex={-1}
                                        disableRipple
                                    />
                                }/>
                        </Grid>
                    </Grid>
                </form>
            </DialogContent>
            <DialogActions>
                <Button onClick={() => handleClose(false)} autoFocus>Cancel</Button>
                <Button onClick={() => handleClose(true)} disabled={usernameError !== '' || passwordError !== ''}>Ok</Button>
            </DialogActions>
        </Dialog>
    );
}