package fi.jannetahkola.palikka.game.api.game;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class GameControllerConfigurer implements WebSocketMessageBrokerConfigurer {
    public static final String SUBSCRIBE_PREFIX_TOPIC = "/topic";
    public static final String SUBSCRIBE_PREFIX_QUEUE = "/queue";
    public static final String MESSAGE_PREFIX = "/app";

    private final HandshakeInterceptor handshakeInterceptor;
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
        registry
                .addEndpoint("/ws")
                .addInterceptors(handshakeInterceptor)
//                .setHandshakeHandler(handshakeHandler)
                .withSockJS();
    }

    // The below are to disable CSRF and configure WS security manually instead of using
    // @EnableWebSocketSecurity. See https://stackoverflow.com/a/75068981

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationManager<Message<?>> build = new MessageMatcherDelegatingAuthorizationManager.Builder()
                .simpDestMatchers("/app/game").hasRole("ADMIN")
                .anyMessage().authenticated().build();

        AuthorizationChannelInterceptor authz = new AuthorizationChannelInterceptor(build);
        AuthorizationEventPublisher publisher = new SpringAuthorizationEventPublisher(this.applicationContext);
        authz.setAuthorizationEventPublisher(publisher);
        registration.interceptors(new SecurityContextChannelInterceptor(), authz);
    }
}
