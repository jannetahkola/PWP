package fi.jannetahkola.palikka.game.api.game;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/echo")
    public void echo(@Payload String payload, Principal principal) {
        log.info("At echo with principal={}", principal);
        messagingTemplate.convertAndSendToUser(
                principal.getName(), "/queue/reply", payload);
    }
}
