package fi.jannetahkola.palikka.core.config;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationFilterConfigurer;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.meta.EnableRemoteUsersIntegration;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "palikka.integration.users-api.base-uri=http://test/",

                "palikka.jwt.keystore.signing.path=keystore-dev.p12",
                "palikka.jwt.keystore.signing.pass=password",
                "palikka.jwt.keystore.signing.type=pkcs12",

                "palikka.jwt.token.system.signing.key-alias=jwt-sys",
                "palikka.jwt.token.system.signing.key-pass=password",
                "palikka.jwt.token.system.signing.validity-time=10s",
                "palikka.jwt.token.system.issuer=palikka-dev-system",
        }
)
@EnableRemoteUsersIntegration
class RemoteUsersIntegrationEnabledTests {

    @Autowired
    ApplicationContext context;

    @Test
    void test() {
        assertThat(context.getBean(UsersClient.class)).isNotNull();
        assertThat(context.getBean(JwtService.class)).isNotNull();
        assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
                .isThrownBy(() -> context.getBean(PalikkaAuthenticationFilterConfigurer.class));
    }
}
