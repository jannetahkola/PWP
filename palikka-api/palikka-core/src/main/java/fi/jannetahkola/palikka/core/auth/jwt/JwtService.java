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
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

@Slf4j
public class JwtService {
    private final JwtProperties properties;
    private final Map<PalikkaJwtType, ConfigurableJWTProcessor<SecurityContext>> jwtProcessors = new EnumMap<>(PalikkaJwtType.class);
    private final Map<PalikkaJwtType, JWSSigner> jwsSigners = new EnumMap<>(PalikkaJwtType.class);

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        initUserTokenSupport();
        initSystemTokenSupport();
    }

    public JwtProperties getProperties() {
        return this.properties;
    }

    public boolean isExpired(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getExpirationTime().before(new Date());
        } catch (ParseException e) {
            log.info("Token parsing failed", e);
        }
        return true;
    }

    public Optional<JWTClaimsSet> parse(String token) {
        if (!StringUtils.hasText(token)) return Optional.empty();
        try {
            String ptyp = (String) Objects.requireNonNull(
                    SignedJWT.parse(token).getHeader().getCustomParam("ptyp"), "Missing palikka token type");
            PalikkaJwtType palikkaJwtType = PalikkaJwtType.valueOf(ptyp);
            if (!jwtProcessors.containsKey(palikkaJwtType)) {
                log.info("Token verification failed - support for token type '{}' not initialized", palikkaJwtType);
                return Optional.empty();
            }
            return Optional.of(jwtProcessors.get(PalikkaJwtType.valueOf(ptyp)).process(token, null));
        } catch (BadJOSEException | ParseException e) {
            log.info("Token verification failed - bad token. Token='{}'", token, e);
        } catch (JOSEException e) {
            log.info("Token verification failed - JOSE exception. Token={}", token, e);
        } catch (Exception e) {
            log.info("Token verification failed. Token={} ", token, e);
        }
        return Optional.empty();
    }

    public Optional<String> sign(JWTClaimsSet.Builder claims, PalikkaJwtType jwtType) {
        return sign(claims, jwtType, null);
    }

    public Optional<String> sign(JWTClaimsSet.Builder claims,
                                 PalikkaJwtType jwtType,
                                 Date expiryDate) {
        if (!jwsSigners.containsKey(jwtType)) {
            log.error("Token signing failed - service not configured with token producer support");
            return Optional.empty();
        }

        log.debug("Signing a token with type {}", jwtType);
        try {
            JwtProperties.TokenProperties tokenProperties;

            if (PalikkaJwtType.USER.equals(jwtType)) {
                tokenProperties = properties.getToken().getUser();
            } else if (PalikkaJwtType.SYSTEM.equals(jwtType)) {
                tokenProperties = properties.getToken().getSystem();
            } else {
                log.error("Token signing failed - invalid token type '{}'", jwtType);
                return Optional.empty();
            }

            JwtProperties.TokenKeyProperties signingProperties = tokenProperties.getSigning();

            claims.jwtID(UUID.randomUUID().toString());
            claims.issuer(tokenProperties.getIssuer());
            claims.issueTime(Date.from(Instant.now()));
            claims.expirationTime(expiryDate);

            if (expiryDate == null) {
                claims.expirationTime(Date.from(Instant.now()
                        .plusSeconds(signingProperties.getValidityTime().getSeconds())));
            }

            JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS512)
                    .keyID(signingProperties.getKeyAlias())
                    .type(JOSEObjectType.JWT)
                    .customParam("ptyp", jwtType);

            SignedJWT signedJWT = new SignedJWT(header.build(), claims.build());
            signedJWT.sign(jwsSigners.get(jwtType));
            return Optional.of(signedJWT.serialize());
        } catch (JOSEException e) {
            log.error("Token signing failed", e);
        }
        return Optional.empty();
    }

    private void initUserTokenSupport() {
        var tokenType = PalikkaJwtType.USER;
        log.info("Initializing support for palikka token type {}", tokenType);

        var userTokenProperties = properties.getToken().getUser();
        if (userTokenProperties == null) {
            log.info("Abort initialization to support palikka token type {} - missing configuration", tokenType);
            return;
        }

        var jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
        jwtProcessor.setJWTClaimsSetVerifier(
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder()
                                .issuer(userTokenProperties.getIssuer())
                                .build(),
                        Set.of("jti", "sub", "iss", "exp", "iat")
                ));

        initProcessingMode(userTokenProperties, jwtProcessor, tokenType);

        jwtProcessors.putIfAbsent(tokenType, jwtProcessor);
    }

    private void initSystemTokenSupport() {
        var tokenType = PalikkaJwtType.SYSTEM;

        log.info("Initializing support for palikka token type {}", PalikkaJwtType.SYSTEM);

        var systemTokenProperties = properties.getToken().getSystem();
        if (systemTokenProperties == null) {
            log.info("Abort initialization to support palikka token type {} - missing configuration", tokenType);
            return;
        }

        var jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
        jwtProcessor.setJWTClaimsSetVerifier(
                new DefaultJWTClaimsVerifier<>(
                        new JWTClaimsSet.Builder()
                                .issuer(systemTokenProperties.getIssuer())
                                .build(),
                        Set.of("jti", "iss", "exp", "iat")
                ));

        initProcessingMode(systemTokenProperties, jwtProcessor, tokenType);

        jwtProcessors.putIfAbsent(tokenType, jwtProcessor);
    }

    private void initProcessingMode(JwtProperties.TokenProperties tokenProperties,
                                    ConfigurableJWTProcessor<SecurityContext> jwtProcessor,
                                    PalikkaJwtType tokenType) {
        JwtProperties.TokenKeyProperties signingProperties = tokenProperties.getSigning();
        JwtProperties.TokenKeyProperties verificationProperties = tokenProperties.getVerification();

        JwtProperties.KeyStoreProperties signingKeyStoreProperties = properties.getKeystore().getSigning();
        JwtProperties.KeyStoreProperties verificationKeyStoreProperties = properties.getKeystore().getVerification();

        if (signingProperties != null) {
            if (signingKeyStoreProperties == null) {
                throw new IllegalStateException(
                        "Incomplete configuration - signing support configured for token type "
                                + tokenType + " but signing keystore configuration missing");
            }
            if (signingProperties.getKeyPass() == null) {
                throw new IllegalStateException(
                        "Incomplete configuration - signing support configured for token type "
                                + tokenType + " but signing key password missing");
            }
            if (signingProperties.getValidityTime() == null) {
                throw new IllegalStateException(
                        "Incomplete configuration - signing support configured for token type "
                                + tokenType + " but validity time missing");
            }
            final KeyPair keyPair = KeyUtil.loadKeyPairFromPropertiesOrError(signingKeyStoreProperties, tokenProperties);
            jwsSigners.putIfAbsent(tokenType, new RSASSASigner(keyPair.getPrivate()));
            jwtProcessor.setJWSKeySelector((jwsHeader, securityContext) -> List.of(keyPair.getPublic())); // Could load these from JWS header but whatever
            log.info("Producer support for token type {} initialized", tokenType);
        } else {
            if (verificationProperties == null) {
                // Some properties like issuer were given but both signing and verification are missing
                throw new IllegalStateException(
                        "Incomplete configuration for token type " + tokenType);
            }
            if (verificationKeyStoreProperties == null) {
                throw new IllegalStateException(
                        "Incomplete configuration - verification support configured for token type "
                                + tokenType + " but verification keystore configuration missing");
            }
            PublicKey key = KeyUtil.loadPublicKeyFromPropertiesOrError(verificationKeyStoreProperties, tokenProperties);
            jwtProcessor.setJWSKeySelector((jwsHeader, securityContext) -> List.of(key));
            log.info("Consumer support for token type {} initialized", tokenType);
        }
    }
}
