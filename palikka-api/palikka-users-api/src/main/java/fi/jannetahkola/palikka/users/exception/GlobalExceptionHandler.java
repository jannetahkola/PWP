package fi.jannetahkola.palikka.users.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> handle(NotFoundException e) {
        log.info("Not found exception occurred", e);
        return ApiErrorModel
                .notFound(e)
                .toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> handle(ConflictException e) {
        log.info("Config exception occurred", e);
        return ApiErrorModel
                .conflict(e)
                .toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> handle(MethodArgumentNotValidException e) {
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
}
