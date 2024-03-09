package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.api.file.model.GameServerFileDownloadRequest;
import fi.jannetahkola.palikka.game.api.file.model.GameServerFileDownloadResponse;
import fi.jannetahkola.palikka.game.config.properties.GameServerProperties;
import fi.jannetahkola.palikka.game.service.GameServerFileService;
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
@RequestMapping("/server")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class GameServerFileController {
    private final GameServerProperties gameServerProperties;
    private final GameServerFileService gameServerFileService;

    @PostMapping("/download")
    public void startDownload(@Valid @RequestBody GameServerFileDownloadRequest request) {
        URI downloadUri = request.getDownloadUri();

        log.info("Downloading server from uri={}", downloadUri.toString());

        gameServerFileService.startDownloadAsync(
                downloadUri, gameServerProperties.getFile().getPathToJarFile().toFile());
    }

    @GetMapping("/download")
    public ResponseEntity<GameServerFileDownloadResponse> getDownloadStatus() {
        GameServerFileDownloadResponse response =
                GameServerFileDownloadResponse.builder()
                        .status(gameServerFileService.getDownloadStatus())
                        .build();
        return ResponseEntity
                .ok()
                .body(response);
    }
}
