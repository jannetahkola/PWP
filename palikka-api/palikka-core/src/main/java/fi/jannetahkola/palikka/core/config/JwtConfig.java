package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
    @Bean
    JwtService jwtService(JwtProperties jwtProperties) {
        log.info("JwtService configured with properties={}", jwtProperties);
        return new JwtService(jwtProperties);
    }
}
