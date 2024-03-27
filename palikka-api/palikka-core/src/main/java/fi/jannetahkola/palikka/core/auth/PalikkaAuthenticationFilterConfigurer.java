package fi.jannetahkola.palikka.core.auth;

import fi.jannetahkola.palikka.core.auth.authenticator.JwtAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@RequiredArgsConstructor
public class PalikkaAuthenticationFilterConfigurer {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    public void register(HttpSecurity http) {
        log.info("------ Authentication filter ENABLED ------");
        http.addFilterBefore(
                new PalikkaAuthenticationFilter(jwtAuthenticationProvider),
                UsernamePasswordAuthenticationFilter.class);
    }
}
