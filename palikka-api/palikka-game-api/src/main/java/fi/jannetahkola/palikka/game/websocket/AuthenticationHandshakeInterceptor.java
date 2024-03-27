package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationDetails;
import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilter;
import fi.jannetahkola.palikka.core.auth.authenticator.JwtAuthenticationProvider;
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
 * because of difficulties in providing HTTP headers with JavaScript STOMP implementations. This is skipped
 * if {@link PalikkaAuthenticationFilter} gets called first - as it should be.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;

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
            log.debug("Already authenticated, proceeding with handshake");
            return true;
        }

        final String requestQuery = request.getURI().getQuery();
        if (requestQuery != null && requestQuery.contains("token=")) {
            log.debug("Using token from request query");
            String token = requestQuery.split("token=")[1];

            jwtAuthenticationProvider.authenticate(token);
            Authentication newAuthentication = SecurityContextHolder.getContext().getAuthentication();

            if (newAuthentication != null
                    && newAuthentication.isAuthenticated()
                    && newAuthentication instanceof UsernamePasswordAuthenticationToken authenticationToken) {
                PalikkaAuthenticationDetails details = new PalikkaAuthenticationDetails();
                details.setToken(token);
                authenticationToken.setDetails(details);
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
