package fi.jannetahkola.palikka.core.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class AuthorizationUtil {
    public boolean hasAnyAuthority(Authentication authentication, String... authorities) {
        if (authentication == null || authorities.length == 0) return false;
        List<String> anyAuthorityList = Arrays.asList(authorities);
        return authentication
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(anyAuthorityList::contains);
    }

    public boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authority == null) return false;
        return authentication
                .getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(grantedAuthority -> grantedAuthority.equals(authority));
    }
}
