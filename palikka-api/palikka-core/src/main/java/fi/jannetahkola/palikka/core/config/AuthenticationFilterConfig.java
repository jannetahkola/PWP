package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.springframework.context.annotation.Bean;

public class AuthenticationFilterConfig {
    @Bean
    PalikkaAuthenticationFilterConfigurer authenticationFilterConfigurer(JwtService jwtService,
                                                                         UsersClient usersClient) {
        return new PalikkaAuthenticationFilterConfigurer(jwtService, usersClient);
    }
}
