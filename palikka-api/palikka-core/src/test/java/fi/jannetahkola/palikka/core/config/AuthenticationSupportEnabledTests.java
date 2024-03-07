package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.meta.EnableAuthenticationSupport;
import fi.jannetahkola.palikka.core.config.meta.EnableRemoteUsersIntegration;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "palikka.jwt.keystore-path=dev.keystore",
                "palikka.jwt.keystore-pass=password",
                "palikka.jwt.keystore-type=pkcs12",
                "palikka.jwt.token.key-alias=jwt",
                "palikka.jwt.token.key-pass=password",
                "palikka.jwt.token.issuer=palikka-dev",
                "palikka.jwt.token.validity-time=10s",
                "palikka.integration.users-api.base-uri=http://test/"
        }
)
@EnableAuthenticationSupport
@EnableRemoteUsersIntegration
class AuthenticationSupportEnabledTests {

    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        assertThat(context.getBean(JwtService.class)).isNotNull();
        assertThat(context.getBean(UsersClient.class)).isNotNull();
        assertThat(context.getBean(PalikkaAuthenticationFilterConfigurer.class)).isNotNull();
    }
}