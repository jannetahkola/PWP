package fi.jannetahkola.palikka.core.util;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.PalikkaAuthenticationDetails;
import fi.jannetahkola.palikka.core.auth.PalikkaSystemAuthenticationToken;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class AuthenticationUtil {
    public void authenticateToken(String token, JwtService jwtService, UsersClient usersClient) {
        log.debug("Authenticating");
        jwtService.parse(token)
                .ifPresentOrElse(claims -> {
                    if (claims.getSubject() != null) {
                        authenticateUserToken(token, claims, usersClient);
                        return;
                    }
                    authenticateSystemToken(token);
                }, () -> log.debug("Authentication failed - invalid token"));
    }

    private void authenticateUserToken(String token, JWTClaimsSet claims, UsersClient usersClient) {
        var userId = Integer.valueOf(claims.getSubject());
        var user = usersClient.getUser(userId);
        if (user == null) {
            log.debug("Authentication failed - user id '{}' doesn't exist", userId);
            return;
        }
        if (Boolean.FALSE.equals(user.getActive())) {
            log.debug("Authentication failed - user id '{}' is inactive", user.getId());
            return;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        user.getRoles()
                .ifPresent(roles -> roles
                        .forEach(role -> authorities.add(new SimpleGrantedAuthority(role))));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);

        PalikkaAuthenticationDetails details = new PalikkaAuthenticationDetails();
        details.setToken(token);
        authentication.setDetails(details);
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("User token with user id '{}' authenticated successfully " +
                "with authorities={}", user.getId(), authentication.getAuthorities());
    }

    private void authenticateSystemToken(String token) {
        PalikkaSystemAuthenticationToken authentication =
                new PalikkaSystemAuthenticationToken(
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));

        PalikkaAuthenticationDetails details = new PalikkaAuthenticationDetails();
        details.setToken(token);
        authentication.setDetails(details);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("System token authenticated successfully with authorities={}", authentication.getAuthorities());
    }
}

