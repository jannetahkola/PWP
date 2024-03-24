package fi.jannetahkola.palikka.game.testutils;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class TestTokenUtils {
    private final JwtService jwtService;

    public String generateSystemToken() {
        return jwtService.sign(
                new JWTClaimsSet.Builder(),
                PalikkaJwtType.SYSTEM
        ).orElseThrow();
    }

    public String generateToken(Integer userId) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER
        ).orElseThrow();
    }

    public String generateExpiredToken(Integer userId) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER,
                Date.from(Instant.now().minusSeconds(60))
        ).orElseThrow();
    }

    public String generateTokenExpiringIn(Integer userId, Duration duration) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER,
                Date.from(Instant.now().plusSeconds(duration.toSeconds()))
        ).orElseThrow();
    }
}
