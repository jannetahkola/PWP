package fi.jannetahkola.palikka.core.auth.authenticator;

import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
public class JwtAuthenticationProvider {
    private final Map<PalikkaJwtType, JwtAuthenticator> authenticators = new EnumMap<>(PalikkaJwtType.class);
    private final JwtService jwtService;
    private final UsersClient usersClient;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtAuthenticationProvider(JwtService jwtService,
                                     UsersClient usersClient,
                                     RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.usersClient = usersClient;
        this.revokedTokenRepository = revokedTokenRepository;
        registerAuthenticators();
    }

    public void authenticate(String token) {
        log.debug("Authenticating");
        jwtService.parse(token).ifPresentOrElse(verifiedJwt -> {
            if (revokedTokenRepository.existsById(verifiedJwt.getClaims().getJWTID())) {
                log.debug("Authentication failed - revoked token, token={}", token);
                return;
            }
            PalikkaJwtType tokenType = verifiedJwt.getType();
            if (!authenticators.containsKey(tokenType)) {
                log.debug("Authentication failed - no authenticator registered for token type={}, token={}", tokenType, token);
                return;
            }
            authenticators.get(tokenType).authenticate(verifiedJwt);
        }, () -> log.debug("Authentication failed - invalid token, token={}", token));
    }

    private void registerAuthenticators() {
        if (jwtService.consumesTokenOfType(PalikkaJwtType.USER)) {
            authenticators.putIfAbsent(PalikkaJwtType.USER, new UserJwtAuthenticator(usersClient));
        }
        if (jwtService.consumesTokenOfType(PalikkaJwtType.SYSTEM)) {
            authenticators.putIfAbsent(PalikkaJwtType.SYSTEM, new SystemJwtAuthenticator());
        }
        log.debug("Registered {} authenticator(s) with types={}", authenticators.size(), authenticators.keySet());
    }
}
