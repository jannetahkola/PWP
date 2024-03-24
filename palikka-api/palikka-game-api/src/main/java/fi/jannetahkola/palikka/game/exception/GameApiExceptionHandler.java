package fi.jannetahkola.palikka.game.exception;

import fi.jannetahkola.palikka.core.api.exception.model.BadRequestErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.ErrorModel;
import fi.jannetahkola.palikka.core.config.meta.EnableDefaultApiExceptionHandling;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static fi.jannetahkola.palikka.core.api.exception.ExceptionUtil.errorResponseOf;

@Slf4j
@ControllerAdvice
@EnableDefaultApiExceptionHandling
public class GameApiExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<ErrorModel> gameProcessStartException(GameProcessStartException e) {
        log.info("Game process start exception occurred", e);
        return errorResponseOf(new BadRequestErrorModel(e));
    }

    @ExceptionHandler
    public ResponseEntity<ErrorModel> gameFileException(GameFileException e) {
        log.info("Game file exception occurred", e);
        return errorResponseOf(new BadRequestErrorModel(e));
    }
}
