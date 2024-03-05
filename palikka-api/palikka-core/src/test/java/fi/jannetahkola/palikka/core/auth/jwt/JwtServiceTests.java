package fi.jannetahkola.palikka.core.auth.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.config.JwtConfig;
import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import fi.jannetahkola.palikka.core.util.KeyUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
                "palikka.jwt.token.validity-time=10s"
        })
@ExtendWith(OutputCaptureExtension.class)
@Import(JwtConfig.class)
class JwtServiceTests {

    @Autowired
    JwtService jwtService;

    @Test
    void testInvalidTokenValue(CapturedOutput capturedOutput) {
        assertThat(jwtService.parse("")).isNotPresent();
        assertThat(jwtService.parse(" ")).isNotPresent();
        assertThat(jwtService.parse(null)).isNotPresent();
        assertThat(jwtService.parse("test")).isNotPresent();
        assertThat(capturedOutput.getAll().split("\n"))
                .filteredOn(logLine -> logLine.contains("Invalid JWT serialization: Missing dot delimiter(s)"))
                .hasSize(3); // null case is not logged
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("provideInvalidClaimsParams")
    void testInvalidIssuer(JWTClaimsSet.Builder claims,
                           String expectedErrorSubstring,
                           @Autowired JwtProperties properties,
                           CapturedOutput capturedOutput) {
        // Create the token manually to skip service's default claim values
        var keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(properties);
        RSASSASigner jwsSigner = new RSASSASigner(keyPair.getPrivate());
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(properties.getToken().getKeyAlias())
                .type(JOSEObjectType.JWT);
        SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
        signedJWT.sign(jwsSigner);

        String token = signedJWT.serialize();

        assertThat(jwtService.parse(token)).isNotPresent();
        assertThat(capturedOutput.getAll()).contains(expectedErrorSubstring);
    }

    @Test
    void testSuccess() throws ParseException {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        claimsBuilder.subject(String.valueOf(1));

        // These should be overwritten
        claimsBuilder.jwtID("test-id");
        claimsBuilder.issuer("test-issuer");
        claimsBuilder.issueTime(Date.from(Instant.now().plusSeconds(30)));
        claimsBuilder.expirationTime(Date.from(Instant.now()));

        Optional<String> tokenMaybe = jwtService.sign(claimsBuilder);
        assertThat(tokenMaybe).isPresent();

        String token = tokenMaybe.get();
        System.out.println(token);

        Optional<JWTClaimsSet> parsedTokenMaybe = jwtService.parse(token);
        assertThat(parsedTokenMaybe).isPresent();

        SignedJWT jwt = SignedJWT.parse(token);

        JWSHeader header = jwt.getHeader();
        assertThat(header.getAlgorithm()).isNotNull();
        assertThat(header.getKeyID()).isNotNull();
        assertThat(header.getType()).isNotNull();

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        assertThat(claims.getJWTID()).isNotNull();
        assertThat(claims.getJWTID()).isNotEqualTo("test-id");
        assertThat(claims.getIssuer()).isNotNull();
        assertThat(claims.getIssuer()).isNotEqualTo("test-issuer");
        assertThat(claims.getExpirationTime()).isNotNull();
        assertThat(claims.getExpirationTime()).isBefore(Date.from(Instant.now().plusSeconds(20)));
        assertThat(claims.getIssueTime()).isNotNull();
        assertThat(claims.getIssueTime()).isBefore(Date.from(Instant.now().plusSeconds(5)));
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(1));
    }

    private static Stream<Arguments> provideInvalidClaimsParams() {
        Supplier<JWTClaimsSet.Builder> defaultJwtClaimsSetBuilder = () ->
                new JWTClaimsSet.Builder()
                        .issuer("palikka-dev")
                        .jwtID(UUID.randomUUID().toString())
                        .issueTime(Date.from(Instant.now()))
                        .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                        .subject(String.valueOf(1));
        return Stream.of(
                Arguments.of(
                        Named.of("Invalid iss", defaultJwtClaimsSetBuilder.get().issuer("test-issuer")),
                        "JWT iss claim has value test-issuer, must be"
                ),
                Arguments.of(
                        Named.of("Missing iss", defaultJwtClaimsSetBuilder.get().issuer(null)),
                        "JWT missing required claims: [iss]"
                ),
                Arguments.of(
                        Named.of("Missing jti", defaultJwtClaimsSetBuilder.get().jwtID(null)),
                        "JWT missing required claims: [jti]"
                ),
                Arguments.of(
                        Named.of("Missing iat", defaultJwtClaimsSetBuilder.get().issueTime(null)),
                        "JWT missing required claims: [iat]"
                ),
                Arguments.of(
                        Named.of("Missing exp", defaultJwtClaimsSetBuilder.get().expirationTime(null)),
                        "JWT missing required claims: [exp]"
                ),
                Arguments.of(
                        Named.of("Missing sub", defaultJwtClaimsSetBuilder.get().subject(null)),
                        "JWT missing required claims: [sub]"
                ),
                Arguments.of(
                        Named.of("Expired jwt", defaultJwtClaimsSetBuilder.get().expirationTime(Date.from(Instant.now().minusSeconds(60)))),
                        "Expired JWT"
                )
        );
    }
}
