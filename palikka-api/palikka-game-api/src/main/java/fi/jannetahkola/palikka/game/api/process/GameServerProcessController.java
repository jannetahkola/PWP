package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.api.process.model.GameServerProcessControlRequest;
import fi.jannetahkola.palikka.game.api.process.model.GameServerProcessStatusResponse;
import fi.jannetahkola.palikka.game.exception.GameServerProcessStartException;
import fi.jannetahkola.palikka.game.service.GameServerProcessService;
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
public class GameServerProcessController {
    private final GameServerProcessService gameServerProcessService;

    @PostMapping
    public synchronized ResponseEntity<GameServerProcessStatusResponse> controlProcess(
            @Valid @RequestBody GameServerProcessControlRequest request) throws GameServerProcessStartException {
        if (request.getAction().equals(GameServerProcessControlRequest.Action.START)) {
            if (gameServerProcessService.initStart()) {
                gameServerProcessService.startAsync();
                return ResponseEntity.ok(getStatusResponse());
            } else {
                throw new GameServerProcessStartException("Failed to start");
            }
        } else if (request.getAction().equals(GameServerProcessControlRequest.Action.STOP)) {
            if (gameServerProcessService.initStop()) {
                gameServerProcessService.stopAsync();
                return ResponseEntity.ok(getStatusResponse());
            } else {
                throw new GameServerProcessStartException("Failed to stop");
            }
        }
        throw new GameServerProcessStartException("Unknown action");
    }

    @GetMapping
    public ResponseEntity<GameServerProcessStatusResponse> getServerStatus() {
        return ResponseEntity.ok(getStatusResponse());
    }

    private GameServerProcessStatusResponse getStatusResponse() {
        return GameServerProcessStatusResponse.builder()
                .status(gameServerProcessService.getGameServerStatus())
                .build();
    }
}
