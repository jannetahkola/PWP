import {
    Button,
    Card, CardActionArea,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Grid,
    List,
    TextField, Typography
} from "@mui/material";
import React, {useEffect, useState} from "react";
import {AlertDialog, AlertDialogProps} from "./AlertDialog";

export interface ListDialogProps {
    open: boolean;
    titleText: string,
    listItems: string[],
    onClose?: (value: string | null) => void;
}
interface SearchBarProps {
    items: string[],
    onItemClick: (item: string) => void,
}

const SearchBar = (props: SearchBarProps) => {
    const [searchTerm, setSearchTerm] = useState("");
    const [searchResults, setSearchResults] = useState<string[]>([]);
    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSearchTerm(event.target.value);
    };
    useEffect(() => {
        const results = props.items.filter(item =>
            item.toLowerCase().includes(searchTerm)
        );
        setSearchResults(results);
    }, [searchTerm]);

    return (
        <div>
            <Grid container direction={"column"}>
                <TextField
                    type="text"
                    placeholder="Search"
                    value={searchTerm}
                    onChange={handleChange}
                />
            </Grid>
            <Grid>
                {searchResults.map((item, idx) => (
                    <Grid item key={idx} paddingBottom={1} paddingTop={1}>
                        <Card>
                            <CardActionArea
                                onClick={_ => props.onItemClick(item)}
                                style={{ padding: '1em' }}>
                                <Typography>{item}</Typography>
                            </CardActionArea>
                        </Card>
                    </Grid>
                ))}
            </Grid>
        </div>
    );
}

export function ListDialog(props: Readonly<ListDialogProps>) {
    const { open, titleText, listItems, onClose } = props;
    const [ confirmDialogProps, setConfirmDialogProps ] = useState<AlertDialogProps>({open: false, titleText: "", contentText: ""});

    const handleClose = (value: string | null) => {
        if (onClose) {
            onClose(value);
        }
    };

    const handleConfirmDialogClose = (value: boolean, item: string) => {
        setConfirmDialogProps((currentProps) => {
           const newProps: AlertDialogProps = {
               ...currentProps,
               open: false
           };
           return newProps;
        });
        if (value) {
            // todo
            handleClose(item);
        }
    }

    const searchProps: SearchBarProps = {
        items: listItems,
        onItemClick: (item: string) => {
            setConfirmDialogProps((currentProps) => {
                const newProps: AlertDialogProps = {
                    ...currentProps,
                    open: true,
                    contentText: `Add privilege "${item}"?`,
                    onClose: (value) => handleConfirmDialogClose(value, item),
                };
                return newProps;
            });
        }
    }

    return (
        <Dialog
            open={open}
            onClose={_ => handleClose(null)}
            aria-labelledby="alert-dialog-title"
            aria-describedby="alert-dialog-description"
        >
            <DialogTitle id="alert-dialog-title">
                {titleText}
            </DialogTitle>
            <DialogContent>
                {
                    <List style={{ maxHeight: 400, minHeight: 400, maxWidth: 400, minWidth: 400 }}>
                        <SearchBar {...searchProps}></SearchBar>
                    </List>
                }
            </DialogContent>
            <DialogActions>
                <Button onClick={() => handleClose(null)}>Cancel</Button>
            </DialogActions>
            <AlertDialog {...confirmDialogProps}/>
        </Dialog>
    );
}