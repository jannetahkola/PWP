package fi.jannetahkola.palikka.core.api.exception;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Delegates Spring Security exceptions to the default {@link HandlerExceptionResolver} that can pass them
 * to {@link ControllerAdvice} interceptors. By default, these exceptions are missed by the interceptors
 * because they are thrown before any controller methods are invoked. Delegating them to common handlers
 * helps in aligning their response format with other exceptions.
 *
 * @see <a href="https://www.baeldung.com/spring-security-exceptionhandler#with-exceptionhandler">
 *     Handle Spring Security Exceptions With @ExceptionHandler</a>
 */
@Slf4j
public class DelegatedAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final HandlerExceptionResolver resolver;

    public DelegatedAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }

    @PostConstruct
    void postConstruct() {
        log.info("----- Delegated authentication entrypoint ENABLED -----");
    }
}
