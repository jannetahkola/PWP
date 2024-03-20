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
        log.debug("At authentication filter for request {} {}",
                request.getMethod(), request.getRequestURI());

        if (request.getRequestURI().startsWith("/ws")) { // todo should make this configurable and set it here and in security config
            log.debug("Abort, open path detected");
            filterChain.doFilter(request, response);
            return;
        }

        String token;
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            String parameter = request.getParameter("token");
            if (!StringUtils.hasText(parameter)) {
                log.debug("Abort, could not parse auth token from header or parameters");
                filterChain.doFilter(request, response);
                return;
            }
            token = parameter;
            log.debug("Authenticating request with parameter={}", token);
        } else {
            String[] parts = header.split(" ");
            if (parts.length <= 1) {
                log.debug("Abort, malformed authorization header");
                filterChain.doFilter(request, response);
                return;
            }

            token = parts[1].trim();
            log.debug("Authenticating request with header={}", token);
        }

        AuthenticationUtil.authenticateToken(token, jwtService, usersClient);

        filterChain.doFilter(request, response);
    }
}
