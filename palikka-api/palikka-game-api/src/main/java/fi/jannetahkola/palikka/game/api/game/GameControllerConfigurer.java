package fi.jannetahkola.palikka.game.api.game;

import fi.jannetahkola.palikka.game.websocket.AuthenticationHandshakeInterceptor;
import fi.jannetahkola.palikka.game.websocket.PreSendAuthorizationChannelInterceptor;
import fi.jannetahkola.palikka.game.websocket.SessionBasedWebSocketHandlerDecoratorFactory;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeHandler;

import java.util.List;

// todo enable CORS
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class GameControllerConfigurer implements WebSocketMessageBrokerConfigurer {
    public static final String SUBSCRIBE_PREFIX_TOPIC = "/topic";
    public static final String SUBSCRIBE_PREFIX_QUEUE = "/queue";
    public static final String MESSAGE_PREFIX = "/app";

    private final PreSendAuthorizationChannelInterceptor preSendAuthorizationChannelInterceptor;
    private final SessionBasedWebSocketHandlerDecoratorFactory sessionBasedWebSocketHandlerDecoratorFactory;
    private final AuthenticationHandshakeInterceptor authenticationHandshakeInterceptor;
    private final HandshakeHandler handshakeHandler;
    private final ApplicationContext applicationContext;

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
        // SockJS is disabled -> all connections must use ws protocol instead of http
        registry
                .addEndpoint("/ws")
                .addInterceptors(authenticationHandshakeInterceptor)
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(@Nonnull WebSocketTransportRegistration registry) {
        WebSocketMessageBrokerConfigurer.super.configureWebSocketTransport(registry);
        registry.addDecoratorFactory(sessionBasedWebSocketHandlerDecoratorFactory);
    }

    // The below are to disable CSRF and configure WS security manually instead of using
    // @EnableWebSocketSecurity. See https://stackoverflow.com/a/75068981

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationManager<Message<?>> authorizationManager = new MessageMatcherDelegatingAuthorizationManager.Builder()
                .simpDestMatchers("/app/game").hasAnyRole("ADMIN", "USER", "VIEWER")
                .anyMessage().authenticated()
                .build();
        AuthorizationChannelInterceptor authorizationChannelInterceptor = new AuthorizationChannelInterceptor(authorizationManager);
        authorizationChannelInterceptor.setAuthorizationEventPublisher(new SpringAuthorizationEventPublisher(this.applicationContext));
        registration.interceptors(
                preSendAuthorizationChannelInterceptor,
                new SecurityContextChannelInterceptor(),
                authorizationChannelInterceptor
        );
    }
}
