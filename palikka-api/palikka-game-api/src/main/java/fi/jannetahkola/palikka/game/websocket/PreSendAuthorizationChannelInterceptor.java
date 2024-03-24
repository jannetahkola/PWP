package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationDetails;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Objects;

/**
 * Checks that the JWT that was used to establish the connection is not expired before passing the received message
 * forward. If it is expired, access is revoked by removing the expired authentication token from the message. Spring
 * will then close the connection automatically when authorization fails in the following security interceptors.
 * <br><br>
 * Anything sent back to the client with {@link SimpMessagingTemplate} will not be caught here because this will only
 * detect STOMP messages, and {@link SimpMessagingTemplate#convertAndSend(Object)} to a generic topic will not contain
 * any authentication information. This is a problem because once a JWT expires, the user is still left with read
 * access to any existing subscriptions, as long as they don't trigger this interceptor by calling a
 * {@link MessageMapping} handler.
 * <br><br>
 * To somewhat mitigate the above, the service also implements the {@link SessionCleanUpScheduler}. In the future a
 * raw WebSocket implementation should be considered instead of STOMP to reduce the need for all these workarounds.
 * Unless there's a way to access STOMP sessions directly for full control.
 */
@Slf4j
@RequiredArgsConstructor
public class PreSendAuthorizationChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@Nonnull Message<?> message, @Nonnull MessageChannel channel) {
        // Note that using StompHeaderAccessor directly will not work as it won't mutate the headers
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && Objects.equals(accessor.getCommand(), StompCommand.SEND)) {
            // Token type is known at this point so cast without checks
            var authenticationToken = (UsernamePasswordAuthenticationToken) accessor.getUser();
            if (authenticationToken != null
                    && jwtService.isExpired(((PalikkaAuthenticationDetails) authenticationToken.getDetails()).getToken())) {
                // This is enough to fail authorization in AuthorizationChannelInterceptor and
                // disconnect the session - no need to clear security context
                accessor.setUser(null);
                log.debug("Access revoked from STOMP session with an expired " +
                        "JWT, principal={}", authenticationToken.getName());
            }
        }
        return message;
    }
}
