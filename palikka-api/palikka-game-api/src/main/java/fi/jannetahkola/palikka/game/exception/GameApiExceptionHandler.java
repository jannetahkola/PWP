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
        log.info("Game process start exception occurred", e);
        return ApiErrorModel.badRequest(e).toResponse();
    }

    @ExceptionHandler
    public ResponseEntity<ApiErrorModel> gameFileException(GameFileException e) {
        log.info("Game file exception occurred", e);
        return ApiErrorModel.badRequest(e).toResponse();
    }
}
