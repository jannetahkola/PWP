package fi.jannetahkola.palikka.game.websocket;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Used to access the raw {@link WebSocketSession} on connect/disconnect and sync them in {@link SessionStore}.
 * This is another workaround that is needed for closing the sessions manually to mitigate the issue
 * mentioned in {@link PreSendAuthorizationChannelInterceptor}. {@link SimpUserRegistry} doesn't any
 * functionality to modify sessions.
 * <br><br>
 * Note that STOMP sessions have a different session id from the corresponding {@link WebSocketSession}, so
 * logs for the same session may have two ids.
 */
@Slf4j
@RequiredArgsConstructor
public class SessionBasedWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {
    private final SessionStore sessionStore;

    @Override
    public @Nonnull WebSocketHandler decorate(@Nonnull WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(@Nonnull WebSocketSession session) throws Exception {
                log.debug("Connection established in WS session id={}", session.getId());
                sessionStore.addSession(session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(@Nonnull WebSocketSession session,
                                              @Nonnull CloseStatus closeStatus) throws Exception {
                log.debug("Connection closed in WS session id={}", session.getId());
                sessionStore.removeSession(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }
}
