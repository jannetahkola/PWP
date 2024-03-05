package fi.jannetahkola.palikka.core.auth;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.core.util.AuthenticationUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class PalikkaAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsersClient usersClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        log.debug("At authentication filter");

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String[] parts = header.split(" ");
        if (parts.length <= 1) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Authenticating request with Authorization header={}", header);

        String token = parts[1].trim();

        AuthenticationUtil.authenticateToken(token, jwtService, usersClient);

        filterChain.doFilter(request, response);
    }
}
