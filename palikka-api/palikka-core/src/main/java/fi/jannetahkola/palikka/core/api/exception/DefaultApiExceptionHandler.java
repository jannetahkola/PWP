package fi.jannetahkola.palikka.core.api.exception;

import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides default exception handlers for custom exceptions in the core lib, as well as all
 * Spring MVC exceptions by extending {@link ResponseEntityExceptionHandler}. All error responses
 * are processed through this superclass and thus follow the Problem Details spec.
 * <br><br>
 *
 * Usage:
 * <ol>
 *     <li>Extend the default handler</li>
 *     <ul><li>
 *         Makes the methods from {@link ResponseEntityExceptionHandler}
 *         available for overriding or adding new handlers that adhere to
 *         the Problem Details spec.
 *     </li></ul>
 *     <li>Annotate the class with {@link EnableDefaultApiExceptionHandling}</li>
 *     <ul><li>
 *         Makes the {@link DelegatedAuthenticationEntryPoint} bean available for
 *         registration in a security config.
 *     </li></ul>
 * </ol>
 *
 * Example usage:
 * <pre>{@code
 * @ControllerAdvice
 * @EnableDefaultApiExceptionHandling
 * public class MyExceptionHandler implements DefaultApiExceptionHandler {}
 * }</pre>
 * <br>
 *
 * Overriding individual handlers for Spring MVC exceptions  can be done by overriding handler methods
 * from {@link ResponseEntityExceptionHandler}. See
 * {@link DefaultApiExceptionHandler#handleMethodArgumentNotValid(MethodArgumentNotValidException, HttpHeaders, HttpStatusCode, WebRequest)}.
 * Custom exceptions can be overridden by creating a new {@link ExceptionHandler} method.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">Problem Details for HTTP APIs</a>
 * @see <a href="https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html">
 *     Spring Error Responses</a>
 */
@Slf4j
@ControllerAdvice
public class DefaultApiExceptionHandler extends ResponseEntityExceptionHandler {
    @PostConstruct
    void postConstruct() {
        log.info("----- Default API exception handler ENABLED -----");
    }

    //
    // Spring Security exceptions
    //

    @ExceptionHandler
    public ResponseEntity<Object> accessDeniedException(AccessDeniedException e, WebRequest request) {
        log.info("Access denied exception occurred", e);
        HttpStatus status = HttpStatus.FORBIDDEN;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> authenticationException(AuthenticationException e, WebRequest request) {
        log.info("Authentication exception occurred", e);
        HttpStatus status = HttpStatus.FORBIDDEN;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    //
    // Spring MVC exceptions
    //

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@Nonnull MethodArgumentNotValidException ex,
                                                                  @Nonnull HttpHeaders headers,
                                                                  @Nonnull HttpStatusCode status,
                                                                  @Nonnull WebRequest request) {
        log.info("Method argument not valid exception occurred", ex);
        // Provide a better detail message than just "Invalid request content."
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String detailMessage = fieldErrors.stream()
                .map(fieldError -> String.format("%s: %s",
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        ProblemDetail body = this.createProblemDetail(ex, status, detailMessage, null, null, request);
        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@Nonnull HttpMessageNotReadableException ex,
                                                                  @Nonnull HttpHeaders headers,
                                                                  @Nonnull HttpStatusCode status,
                                                                  @Nonnull WebRequest request) {
        log.info("HTTP message not readable exception occurred", ex);
        Throwable cause = ExceptionUtil.getOriginalCause(ex.getCause());
        if (cause == null || cause.getMessage() == null) {
            cause = new Throwable("HTTP message not readable");
        }
        ProblemDetail body = this.createProblemDetail(ex, status, cause.getMessage(), null, null, request);
        return this.handleExceptionInternal(ex, body, headers, status, request);
    }

    //
    // Custom exceptions
    //

    @ExceptionHandler
    public ResponseEntity<Object> badRequestException(BadRequestException e, WebRequest request) {
        log.debug("Bad request exception occurred", e);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> notFoundException(NotFoundException e, WebRequest request) {
        log.info("Not found exception occurred", e);
        HttpStatus status = HttpStatus.NOT_FOUND;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> conflictException(ConflictException e, WebRequest request) {
        log.info("Conflict exception occurred", e);
        HttpStatus status = HttpStatus.CONFLICT;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    @ExceptionHandler
    public ResponseEntity<Object> unhandledException(Exception e, WebRequest request) {
        log.error("Unhandled exception occurred", e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }

    @Override
    @Nonnull
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body,
                                                          @Nonnull HttpHeaders headers,
                                                          @Nonnull HttpStatusCode statusCode,
                                                          @Nonnull WebRequest request) {
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
