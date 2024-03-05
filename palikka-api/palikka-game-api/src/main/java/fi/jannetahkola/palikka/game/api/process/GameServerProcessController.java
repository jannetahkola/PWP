package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.api.process.model.GameServerProcessStatusResponse;
import fi.jannetahkola.palikka.game.service.GameServerProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/process") // TODO Change the other controller to /server/file
@Validated
@RequiredArgsConstructor
public class GameServerProcessController {
    private final GameServerProcessService gameServerProcessService;

    @PostMapping
    public void startServer() {
        gameServerProcessService.start();
    }

    @GetMapping
    public ResponseEntity<GameServerProcessStatusResponse> getServerStatus() {
        GameServerProcessStatusResponse response = GameServerProcessStatusResponse.builder()
                .status(gameServerProcessService.getGameServerStatus())
                .build();
        return ResponseEntity
                .ok(response);
    }
}
