package fi.jannetahkola.palikka.game.api.game;

import fi.jannetahkola.palikka.game.api.game.model.GameMessage;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

// todo test with expired JWT since WS has a bit different flow?
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {
    private static final Validator VALIDATOR;

    private final SimpMessagingTemplate messagingTemplate;
    private final GameProcessService gameProcessService;

    static {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = validatorFactory.getValidator();
        }
    }

    @PostConstruct
    void postConstruct() {
        gameProcessService.registerInputListener(input -> {
            GameMessage msg = GameMessage.builder()
                    .src(GameMessage.Source.GAME)
                    .typ(GameMessage.Type.LOG)
                    .data(input)
                    .build();
            messagingTemplate.convertAndSend("/topic/game", msg);
        });
    }

    @SubscribeMapping("/game")
    public void subscribeToGame(Principal principal) {
        log.info("New subscription with principal '{}'", principal.getName());

        final List<String> inputList = gameProcessService.copyInputList();

        // Send the history in one batch to avoid any new messages
        // that come to the topic appearing in between the history entries
        log.debug("Sending input history to user={}", principal.getName());
        String inputHistory = String.join("\n", inputList);
        GameMessage inputHistoryMsg = GameMessage.builder()
                .src(GameMessage.Source.SERVER)
                .typ(GameMessage.Type.HISTORY)
                .data(inputHistory)
                .build();
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/reply", inputHistoryMsg);
    }

    @MessageMapping("/game")
    public void handleMessageToGame(@Payload GameMessage msg,
                                    Principal principal) {
        Set<ConstraintViolation<GameMessage>> violations = VALIDATOR.validate(msg);
        if (!violations.isEmpty()) {
            log.debug("Failed to process message with constraint violations: {}", violations);
            return;
        }
        // If process is not up, nothing will happen. Process service will clear the
        // queue next time the process is started.
        log.info("Outputting to game as principal '{}'", principal.getName());
        CompletableFuture.runAsync(() -> {
            log.debug("Passing output message to game process");
            gameProcessService.addOutput(msg.getData());
        });
    }

//    @MessageMapping("/echo")
//    public void echo(@Payload String payload, Principal principal) {
//        log.info("At echo with principal={}", principal);
//        messagingTemplate.convertAndSendToUser(
//                principal.getName(), "/queue/reply", payload);
//    }
}
