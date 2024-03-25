package fi.jannetahkola.palikka.core.api.exception;

import fi.jannetahkola.palikka.core.api.exception.model.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

import static fi.jannetahkola.palikka.core.api.exception.ExceptionUtil.errorResponseOf;

// TODO Check exception log levels
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
    @PostConstruct
    void postConstruct() {
        log.info("----- Default API exception handling ENABLED -----");
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> badRequestException(BadRequestException e) {
        log.debug("Bad request exception occurred", e);
        return errorResponseOf(new BadRequestErrorModel(e.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> accessDeniedException(AccessDeniedException e) {
        log.info("Access denied exception occurred", e);
        return errorResponseOf(new ForbiddenErrorModel(e));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> noResourceFoundException(NoResourceFoundException e) {
        log.debug("No resource found exception occurred", e);
        return errorResponseOf(new NotFoundErrorModel(e));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> notFoundException(NotFoundException e) {
        log.info("Not found exception occurred", e);
        return errorResponseOf(new NotFoundErrorModel(e));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> conflictException(ConflictException e) {
        log.info("Conflict exception occurred", e);
        return errorResponseOf(new ConflictErrorModel(e));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.info("Method argument not valid exception occurred", e);
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(fieldError -> String.format("%s: %s",
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        return errorResponseOf(new BadRequestErrorModel(errorMessage));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.info("Method argument type mismatch exception occurred", e);
        return errorResponseOf(new BadRequestErrorModel("Invalid request"));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.info("HTTP message not readable exception occurred", e);
        return errorResponseOf(new BadRequestErrorModel("Invalid request"));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> httpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.info("HTTP media type not supported exception occurred", e);
        return errorResponseOf(new BadRequestErrorModel(e.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> unhandledException(Exception e) {
        log.info("Unhandled exception occurred", e);
        return errorResponseOf(new ServerErrorModel("Something went wrong"));
    }
}
