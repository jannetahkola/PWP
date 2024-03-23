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

/**
 * Authenticates the connection from query parameters before proceeding with the handshake. Query parameters are used
 * because of difficulties in providing HTTP headers with JavaScript STOMP implementations.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationHandshakeInterceptor implements HandshakeInterceptor {
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication instanceof UsernamePasswordAuthenticationToken) {
            // todo check if this ever happens
            log.debug("Already authenticated, proceeding with handshake");
            return true;
        }

        final String requestQuery = request.getURI().getQuery();
        if (requestQuery != null && requestQuery.contains("token=")) {
            log.debug("Using token from request query");
            String token = requestQuery.split("token=")[1];

            AuthenticationUtil.authenticateToken(token, jwtService, usersClient);
            Authentication newAuthentication = SecurityContextHolder.getContext().getAuthentication();

            if (newAuthentication != null
                    && newAuthentication.isAuthenticated()
                    && newAuthentication instanceof UsernamePasswordAuthenticationToken authenticationToken) {
                authenticationToken.setDetails(token); // todo proper object
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
