package fi.jannetahkola.palikka.game.api.game;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class GameControllerConfigurer implements WebSocketMessageBrokerConfigurer {
    public static final String SUBSCRIBE_PREFIX_TOPIC = "/topic";
    public static final String SUBSCRIBE_PREFIX_QUEUE = "/queue";
    public static final String MESSAGE_PREFIX = "/app";

    private final HandshakeInterceptor handshakeInterceptor;
    private final HandshakeHandler handshakeHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Subscription paths
        registry.enableSimpleBroker(SUBSCRIBE_PREFIX_TOPIC, SUBSCRIBE_PREFIX_QUEUE);
        // Message paths
        registry.setApplicationDestinationPrefixes(
                MESSAGE_PREFIX,
                // Register this as well so that Spring will forward subscriptions
                // to the app instead of just passing them to the broker
                SUBSCRIBE_PREFIX_TOPIC
        );
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor)
//                .setHandshakeHandler(handshakeHandler)
                .withSockJS();
    }
}
