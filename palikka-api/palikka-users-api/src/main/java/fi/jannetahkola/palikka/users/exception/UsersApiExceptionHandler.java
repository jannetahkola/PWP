package fi.jannetahkola.palikka.users.exception;

import fi.jannetahkola.palikka.core.api.exception.DefaultApiExceptionHandler;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
@EnableDefaultApiExceptionHandling
public class UsersApiExceptionHandler extends DefaultApiExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<Object> loginFailedException(UsersLoginFailedException e, WebRequest request) {
        log.debug("Log in failed exception occurred", e);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ProblemDetail body = this.createProblemDetail(e, status, e.getMessage(), null, null, request);
        return this.handleExceptionInternal(e, body, new HttpHeaders(), status, request);
    }
}
