package fi.jannetahkola.palikka.core.testutils.token;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@RequiredArgsConstructor
public class TestTokenGenerator {
    private final JwtService jwtService;

    public String generateToken(Integer userId) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER
        ).map(JWSObject::serialize).orElseThrow();
    }

    public String generateSystemToken() {
        return jwtService.sign(
                new JWTClaimsSet.Builder(),
                PalikkaJwtType.SYSTEM
        ).map(JWSObject::serialize).orElseThrow();
    }

    public String generateExpiredToken(Integer userId) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER,
                Date.from(Instant.now().minusSeconds(60))
        ).map(JWSObject::serialize).orElseThrow();
    }

    public String generateTokenExpiringIn(Integer userId, Duration duration) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER,
                Date.from(Instant.now().plusSeconds(duration.toSeconds()))
        ).map(JWSObject::serialize).orElseThrow();
    }

    public String generateBearerToken(Integer userId) {
        return "Bearer " + generateToken(userId);
    }

    public String generateBearerSystemToken() {
        return "Bearer " + generateSystemToken();
    }
}
