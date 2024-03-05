package fi.jannetahkola.palikka.game.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
public class SessionInterceptor {

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        log.debug("Connected session [{}]", getSessionId());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.debug("Disconnected session [{}]", getSessionId());
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        log.debug("Subscribed session [{}]", getSessionId());
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        log.debug("Unsubscribed session [{}]", getSessionId());
    }

    private String getSessionId() {
        return SimpAttributesContextHolder.currentAttributes().getSessionId();
    }
}
