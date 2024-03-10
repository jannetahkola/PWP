package fi.jannetahkola.palikka.game.exception;

import fi.jannetahkola.palikka.core.api.exception.ApiErrorModel;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@EnableDefaultApiExceptionHandling
public class GameApiExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> gameProcessStartException(GameProcessStartException e) {
        log.info("", e);
        return ApiErrorModel
                .badRequest(e)
                .toResponse();
    }
}
