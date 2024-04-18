import {
    Button, CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle, Divider,
    FormControl,
    Grid, IconButton, Input,
    TextField, Tooltip,
    Typography, useTheme
} from "@mui/material";
import PalikkaAPI from "../api/PalikkaAPI";
import React, {FormEvent, useEffect, useState} from "react";
import {useAuthContext} from "../context/AuthContext";
import {Check, Clear, CloudDownload, InfoOutlined, Refresh, UploadFile} from "@mui/icons-material";
import {fakeDelay} from "../util/Utils";
import Config from "../config";

export interface GameFileControlsProps {
    onSuccess: (message: string) => void,
    onError: (message: string) => void
}

type GameDownloadRequestType = {
    download_url: string
}

type GameDownloadResponseType = {
    status: string
}

type GameExecutableMetaType = {
    configured_path: string,
    exists: boolean,
    is_file: boolean
    file_size_mb: number
}

type GameConfig = {
    lastUpdatedAt: null | Date,
    config: string
}

// todo server -> executable
// todo 403 handling here as well
async function downloadServerFile(token: string, downloadUri: string) {
    console.debug('Requesting download..');
    let request = { download_url: downloadUri } as GameDownloadRequestType;
    return await PalikkaAPI.game.file.download(token, request)
        .then(async (res) => {
            if (res.ok) {
                return Promise.resolve(); // Async operation so no response
            }
            if (res.status === 403) {
                return Promise.reject(Error("Session expired, please login again"))
            }
            let json = await res.json();
            return Promise.reject(Error(JSON.stringify(json) ?? 'Server did not respond'));
        })
        .catch(e => Promise.reject(Error(e.message)));
}

async function getDownloadStatus(token: string): Promise<GameDownloadResponseType> {
    let res = await PalikkaAPI.game.file.downloadStatus(token);
    let json = await res.json();
    if (res.ok) {
        return { status: json['status'] } as GameDownloadResponseType;
    }
    return Promise.reject(Error(JSON.stringify(json) ?? 'Server did not respond'));
}

async function getExecutableMetadata(token: string) {
    let res = await PalikkaAPI.game.file.executable.meta(token);
    let json = await res.json();
    if (res.ok) {
        return json as GameExecutableMetaType;
    }
    return Promise.reject(Error(JSON.stringify(json) ?? 'Server did not respond'));
}

async function getConfig(token: string): Promise<string[]> {
    return await PalikkaAPI.game.file.config(token)
        .then(async (res) => {
            let json = await res.json();
            // todo copy this error handling from somewhere where it works and test 403 etc.
            if (!res.ok) {
                if (res.status === 403) {
                    return Promise.reject(Error("Session expired, please login again"));
                }
                return Promise.reject(Error(JSON.stringify(json) ?? 'Server did not respond'));
            }
            return json['config'] as string[];
        })
        .catch(e => Promise.reject(Error(e.message)));
}

