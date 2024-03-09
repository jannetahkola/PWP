package fi.jannetahkola.palikka.core.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides default exception handling. Usage:
 * <pre>{@code
 * @ControllerAdvice
 * @EnableDefaultApiExceptionHandling
 * public class MyExceptionHandler {}
 * }</pre>
 *
 * Overriding individual handlers is possible with a following class definition:
 * <pre>{@code
 * @ControllerAdvice
 * @Order(Ordered.HIGHEST_PRECEDENCE)
 * public class MyExceptionHandler {}
 * }</pre>
 */
@Slf4j
@ControllerAdvice
public class DefaultApiExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> accessDeniedException(AccessDeniedException e) {
        log.info("Access denied exception occurred", e); // TODO Check log levels
        return ApiErrorModel
                .forbidden(e)
                .toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> notFoundException(NotFoundException e) {
        log.info("Not found exception occurred", e);
        return ApiErrorModel
                .notFound(e)
                .toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> conflictException(ConflictException e) {
        log.info("Conflict exception occurred", e);
        return ApiErrorModel
                .conflict(e)
                .toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.info("Method argument not valid exception occurred", e);
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(fieldError -> String.format("%s: %s",
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        return ApiErrorModel
                .badRequest(e)
                .withMessage(errorMessage)
                .toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.info("HTTP message not readable exception occurred", e);
        return ApiErrorModel
                .badRequest(e)
                .withMessage("Invalid request")
                .toResponse();
    }

    @ExceptionHandler // TODO test that this isn't triggered before others when default handler used in another service
    public ResponseEntity<ApiErrorModel> unhandledException(Exception e) {
        log.info("Unhandled exception occurred", e);
        return ApiErrorModel
                .internalServerError(e)
                .withMessage("Something went wrong")
                .toResponse();
    }
}
