package fi.jannetahkola.palikka.game.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.authenticator.JwtAuthenticationProvider;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.meta.EnableAuthenticationSupport;
import fi.jannetahkola.palikka.core.config.meta.EnableRemoteUsersIntegration;
import fi.jannetahkola.palikka.core.config.meta.EnableRequestAndResponseLoggingSupport;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.service.factory.ProcessFactory;
import fi.jannetahkola.palikka.game.service.factory.SocketFactory;
import fi.jannetahkola.palikka.game.websocket.*;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.socket.server.HandshakeHandler;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableWebSecurity
@EnableMethodSecurity
@EnableAuthenticationSupport
@EnableRemoteUsersIntegration
@EnableRequestAndResponseLoggingSupport
@EnableConfigurationProperties(GameProperties.class)
public class PalikkaGameApiAppConfig {

    @SneakyThrows
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            PalikkaAuthenticationFilterConfigurer authenticationFilterConfigurer) {
        http
                .sessionManagement(sessions -> sessions
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // Goes through authentication filter to WebSocket security configs
                        .requestMatchers("/ws").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        authenticationFilterConfigurer.register(http);
        return http.build();
    }

    @Bean
    AuthenticationHandshakeInterceptor authenticationHandshakeInterceptor(JwtAuthenticationProvider jwtAuthenticationProvider) {
        return new AuthenticationHandshakeInterceptor(jwtAuthenticationProvider);
    }

    @Bean
    HandshakeHandler handshakeHandler() {
        return new GameControllerHandshakeHandler();
    }

    @Bean
    SessionEventLogger sessionInterceptor() {
        return new SessionEventLogger();
    }

    @Bean
    SessionStore webSocketSessionStore(JwtService jwtService) {
        return new SessionStore(jwtService);
    }

    @Bean
    PreSendAuthorizationChannelInterceptor preSendAuthorizationChannelInterceptor(JwtService jwtService) {
        return new PreSendAuthorizationChannelInterceptor(jwtService);
    }

    @Bean
    SessionBasedWebSocketHandlerDecoratorFactory sessionBasedWebSocketHandlerDecoratorFactory(SessionStore sessionStore) {
        return new SessionBasedWebSocketHandlerDecoratorFactory(sessionStore);
    }

    @Bean
    @ConditionalOnProperty(value = "palikka.session.auto-clean.enabled", matchIfMissing = true)
    SessionCleanUpScheduler sessionCleanUpScheduler(GameProperties gameProperties, SessionStore sessionStore) {
        return new SessionCleanUpScheduler(gameProperties, sessionStore);
    }

    @Bean
    SocketFactory socketFactory() {
        return new SocketFactory();
    }

    @Bean
    Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(5);
        return executor;
    }

    @Bean
    ProcessFactory processFactory() {
        return new ProcessFactory();
    }

    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("*"));
        corsConfiguration.setAllowedMethods(List.of("GET", "POST"));
        corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
