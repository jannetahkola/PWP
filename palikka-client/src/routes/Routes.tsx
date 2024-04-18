import {Navigate, Outlet, Route, Routes as Router} from "react-router-dom";
import LoginPage from "../component/LoginPage";
import {useAuthContext} from "../context/AuthContext";
import HomePage from "../component/HomePage";
import UserManagementPage from "../component/users/UserManagementPage";

function SecureRoutes() {
    const { user } = useAuthContext();

    if (user == null) {
        console.log('No authentication context, navigating to login...');
        return <Navigate to={"/login"} replace/>
    }

    console.log('Authentication context present, navigating to outlet');
    return <Outlet/>
}

function Routes() {
    return (
        <Router>
            <Route path={"/login"} element={<LoginPage/>}/>
            <Route element={<SecureRoutes/>}>
                <Route path={"/users"} element={<UserManagementPage/>}/>
                <Route path={"/"} element={<HomePage/>}/>
            </Route>
        </Router>
    );
}

export default Routes;