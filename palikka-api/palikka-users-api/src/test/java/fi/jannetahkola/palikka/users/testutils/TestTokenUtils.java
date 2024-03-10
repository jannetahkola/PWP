package fi.jannetahkola.palikka.users.testutils;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestTokenUtils {
    private final JwtService jwtService;

    public String generateToken(Integer userId) {
        return jwtService.sign(
                new JWTClaimsSet.Builder().subject(String.valueOf(userId)),
                PalikkaJwtType.USER
        ).orElseThrow();
    }

    public String generateSystemToken() {
        return jwtService.sign(
                new JWTClaimsSet.Builder(),
                PalikkaJwtType.SYSTEM
        ).orElseThrow();
    }
}
