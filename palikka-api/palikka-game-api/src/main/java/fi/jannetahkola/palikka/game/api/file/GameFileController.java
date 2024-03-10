package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.api.file.model.GameFileDownloadRequest;
import fi.jannetahkola.palikka.game.api.file.model.GameFileDownloadResponse;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.service.GameFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/game-api/game/files")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class GameFileController {
    private final GameProperties gameProperties;
    private final GameFileService gameFileService;

    @PostMapping("/download")
    public void startDownload(@Valid @RequestBody GameFileDownloadRequest request) {
        URI downloadUri = request.getDownloadUri();

        log.info("Downloading server from uri={}", downloadUri.toString());

        gameFileService.startDownloadAsync(
                downloadUri, gameProperties.getFile().getPathToJarFile().toFile());
    }

    @GetMapping("/download")
    public ResponseEntity<GameFileDownloadResponse> getDownloadStatus() {
        GameFileDownloadResponse response =
                GameFileDownloadResponse.builder()
                        .status(gameFileService.getDownloadStatus())
                        .build();
        return ResponseEntity
                .ok()
                .body(response);
    }
}
