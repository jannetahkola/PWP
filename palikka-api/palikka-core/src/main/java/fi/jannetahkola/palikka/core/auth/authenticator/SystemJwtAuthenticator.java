package fi.jannetahkola.palikka.core.auth.authenticator;

import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationDetails;
import fi.jannetahkola.palikka.core.auth.PalikkaSystemAuthenticationToken;
import fi.jannetahkola.palikka.core.auth.jwt.VerifiedJwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SystemJwtAuthenticator implements JwtAuthenticator {
    @Override
    public void authenticate(VerifiedJwt verifiedJwt) {
        PalikkaSystemAuthenticationToken authentication =
                new PalikkaSystemAuthenticationToken(
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
        PalikkaAuthenticationDetails details = new PalikkaAuthenticationDetails();
        details.setToken(verifiedJwt.getToken());
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("System token authenticated successfully with authorities={}", authentication.getAuthorities());
    }
}
