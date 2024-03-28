package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.EnableRedisTestSupport;
import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
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
                "palikka.jwt.keystore.signing.path=keystore-dev.p12",
                "palikka.jwt.keystore.signing.pass=password",
                "palikka.jwt.keystore.signing.type=pkcs12",

                "palikka.jwt.token.user.signing.key-alias=jwt-usr",
                "palikka.jwt.token.user.signing.key-pass=password",
                "palikka.jwt.token.user.signing.validity-time=10s",
                "palikka.jwt.token.user.issuer=palikka-dev-user",

                "palikka.jwt.token.system.signing.key-alias=jwt-sys",
                "palikka.jwt.token.system.signing.key-pass=password",
                "palikka.jwt.token.system.signing.validity-time=10s",
                "palikka.jwt.token.system.issuer=palikka-dev-system",

                "palikka.integration.users-api.base-uri=http://test/"
        })
@EnableAuthenticationSupport
@EnableRemoteUsersIntegration
@EnableRedisTestSupport
class AuthenticationSupportEnabledTests {

    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        assertThat(context.getBean(JwtService.class)).isNotNull();
        assertThat(context.getBean(UsersClient.class)).isNotNull();
        assertThat(context.getBean(PalikkaAuthenticationFilterConfigurer.class)).isNotNull();
        assertThat(context.getBean(RevokedTokenRepository.class)).isNotNull();
    }
}
