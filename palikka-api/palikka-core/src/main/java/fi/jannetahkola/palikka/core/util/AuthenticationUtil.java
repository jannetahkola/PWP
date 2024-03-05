package fi.jannetahkola.palikka.core.util;

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
        log.debug("AUTHENTICATING");
        jwtService.parse(token)
                .map(claims -> usersClient.getUser(Integer.valueOf(claims.getSubject())))
                .ifPresentOrElse(user -> {
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
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("User with id '{}' authenticated successfully with roles={}", user.getId(), user.getRoles());
                }, () -> log.debug("Authentication failed - invalid token or user doesn't exist"));
    }
}

