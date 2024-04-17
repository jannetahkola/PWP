package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import fi.jannetahkola.palikka.game.util.GameCommandUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class GameMessageValidator {
    private static final Validator VALIDATOR;

    static {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = validatorFactory.getValidator();
        }
    }

    private final GameProcessService gameProcessService;
    private final UsersClient usersClient;

    public void validateMessageIsValid(GameOutputMessage msg) throws GameCommandProcessingException {
        Set<ConstraintViolation<GameOutputMessage>> violations = VALIDATOR.validate(msg);
        if (!violations.isEmpty()) {
            log.debug("Failed to process message with constraint violations={}", violations);
            throw new GameCommandProcessingException("Invalid message");
        }
    }

    public void validateGameProcessIsUp() throws GameCommandProcessingException {
        if (!gameProcessService.isUp()) {
            // If process is not up, nothing will happen. Process service will clear the
            // queue next time the process is started.
            throw new GameCommandProcessingException("Cannot process message - game is not UP");
        }
    }

    public void validateUserIsAuthorizedForCommand(GameOutputMessage msg, Authentication authentication) throws GameCommandProcessingException {
        String normalizedCommand = GameCommandUtil.normalizeCommand(msg.getData());
        if (!GameCommandUtil.authorizeCommand(usersClient, authentication, normalizedCommand)) {
            throw new GameCommandProcessingException("Access denied to command='" + msg.getData() + "'");
        }
    }

    public static class GameCommandProcessingException extends Exception {
        public GameCommandProcessingException(String message) {
            super(message);
        }
    }
}
