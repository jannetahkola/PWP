package fi.jannetahkola.palikka.users.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.config.meta.EnableAuthenticationSupport;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.users.LocalUsersClient;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableAuthenticationSupport
public class AppConfig {
    @SneakyThrows
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            PalikkaAuthenticationFilterConfigurer authenticationFilterConfigurer) {
        log.info("------ Security ENABLED ------");
        http
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/users-api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);
        authenticationFilterConfigurer.register(http);
        return http.build();
    }

    @Bean
    @Primary
    UsersClient usersClient(UserRepository userRepository) {
        log.info("------ Local users client ENABLED ------");
        return new LocalUsersClient(userRepository);
    }
}
