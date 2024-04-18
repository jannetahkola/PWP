import {createContext, ReactNode, useContext, useMemo, useState} from "react";
import User from "../model/User";

type IAuthContext = {
    token: null | string
    user: null | User
    setUser: (newUser: null | User) => void
    setToken: (newToken: null | string) => void
    setTokenExpiresAt: (newExpiresAt: null | Date) => void
}

const AuthContext = createContext<null | IAuthContext>(null);

type AuthProviderProps = {
    children?: ReactNode
}

const AuthProvider = ({children}: AuthProviderProps) => {
    const [token, setToken] = useState<null | string>(null);
    const [tokenExpiresAt, setTokenExpiresAt] = useState<null | Date>(null);
    const [user, setUser] = useState<null | User>(null);

    return (
        <AuthContext.Provider value={
            useMemo(() => ({
                token,
                setToken,
                user,
                setUser,
                tokenExpiresAt,
                setTokenExpiresAt
            }), [token, setToken, user, setUser, tokenExpiresAt, setTokenExpiresAt])
        }>
            {children}
        </AuthContext.Provider>
    );
}

const useAuthContext = () => {
    const authContext = useContext(AuthContext) as IAuthContext;
    if (!authContext) {
        throw Error("useAuthContext must be used within AuthProvider");
    }
    return authContext;
}

export {AuthProvider, useAuthContext};
