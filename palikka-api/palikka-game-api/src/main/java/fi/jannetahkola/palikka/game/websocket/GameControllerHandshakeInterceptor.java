package fi.jannetahkola.palikka.game.websocket;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
public class GameControllerHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   @Nonnull ServerHttpResponse response,
                                   @Nonnull WebSocketHandler wsHandler,
                                   @Nonnull Map<String, Object> attributes) {
//        log.debug("Received handshake request {} {}", request.getMethod(), request.getURI().getPath());
//        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
//            log.debug("Aborting handshake request due to invalid/missing token");
//            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            return false;
//        }
//        attributes.put("token", header.replace("Bearer ", ""));
        return true;
    }

    @Override
    public void afterHandshake(@Nonnull ServerHttpRequest request,
                               @Nonnull ServerHttpResponse response,
                               @Nonnull WebSocketHandler wsHandler,
                               Exception exception) {
        log.debug("Completed handshake request {} {}", request.getMethod(), request.getURI().getPath());
    }
}
