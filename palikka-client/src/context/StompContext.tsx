import {createContext, ReactNode, useContext, useState} from "react";
import {Client, StompSubscription} from "@stomp/stompjs";

type IStompContext = {
    stompClient: null | Client,
    setStompClient: (newClient: null | Client) => void,
    stompSubscriptions: StompSubscription[],
    setStompSubscriptions: (newSubscriptions: StompSubscription[]) => void,
    resetStompContext: () => void
}

const StompContext = createContext<null | IStompContext>(null);

type StompProviderProps = {
    children?: ReactNode
}

const StompProvider = ({ children }: StompProviderProps) => {
    const [stompClient, setStompClient] = useState<null | Client>(null);
    const [stompSubscriptions, setStompSubscriptions] = useState<StompSubscription[]>([])

    // Set STOMP client to null here. Due to issues with state management for the client & its subscriptions, it's best
    // to just unsubscribe and recreate the client instead of re-activating it.
    const resetStompContext = () => {
        stompSubscriptions.forEach(subscription => subscription.unsubscribe);
        setStompClient(null);
        console.debug("STOMP: Subscriptions cancelled & client reset");
    }

    return (
        <StompContext.Provider
            value={{
                stompClient, setStompClient,
                stompSubscriptions, setStompSubscriptions,
                resetStompContext
            }}>
            { children }
        </StompContext.Provider>
    );
}

const useStompContext = () => {
    const stompContext = useContext(StompContext) as IStompContext;
    if (!stompContext) {
        throw Error("useStompContext must be used with StompProvider");
    }
    return stompContext;
}

export { StompProvider, useStompContext };