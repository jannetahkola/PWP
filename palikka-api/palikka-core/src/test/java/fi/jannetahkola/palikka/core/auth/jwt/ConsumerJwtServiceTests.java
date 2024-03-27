package fi.jannetahkola.palikka.core.auth.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fi.jannetahkola.palikka.core.config.JwtConfig;
import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "palikka.jwt.keystore.verification.path=truststore-dev.p12",
                "palikka.jwt.keystore.verification.pass=password",
                "palikka.jwt.keystore.verification.type=pkcs12",

                "palikka.jwt.token.user.verification.key-alias=jwt-usr",
                "palikka.jwt.token.user.issuer=palikka-dev-user",

                "palikka.jwt.token.system.verification.key-alias=jwt-sys",
                "palikka.jwt.token.system.issuer=palikka-dev-system",
        })
@ExtendWith(OutputCaptureExtension.class)
@Import(JwtConfig.class)
class ConsumerJwtServiceTests {
    @Autowired
    JwtProperties properties;

    @Autowired
    JwtService jwtService;

    @Test
    void testSigningFails(CapturedOutput capturedOutput) {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        claimsBuilder.subject(String.valueOf(1));

        Optional<SignedJWT> userTokenMaybe = jwtService.sign(claimsBuilder, PalikkaJwtType.USER);
        assertThat(userTokenMaybe).isNotPresent();
        assertThat(capturedOutput.getAll()).contains("Token signing failed - service not configured with token producer support");

        Optional<SignedJWT> systemTokenMaybe = jwtService.sign(claimsBuilder, PalikkaJwtType.SYSTEM);
        assertThat(systemTokenMaybe).isNotPresent();
        assertThat(capturedOutput.getAll()).contains("Token signing failed - service not configured with token producer support");
    }

    @Test
    void testVerificationSucceeds() {
        JwtProperties jwtProperties = new JwtProperties();
        JwtProperties.KeyStoreProperties keystoreProperties = new JwtProperties.KeyStoreProperties();
        keystoreProperties.setPath("keystore-dev.p12");
        keystoreProperties.setPass("password");
        keystoreProperties.setType("pkcs12");
        JwtProperties.KeyStorePropertiesGroup keyStorePropertiesGroup = new JwtProperties.KeyStorePropertiesGroup();
        keyStorePropertiesGroup.setSigning(keystoreProperties);
        jwtProperties.setKeystore(keyStorePropertiesGroup);

        JwtProperties.TokenProperties tokenProperties = new JwtProperties.TokenProperties();
        tokenProperties.setIssuer(properties.getToken().getUser().getIssuer());
        jwtProperties.getToken().setUser(tokenProperties);

        JwtProperties.TokenKeyProperties tokenKeyProperties = new JwtProperties.TokenKeyProperties();
        tokenKeyProperties.setKeyAlias("jwt-usr");
        tokenKeyProperties.setKeyPass("password");
        tokenKeyProperties.setValidityTime(Duration.ofSeconds(10));
        tokenProperties.setSigning(tokenKeyProperties);

        JwtService signingJwtService = new JwtService(jwtProperties);

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        claimsBuilder.subject(String.valueOf(1));

        Optional<SignedJWT> token = signingJwtService.sign(claimsBuilder, PalikkaJwtType.USER);
        assertThat(token).isPresent();

        Optional<VerifiedJwt> parsedToken = jwtService.parse(token.get().serialize());
        assertThat(parsedToken).isNotNull();
    }
}
