package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.core.util.AuthenticationUtil;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class GameControllerHandshakeHandler extends DefaultHandshakeHandler {
    private final JwtService jwtService;
    private final UsersClient usersClient;

    @Override
    public Principal determineUser(@Nonnull ServerHttpRequest request,
                                   @Nonnull WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        log.debug("Authenticating handshake request {} {}", request.getMethod(), request.getURI().getPath());
        String token = (String) attributes.get("token");
        AuthenticationUtil.authenticateToken(token, jwtService, usersClient); // TODO Check if ok that security context updated inside
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication instanceof UsernamePasswordAuthenticationToken)) {
            return null;
        }
        return authentication;
    }
}
