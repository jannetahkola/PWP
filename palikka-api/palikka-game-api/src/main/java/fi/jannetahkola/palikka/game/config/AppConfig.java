package fi.jannetahkola.palikka.game.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.meta.EnableAuthenticationSupport;
import fi.jannetahkola.palikka.core.config.meta.EnableRemoteUsersIntegration;
import fi.jannetahkola.palikka.core.config.meta.EnableRequestAndResponseLoggingSupport;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.service.ProcessFactory;
import fi.jannetahkola.palikka.game.service.SocketFactory;
import fi.jannetahkola.palikka.game.websocket.GameControllerHandshakeHandler;
import fi.jannetahkola.palikka.game.websocket.GameControllerHandshakeInterceptor;
import fi.jannetahkola.palikka.game.websocket.SessionInterceptor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableWebSecurity
@EnableMethodSecurity
@EnableAuthenticationSupport
@EnableRemoteUsersIntegration
@EnableRequestAndResponseLoggingSupport
@Import(GameProperties.class)
public class AppConfig {

    @SneakyThrows
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            PalikkaAuthenticationFilterConfigurer authenticationFilterConfigurer) {
        http
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable);
        authenticationFilterConfigurer.register(http);
        return http.build();
    }

    @Bean
    HandshakeInterceptor handshakeInterceptor() {
        return new GameControllerHandshakeInterceptor();
    }

    @Bean
    HandshakeHandler handshakeHandler(JwtService jwtService, UsersClient usersClient) {
        return new GameControllerHandshakeHandler(jwtService, usersClient);
    }

    @Bean
    SessionInterceptor sessionInterceptor() {
        return new SessionInterceptor();
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
}
