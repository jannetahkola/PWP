package fi.jannetahkola.palikka.core.auth.jwt;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
        })
@ExtendWith(OutputCaptureExtension.class)
@Import(JwtConfig.class)
class ProducerJwtServiceTests {

    @Autowired
    JwtService jwtService;

    @Test
    void testInvalidTokenValue(CapturedOutput capturedOutput) {
        assertThat(jwtService.parse("")).isNotPresent();
        assertThat(jwtService.parse(" ")).isNotPresent();
        assertThat(jwtService.parse(null)).isNotPresent();
        assertThat(jwtService.parse("test")).isNotPresent();
        assertThat(capturedOutput.getAll().split("\n"))
                .filteredOn(logLine -> logLine.contains("Invalid serialized unsecured/JWS/JWE object: Missing part delimiters"))
                .hasSize(1); // blank/null cases are not logged
    }

    @SneakyThrows
    @Test
    void testInvalidSigningKey(@Autowired JwtProperties properties,
                               CapturedOutput capturedOutput) {
        var keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(
                properties.getKeystore().getSigning(), properties.getToken().getUser());
        RSASSASigner jwsSigner = new RSASSASigner(keyPair.getPrivate());
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(properties.getToken().getUser().getSigning().getKeyAlias())
                .type(JOSEObjectType.JWT)
                // Set ptyp wrong
                .customParam("ptyp", PalikkaJwtType.SYSTEM);

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(properties.getToken().getUser().getIssuer())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .subject(String.valueOf(1));
        SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
        signedJWT.sign(jwsSigner);

        String token = signedJWT.serialize();

        assertThat(jwtService.parse(token)).isNotPresent();
        assertThat(capturedOutput.getAll()).contains("Signed JWT rejected: Invalid signature");
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("provideInvalidClaimsParamsForUserToken")
    void testInvalidClaimsForUserToken(JWTClaimsSet.Builder claims,
                           String expectedErrorSubstring,
                           @Autowired JwtProperties properties,
                           CapturedOutput capturedOutput) {
        // Create the token manually to skip service's default claim values
        var tokenProperties = properties.getToken().getUser();
        var keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(
                properties.getKeystore().getSigning(), tokenProperties);
        RSASSASigner jwsSigner = new RSASSASigner(keyPair.getPrivate());
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(tokenProperties.getSigning().getKeyAlias())
                .type(JOSEObjectType.JWT)
                .customParam("ptyp", PalikkaJwtType.USER);
        SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
        signedJWT.sign(jwsSigner);

        String token = signedJWT.serialize();

        assertThat(jwtService.parse(token)).isNotPresent();
        assertThat(capturedOutput.getAll()).contains(expectedErrorSubstring);
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("provideInvalidClaimsParamsForSystemToken")
    void testInvalidClaimsForSystemToken(JWTClaimsSet.Builder claims,
                                         String expectedErrorSubstring,
                                         @Autowired JwtProperties properties,
                                         CapturedOutput capturedOutput) {
        // Create the token manually to skip service's default claim values
        var tokenProperties = properties.getToken().getSystem();
        var keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(
                properties.getKeystore().getSigning(), tokenProperties);
        RSASSASigner jwsSigner = new RSASSASigner(keyPair.getPrivate());
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(tokenProperties.getSigning().getKeyAlias())
                .type(JOSEObjectType.JWT)
                .customParam("ptyp", PalikkaJwtType.SYSTEM);
        SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
        signedJWT.sign(jwsSigner);

        String token = signedJWT.serialize();

        assertThat(jwtService.parse(token)).isNotPresent();
        assertThat(capturedOutput.getAll()).contains(expectedErrorSubstring);
    }

    @SneakyThrows
    @Test
    void testMissingPalikkaTokenType(@Autowired JwtProperties properties,
                                     CapturedOutput capturedOutput) {
        var keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(
                properties.getKeystore().getSigning(), properties.getToken().getUser());
        RSASSASigner jwsSigner = new RSASSASigner(keyPair.getPrivate());
        JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(properties.getToken().getUser().getSigning().getKeyAlias())
                .type(JOSEObjectType.JWT);

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(properties.getToken().getUser().getIssuer())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(10)))
                .subject(String.valueOf(1));
        SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
        signedJWT.sign(jwsSigner);

        String token = signedJWT.serialize();

        assertThat(jwtService.parse(token)).isNotPresent();
        assertThat(capturedOutput.getAll()).contains("NullPointerException: Missing palikka token type");
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

        Optional<SignedJWT> tokenMaybe = jwtService.sign(claimsBuilder, PalikkaJwtType.USER);
        assertThat(tokenMaybe).isPresent();

        SignedJWT signedJwt = tokenMaybe.get();
        String token = signedJwt.serialize();

        Optional<VerifiedJwt> parsedTokenMaybe = jwtService.parse(token);
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

    private static Stream<Arguments> provideInvalidClaimsParamsForUserToken() {
        Supplier<JWTClaimsSet.Builder> defaultJwtClaimsSetBuilder = () ->
                new JWTClaimsSet.Builder()
                        .issuer("palikka-dev-user")
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

    private static Stream<Arguments> provideInvalidClaimsParamsForSystemToken() {
        Supplier<JWTClaimsSet.Builder> defaultJwtClaimsSetBuilder = () ->
                new JWTClaimsSet.Builder()
                        .issuer("palikka-dev-system")
                        .jwtID(UUID.randomUUID().toString())
                        .issueTime(Date.from(Instant.now()))
                        .expirationTime(Date.from(Instant.now().plusSeconds(10)));
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
                        Named.of("Expired jwt", defaultJwtClaimsSetBuilder.get().expirationTime(Date.from(Instant.now().minusSeconds(60)))),
                        "Expired JWT"
                )
        );
    }
}
