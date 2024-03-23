package fi.jannetahkola.palikka.game.websocket;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Does nothing, here for reference.
 */
@Slf4j
@RequiredArgsConstructor
public class GameControllerHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    public Principal determineUser(@Nonnull ServerHttpRequest request,
                                   @Nonnull WebSocketHandler handler,
                                   @Nonnull Map<String, Object> attributes) {
        return super.determineUser(request, handler, attributes);
    }
}
