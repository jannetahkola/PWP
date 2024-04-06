package fi.jannetahkola.palikka.core.auth.authenticator;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationDetails;
import fi.jannetahkola.palikka.core.auth.PalikkaPrincipal;
import fi.jannetahkola.palikka.core.auth.jwt.VerifiedJwt;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class UserJwtAuthenticator implements JwtAuthenticator {
    private final UsersClient usersClient;

    @Override
    public void authenticate(VerifiedJwt verifiedJwt) {
        log.debug("Authenticating user token");
        Authentication authentication = authenticateInternal(verifiedJwt.getClaims());
        if (authentication instanceof
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
            // Store original token for validating session expiry after WS session is established
            PalikkaAuthenticationDetails details = new PalikkaAuthenticationDetails();
            details.setToken(verifiedJwt.getToken());
            usernamePasswordAuthenticationToken.setDetails(details);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("User token authenticated successfully with principal={} authorities={}, details={}",
                    authentication.getPrincipal(),
                    authentication.getAuthorities(),
                    authentication.getDetails());
        }
    }

    private Authentication authenticateInternal(JWTClaimsSet claims) {
        if (claims.getSubject() == null) {
            log.debug("Authentication failed - no subject in token");
            return null;
        }

        var userId = Integer.valueOf(claims.getSubject());
        var user = usersClient.getUser(userId);
        if (user == null) {
            log.debug("Authentication failed - user id '{}' doesn't exist", userId);
            return null;
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            log.debug("Authentication failed - user id '{}' is inactive", user.getId());
            return null;
        }

        // Add roles and privileges to authorities
        Collection<Role> userRoles = usersClient.getUserRoles(userId);
        List<GrantedAuthority> authorities = new ArrayList<>();
        userRoles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getName()));
            role.getPrivileges().forEach(privilege ->
                    authorities.add(new SimpleGrantedAuthority(
                            privilege.getDomain() + "_" + privilege.getName())));
        });

        PalikkaPrincipal principal = new PalikkaPrincipal(userId, user.getUsername());
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
