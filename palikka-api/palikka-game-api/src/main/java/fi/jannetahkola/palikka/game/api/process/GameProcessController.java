package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.api.process.model.GameProcessControlRequest;
import fi.jannetahkola.palikka.game.api.process.model.GameProcessStatusResponse;
import fi.jannetahkola.palikka.game.exception.GameProcessStartException;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/game-api/game/process")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class GameProcessController {
    private final GameProcessService gameProcessService;

    @PostMapping
    public synchronized ResponseEntity<GameProcessStatusResponse> controlProcess(
            @Valid @RequestBody GameProcessControlRequest request) throws GameProcessStartException {
        if (request.getAction().equals(GameProcessControlRequest.Action.START)) {
            if (gameProcessService.initStart()) {
                gameProcessService.startAsync();
                return ResponseEntity.ok(getStatusResponse());
            } else {
                throw new GameProcessStartException("Failed to start");
            }
        } else if (request.getAction().equals(GameProcessControlRequest.Action.STOP)) {
            if (gameProcessService.initStop()) {
                gameProcessService.stopAsync();
                return ResponseEntity.ok(getStatusResponse());
            } else {
                throw new GameProcessStartException("Failed to stop");
            }
        }
        throw new GameProcessStartException("Unknown action");
    }

    @GetMapping
    public ResponseEntity<GameProcessStatusResponse> getServerStatus() {
        return ResponseEntity.ok(getStatusResponse());
    }

    private GameProcessStatusResponse getStatusResponse() {
        return GameProcessStatusResponse.builder()
                .status(gameProcessService.getGameProcessStatus())
                .build();
    }
}
