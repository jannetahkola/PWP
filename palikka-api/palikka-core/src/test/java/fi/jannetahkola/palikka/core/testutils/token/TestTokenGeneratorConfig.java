package fi.jannetahkola.palikka.core.testutils.token;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import org.springframework.context.annotation.Bean;

public class TestTokenGeneratorConfig {
    @Bean
    TestTokenGenerator testTokenGenerator(JwtService jwtService) {
        return new TestTokenGenerator(jwtService);
    }
}
