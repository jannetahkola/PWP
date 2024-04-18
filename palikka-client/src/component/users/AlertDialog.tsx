import React from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from "@mui/material";

export interface AlertDialogProps {
    open: boolean;
    titleText: string,
    contentText: string,
    onClose?: (value: boolean) => void;
}

export function AlertDialog(props: Readonly<AlertDialogProps>) {
    const { open, titleText, contentText, onClose } = props;

    const handleClose = (value: boolean) => {
        if (onClose) {
            onClose(value);
        }
    };

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            aria-labelledby="alert-dialog-title"
            aria-describedby="alert-dialog-description"
        >
            <DialogTitle id="alert-dialog-title">
                {titleText}
            </DialogTitle>
            <DialogContent>
                {
                    contentText.split('\n').map((line, i) => (
                        <DialogContentText key={i} className={"id-alert-dialog-description"}>
                            {line}
                        </DialogContentText>
                    ))
                }
            </DialogContent>
            <DialogActions>
                <Button onClick={() => handleClose(false)} autoFocus>Cancel</Button>
                <Button onClick={() => handleClose(true)}>Ok</Button>
            </DialogActions>
        </Dialog>
    );
}