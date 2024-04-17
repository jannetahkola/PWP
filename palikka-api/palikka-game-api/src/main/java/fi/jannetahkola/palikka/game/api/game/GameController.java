package fi.jannetahkola.palikka.game.api.game;

import fi.jannetahkola.palikka.game.api.game.model.GameLifecycleMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameLogMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameUserReplyMessage;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import fi.jannetahkola.palikka.game.websocket.GameMessageValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    /**
     * Destination to target specific users.
     * @see <a href="https://docs.spring.io/spring-framework/reference/web/websocket/stomp/user-destination.html">User destinations</a>
     */
    private static final String DEST_USER = "/queue/reply";

    /**
     * Destination to target the game lifecycle topic.
     */
    private static final String DEST_GAME_LIFECYCLE = "/topic/game/lifecycle";

    /**
     * Destination to target the game logs topic.
     */
    private static final String DEST_GAME_LOGS = "/topic/game/logs";

    private final SimpMessagingTemplate messagingTemplate;
    private final GameProcessService gameProcessService;
    private final GameMessageValidator gameMessageValidator;

    @PostConstruct
    void postConstruct() {
        gameProcessService.registerLifecycleListener(processStatus -> {
            log.debug("Publishing game lifecycle event to subscribers");
            GameLifecycleMessage msg = GameLifecycleMessage.builder()
                    .status(processStatus)
                    .build();
            messagingTemplate.convertAndSend(DEST_GAME_LIFECYCLE, msg);
        });
        gameProcessService.registerInputListener(input -> {
            log.debug("Publishing game input event to subscribers");
            GameLogMessage msg = GameLogMessage.builder()
                    .data(input)
                    .build();
            messagingTemplate.convertAndSend(DEST_GAME_LOGS, msg);
        });
    }

    @SubscribeMapping("/game/lifecycle")
    public void subscribeToGameLifecycle(Principal principal) {
        log.info("New game lifecycle subscription with principal '{}'", principal.getName());
    }

    @SubscribeMapping("/game/logs")
    public void subscribeToGame(Principal principal) {
        log.info("New game logs subscription with principal '{}'", principal.getName());

        final List<String> inputList = gameProcessService.copyInputList();

        // Send the history in one batch to avoid any new messages
        // that come to the topic appearing in between the history entries
        log.debug("Publishing game input history to user={}", principal.getName());
        String inputHistory = String.join("\n", inputList);
        GameUserReplyMessage inputHistoryMsg = GameUserReplyMessage.builder()
                .typ(GameUserReplyMessage.Type.HISTORY)
                .data(inputHistory)
                .build();
        messagingTemplate.convertAndSendToUser(principal.getName(), DEST_USER, inputHistoryMsg);
    }

    @MessageMapping("/game")
    public void handleMessageToGame(@Payload GameOutputMessage msg,
                                    Authentication authentication) {
        // For now all messages are treated as commands, but support
        // for other stuff may come in the future
        try {
            gameMessageValidator.validateMessageIsValid(msg);
            gameMessageValidator.validateGameProcessIsUp();
            gameMessageValidator.validateUserIsAuthorizedForCommand(msg, authentication);
        } catch (GameMessageValidator.GameCommandProcessingException e) {
            GameUserReplyMessage reply = GameUserReplyMessage.builder()
                    .typ(GameUserReplyMessage.Type.ERROR)
                    .data(e.getMessage())
                    .build();
            messagingTemplate.convertAndSendToUser(authentication.getName(), DEST_USER, reply);
            return;
        }

        log.info("Game message validated successfully, outputting " +
                "to game as principal '{}'", authentication.getName());
        CompletableFuture.runAsync(() -> {
            log.debug("Passing output message to game process");
            gameProcessService.addOutput(msg.getData());
        });
    }
}
