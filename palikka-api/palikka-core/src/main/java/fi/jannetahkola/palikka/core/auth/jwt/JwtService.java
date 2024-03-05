package fi.jannetahkola.palikka.core.auth.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import fi.jannetahkola.palikka.core.util.KeyUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

@Slf4j
public class JwtService {
    private final JwtProperties properties;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final JWSSigner jwsSigner;

    public JwtService(JwtProperties properties) {
        final KeyPair keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(properties);
        this.properties = properties;
        this.jwtProcessor = new DefaultJWTProcessor<>();
        this.jwtProcessor.setJWSKeySelector((jwsHeader, securityContext) -> List.of(keyPair.getPublic()));
        this.jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
        this.jwtProcessor.setJWTClaimsSetVerifier(
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder()
                                .issuer(properties.getToken().getIssuer())
                                .build(),
                        Set.of("jti", "sub", "iss", "exp", "iat")
                ));
        this.jwsSigner = new RSASSASigner(keyPair.getPrivate());
    }

    public Optional<JWTClaimsSet> parse(String token) {
        if (token == null) return Optional.empty();
        try {
            return Optional.of(jwtProcessor.process(token, null));
        } catch (BadJOSEException | ParseException e) {
            log.info("Token verification failed - bad token. Token='{}'", token, e);
        } catch (JOSEException e) {
            log.info("Token verification failed. Token={}", token, e);
        }
        return Optional.empty();
    }

    public Optional<String> sign(JWTClaimsSet.Builder claims) {
        try {
            claims.jwtID(UUID.randomUUID().toString());
            claims.issuer(properties.getToken().getIssuer());
            claims.issueTime(Date.from(Instant.now()));
            claims.expirationTime(Date.from(Instant.now().plusSeconds(properties.getToken().getValidityTime().getSeconds())));

            JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                    .keyID(properties.getToken().getKeyAlias())
                    .type(JOSEObjectType.JWT);

            SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
            signedJWT.sign(jwsSigner);
            return Optional.of(signedJWT.serialize());
        } catch (JOSEException e) {
            log.error("Token signing failed", e);
        }
        return Optional.empty();
    }
}