function GameFileControls(props: Readonly<GameFileControlsProps>) {
    const theme = useTheme();

    const {token} = useAuthContext();

    const [executableMetadata, setExecutableMetadata] = useState<null | GameExecutableMetaType>(null);

    const [downloadUriText, setDownloadUriText] = useState('');
    const [downloading, setDownloading] = useState(false);
    const [downloadStatus, setDownloadStatus] = useState<null | string>(null);
    const [downloadStatusErrorText, setDownloadStatusErrorText] = useState<string>('');
    const [downloadConfirmationDialogOpen, setDownloadConfirmationDialogOpen] = useState(false);

    const [config, setConfig] = useState<null | GameConfig>(null);
    const [configLoading, setConfigLoading] = useState<boolean>(false);

    const [selectedIconFile, setSelectedIconFile] = useState<null | File>(null);

    const validateDownloadUriText = (): boolean => {
        return downloadUriText.trim() !== '';
    }

    const submitDownloadRequest = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!downloading && validateDownloadUriText()) {
            // todo handle dialog before continuing
            // https://mui.com/material-ui/react-dialog/#confirmation-dialogs
            //setDownloadConfirmationDialogOpen(true);

            setDownloading(true);
            setDownloadStatus("working");

            // todo rewrite
            // todo remove line, just for testing: http://palikka-mock-file-server:8081/files/server
            await downloadServerFile(token!, downloadUriText)
                .then(_ => {
                    let getStatusInterval = setInterval(() => {
                        getDownloadStatus(token!)
                            .then(res => {
                                if (res.status === 'success' || res.status === 'failed') {
                                    clearInterval(getStatusInterval);
                                    setDownloadStatusErrorText('');
                                    setDownloading(false);

                                    refreshConfig(false);
                                    refreshExecutableMetadata();

                                    if (res.status === "failed") {
                                        props.onError("Download failed, status " + res.status);
                                    } else if (res.status === "success") {
                                        props.onSuccess("Download successful");
                                    }
                                }
                                setDownloadStatus(res.status);
                                console.log("download status = " + res.status);
                            })
                            .catch(e => {
                                setDownloadStatus("failed");
                                setDownloadStatusErrorText(`${e.message}`);
                                clearInterval(getStatusInterval);
                                setDownloading(false);
                            });
                    }, 2000);
                })
                .catch(async (e) => {
                    await fakeDelay(500);
                    setDownloading(false);
                    let globalErrorMessage: string;
                    try {
                        let json = JSON.parse(e.message);
                        let message = json["message"];
                        globalErrorMessage = "Download failed - " + (message ?? e.message);
                    } catch (ee) {
                        console.debug("Error message is not JSON: " + e);
                        globalErrorMessage = "Download failed - " + e.message;
                    }
                    props.onError(globalErrorMessage);
                    setDownloadStatus("failed");
                    setDownloadStatusErrorText(`${e.message}`);
                });

            // Start interval if the download API responded without errors, and the current status
            // indicates download is still in progress
            if (downloadStatusErrorText === '') {
                console.debug('Start interval, downloadStatusErrorText=', downloadStatusErrorText);
            } else {
                console.debug('Not starting interval, downloadErrorText = ', downloadStatusErrorText);
            }
        }
    }

    const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        let file = e.target.files?.item(0);
        if (file) {
            setSelectedIconFile(file);
        }
    }

    const submitImageUploadRequest = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (!selectedIconFile) return;

        let formData = new FormData();
        formData.set("file", selectedIconFile);

        // Do not send Content-Type: https://stackoverflow.com/a/49510941
        await fetch(`${Config.urls.gameApi}/game/files/icon`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token!
            },
            body: formData
        }).then(res => {
           if (res.ok) {
               props.onSuccess("File upload successful");
           } else {
               let error = ''
               if (res.status === 403) {
                   error = 'You are not authorized to do this operation';
               } else {
                   error = 'File upload failed, status ' + res.status;
               }
               props.onError(error);
           }
        });
    }

    const refreshConfig = async (silent: boolean) => {
        let configResponse = await getConfig(token!)
            .catch(e => {
                if (!silent) {
                    props.onError(e.message);
                }
            });
        if (configResponse) {
            let gameConfig: GameConfig = {
                config: configResponse.join("\n") ?? "",
                lastUpdatedAt: new Date()
            }
            setConfig(gameConfig);
        }
    };

    const handleConfigValueChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setConfig((prevState) => {
            if (prevState) {
                const newState = {
                    lastUpdatedAt: prevState?.lastUpdatedAt,
                    config: e.target.value
                };
                return newState;
            }
            return prevState;
        });
    }

    const submitConfig = async () => {
        if (!config) return;
        await fetch(`${Config.urls.gameApi}/game/files/config`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                config: config.config
            })
        }).then(async (res) => {
            if (res.ok) {
                props.onSuccess("Config update successful");
                let configResponse = (await res.json())["config"] as string[];
                let gameConfig: GameConfig = {
                    config: configResponse.join("\n") ?? "",
                    lastUpdatedAt: new Date()
                }
                setConfig(gameConfig);
                return;
            }
            let error = '';
            if (res.status === 403) {
                error = 'You are not authorized to do this operation';
            } else {
                error = 'Config upload failed, status ' + res.status;
            }
            props.onError(error);
        }).catch(e => props.onError(e.message));
    }

    const refreshExecutableMetadata = async () => {
        await getExecutableMetadata(token!)
            .then(metadata => {
                setExecutableMetadata(metadata);
            })
            .catch(e => {});
    };

    useEffect(() => {
        refreshConfig(true);
    }, []);

    useEffect(() => {
        refreshExecutableMetadata();
    }, []);

    return (
        <Grid container padding={2}>
            <Grid item marginBottom={4}>
                <Typography variant={"h5"}>Server Files</Typography>
                <Typography variant={"subtitle2"}>Manage server files & configuration</Typography>
            </Grid>
            <Grid container marginBottom={4}>
                <Grid item>
                    <Typography variant={"h6"}>Executable</Typography>
                </Grid>
                <Grid container item>
                    <Grid container>
                        {
                            executableMetadata
                                ?
                                <Grid container rowSpacing={0}>
                                    <Grid
                                        item
                                        container
                                        justifyContent={"space-between"}>
                                        <Grid item>
                                            <Typography>Configured path on host</Typography>
                                        </Grid>
                                        <Grid item>
                                            <Typography>{executableMetadata.configured_path}</Typography>
                                        </Grid>
                                    </Grid>
                                    <Grid
                                        item
                                        container
                                        justifyContent={"space-between"}
                                        alignItems={"center"}>
                                        <Grid item>
                                            <Typography>Exists on host</Typography>
                                        </Grid>
                                        <Grid item>
                                            {
                                                executableMetadata.exists &&
                                                <Tooltip title={"A game server executable exists on the host"}>
                                                    <IconButton>
                                                        <Check color={"success"}/>
                                                    </IconButton>
                                                </Tooltip>
                                            }
                                            {
                                                !executableMetadata.exists &&
                                                <Tooltip title={"No game server executable exists on the host"}>
                                                    <IconButton>
                                                        <Clear color={"error"}/>
                                                    </IconButton>
                                                </Tooltip>
                                            }
                                        </Grid>
                                    </Grid>
                                    <Grid
                                        item
                                        container
                                        justifyContent={"space-between"}>
                                        <Grid item>
                                            <Typography>File size</Typography>
                                        </Grid>
                                        <Grid item>
                                            <Typography>{executableMetadata.file_size_mb} MB</Typography>
                                        </Grid>
                                    </Grid>
                                </Grid>
                                : <div>
                                    { "todo error here" }
                                </div>
                        }
                    </Grid>
                </Grid>
                <Grid container item>
                    <Grid container item alignItems={"center"}>
                        <Typography>Download executable</Typography>
                        <Tooltip title={
                            "The file will be downloaded to the configured path on the host. " +
                            "It is renamed and will override any existing file"
                        }>
                            <IconButton size={"small"}>
                                <InfoOutlined/>
                            </IconButton>
                        </Tooltip>
                    </Grid>
                    <form
                        id={"download-form"}
                        onSubmit={submitDownloadRequest}
                        style={{ width: "100%" }}>
                        <Grid
                            container
                            direction={"row"}
                            alignItems={"center"}>
                            <Grid item xs={11} paddingRight={2}>
                                <FormControl fullWidth>
                                    <TextField
                                        disabled={downloading}
                                        id={"file-url"}
                                        label={"Download URL, e.g. https://test.com"}
                                        value={downloadUriText}
                                        onChange={e => setDownloadUriText(e.target.value)}
                                        size={"small"}/>
                                </FormControl>
                            </Grid>
                            <Grid
                                item
                                container
                                xs={1}
                                justifyContent={"center"}>
                                {
                                    !downloading &&
                                    <Tooltip title={"Download the file into the host"}>
                                        <IconButton
                                            disabled={downloading || downloadUriText.trim() === ''}
                                            type={"submit"}
                                            size={"medium"}>
                                            <CloudDownload/>
                                        </IconButton>
                                    </Tooltip>
                                }
                                { downloading && <CircularProgress size={24}/> }
                            </Grid>
                        </Grid>
                        <Grid
                            container
                            className={"font-subtext"}>
                            <span>{"Download status:"}</span>
                            <span
                                style={{
                                    color:
                                        downloadStatus === 'failed' ? "orangered" :
                                            ( downloadStatus === 'success' ? "green" : ( downloadStatus === 'working' ? "aliceblue" : "#888" )),
                                    whiteSpace: "pre-wrap"
                                }}>
                                {` ${downloadStatus ?? 'Not started'}`}
                            </span>
                        </Grid>
                    </form>
                </Grid>
            </Grid>
            <Grid container marginBottom={4} alignItems={"center"}>
                <Grid item>
                    <Typography variant={"h6"}>Icon</Typography>
                </Grid>
                <Grid container item>
                    <Grid container item alignItems={"center"}>
                        <Typography>Upload icon</Typography>
                        <Tooltip title={
                            "The icon image will be downloaded to the configured path on the host. " +
                            "It is renamed and will override any existing file"
                        }>
                            <IconButton size={"small"}>
                                <InfoOutlined/>
                            </IconButton>
                        </Tooltip>
                    </Grid>
                    <form
                        id={"image-form"}
                        onSubmit={submitImageUploadRequest}
                        style={{ width: "100%" }}>
                        <Grid
                            container
                            direction={"row"}
                            alignItems={"center"}>
                            <Grid item xs={11} paddingRight={2}>
                                <FormControl fullWidth>
                                    <Input
                                        inputProps={{
                                            type: "file",
                                            accept: ".png"
                                        }}
                                        onChange={handleFileInputChange}
                                        disableUnderline={true}/>
                                </FormControl>
                            </Grid>
                            <Grid
                                item
                                container
                                xs={1}
                                justifyContent={"center"}>
                                {
                                    !downloading &&
                                    <Tooltip title={"Upload the file onto the host"}>
                                        <IconButton
                                            disabled={selectedIconFile === null}
                                            type={"submit"}
                                            size={"medium"}>
                                            <UploadFile/>
                                        </IconButton>
                                    </Tooltip>
                                }
                                { downloading && <CircularProgress size={24}/> }
                            </Grid>
                        </Grid>
                    </form>
                </Grid>
            </Grid>
            <Grid container alignItems={"center"}>
                <Grid item>
                    <Typography variant={"h6"}>Configuration</Typography>
                </Grid>
                <Grid item>
                    <Tooltip title={"Refresh the game server configuration"}>
                        <IconButton
                            onClick={_ => {
                                setConfigLoading(true);
                                refreshConfig(false)
                                    .then(_ => fakeDelay(250))
                                    .then(_ => setConfigLoading(false));
                            }}>
                            {
                                configLoading
                                    ? <CircularProgress size={24}/>
                                    : <Refresh/>
                            }
                        </IconButton>
                    </Tooltip>
                </Grid>
            </Grid>
            <Grid item direction={"column"} alignItems={"start"}>
                <Grid item>
                    <span className={"font-subtext"}>
                        Last updated {config?.lastUpdatedAt?.toLocaleString() ?? 'N/A'}
                    </span>
                </Grid>
                {
                    <Grid item>
                        <div
                            id={"config-container"}
                            style={{
                                overflowY: "scroll",
                                height: 300
                            }}>
                            <textarea
                                value={config?.config}
                                onChange={handleConfigValueChange}
                                defaultValue={"Config not found"}
                                rows={17}
                                cols={45}
                                style={{
                                    fontSize: 14,
                                    resize: "horizontal",
                                    color: theme.palette.text.primary,
                                    background: theme.palette.background.default
                                }}/>
                        </div>
                    </Grid>
                }
                <Grid item>
                    <Button onClick={submitConfig}>Save</Button>
                </Grid>
            </Grid>
        </Grid>
    );
}

export default GameFileControls;