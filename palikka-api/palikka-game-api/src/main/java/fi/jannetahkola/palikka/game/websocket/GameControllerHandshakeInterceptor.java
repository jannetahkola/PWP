package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.core.util.AuthenticationUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class GameControllerHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;
    private final UsersClient usersClient;

    @PostConstruct
    void postConstruct() {
        log.info("----- Handshake interceptor ENABLED ----");
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   @Nonnull ServerHttpResponse response,
                                   @Nonnull WebSocketHandler wsHandler,
                                   @Nonnull Map<String, Object> attributes) {
        log.debug("Received handshake request {} {}", request.getMethod(), request.getURI().getPath());
        if (request.getURI().getQuery() != null && request.getURI().getQuery().contains("token=")) {
            log.debug("Using token from request query");
            String token = request.getURI().getQuery().split("token=")[1];
            attributes.put("token", token); // Set in case needed at some point
            AuthenticationUtil.authenticateToken(token, jwtService, usersClient);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication instanceof UsernamePasswordAuthenticationToken) {
                return true;
            }
        }
        log.debug("Aborting handshake request due to invalid/missing token");
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return false;
    }

    @Override
    public void afterHandshake(@Nonnull ServerHttpRequest request,
                               @Nonnull ServerHttpResponse response,
                               @Nonnull WebSocketHandler wsHandler,
                               Exception exception) {
        log.debug("Completed handshake request {} {}", request.getMethod(), request.getURI().getPath());
    }
}
