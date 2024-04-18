import React, {useState} from 'react';
import './App.css';
import {BrowserRouter, useNavigate} from "react-router-dom";
import Routes from "./routes/Routes";
import {AuthProvider, useAuthContext} from "./context/AuthContext";
import {
    AppBar,
    Button, createTheme,
    CssBaseline,
    ListItemIcon, ListItemText, Menu,
    MenuItem, ThemeProvider,
    Toolbar,
    Typography
} from "@mui/material";
import {ArrowDropDown, ExitToApp, SupervisedUserCircle, VideogameAsset} from "@mui/icons-material";
import Footer from "./component/Footer";
import {StompProvider, useStompContext} from "./context/StompContext";

const darkTheme = createTheme({
    palette: {
        mode: 'dark',
    },
});

const titleGameDashboard = "Game Dashboard";
const titleUserManagement = "User Management";

function AppContent() {
    const navigate = useNavigate();
    const { user, token, setUser, setToken } = useAuthContext();
    const { stompClient, setStompClient } = useStompContext();

    const [pageTitle, setPageTitle] = useState<string>(titleGameDashboard);
    const [loading, setLoading] = useState(false);
    const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);

    // This needs to be called anytime we navigate away from the game dashboard
    const deactivateStompClient = async () => {
        console.debug("Cleaning up STOMP client");
        await stompClient?.deactivate(); // subscriptions handled in disconnected callback
        setStompClient(null);
    }

    const logout = async () => {
        console.debug("Logging out");
        setLoading(true);

        await deactivateStompClient();

        console.debug("Cleaning local storage");
        localStorage.clear();

        console.debug("Cleaning relevant React state(s)");
        setToken(null);
        setUser(null);
        setMenuAnchor(null);
        setLoading(false);

        navigate('/');
    };

    return (
        <>
            <AppBar
                position={"static"}
                style={{ justifyContent: "center" }}>
                <Toolbar
                    variant={"dense"}
                    style={{justifyContent: 'space-between'}}>
                    <Typography variant={"h6"}>PALIKKA</Typography>
                    { token && <Typography variant={"subtitle1"}>{ pageTitle }</Typography> }
                    { token && <Button
                        id={"menu-button"}
                        onClick={e => setMenuAnchor(e.currentTarget)}
                        endIcon={<ArrowDropDown/>}
                        style={{color: 'white'}}>
                        {user?.username}
                    </Button> }
                    { token && <Menu
                        id={"menu"}
                        open={Boolean(menuAnchor)}
                        onClose={_ => setMenuAnchor(null)}
                        anchorEl={menuAnchor}>
                        <MenuItem
                            disabled={loading}
                            onClick={_ => {
                                setPageTitle(titleGameDashboard)
                                navigate("/");
                                setMenuAnchor(null);
                            }}>
                            <ListItemIcon>
                                <VideogameAsset/>
                            </ListItemIcon>
                            <ListItemText>
                                {"Game"}
                            </ListItemText>
                        </MenuItem>
                        <MenuItem
                            disabled={loading}
                            onClick={async (_) => {
                                await deactivateStompClient();
                                setPageTitle(titleUserManagement)
                                navigate("/users");
                                setMenuAnchor(null);
                            }}>
                            <ListItemIcon>
                                <SupervisedUserCircle/>
                            </ListItemIcon>
                            <ListItemText>
                                {"Users"}
                            </ListItemText>
                        </MenuItem>
                        <MenuItem
                            disabled={loading}
                            onClick={logout}>
                            <ListItemIcon>
                                <ExitToApp/>
                            </ListItemIcon>
                            <ListItemText>
                                {"Log out"}
                            </ListItemText>
                        </MenuItem>
                    </Menu> }
                </Toolbar>
            </AppBar>
            <Routes/>
            <Footer/>
        </>
    );
}

function App() {
  return (
      <ThemeProvider theme={darkTheme}>
          <CssBaseline/>
          <BrowserRouter>
              <AuthProvider>
                  <StompProvider>
                      <AppContent/>
                  </StompProvider>
              </AuthProvider>
          </BrowserRouter>
      </ThemeProvider>
  );
}

export default App;
