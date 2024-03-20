package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.api.file.model.GameConfigResponse;
import fi.jannetahkola.palikka.game.api.file.model.GameExecutableResponse;
import fi.jannetahkola.palikka.game.api.file.model.GameFileDownloadRequest;
import fi.jannetahkola.palikka.game.api.file.model.GameFileDownloadResponse;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.exception.GameFileException;
import fi.jannetahkola.palikka.game.service.GameFileService;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/game-api/game/files")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class GameFileController {
    private final GameProperties gameProperties;
    private final GameFileService gameFileService;
    private final GameProcessService gameProcessService;

    // todo test if file overriding works
    // todo prefix with with 'server' or 'executable'
    @PostMapping("/download")
    public ResponseEntity<Void> startDownload(@Valid @RequestBody GameFileDownloadRequest request) {
        URL downloadUrl;
        try {
            downloadUrl = URI.create(request.getDownloadUrl()).toURL();
        } catch (Exception e) {
            throw new GameFileException("Invalid download URL", e);
        }

        if (!gameProcessService.isDown()) {
            throw new GameFileException("Game files cannot be modified when game status is not DOWN");
        }

        gameFileService.startDownloadAsync(
                downloadUrl, gameProperties.getFile().getPathToJarFile().toFile());

        String linkToGetDownloadStatus = linkTo(methodOn(GameFileController.class)
                .getDownloadStatus()).withRel("status").toUri().toString();
        return ResponseEntity
                .noContent()
                .header(HttpHeaders.LOCATION, linkToGetDownloadStatus)
                .build();
    }

    @GetMapping("/download")
    public ResponseEntity<GameFileDownloadResponse> getDownloadStatus() {
        GameFileDownloadResponse response =
                GameFileDownloadResponse.builder()
                        .status(gameFileService.getDownloadStatus())
                        .build();
        log.info("Returning download status={}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/executable/meta")
    public ResponseEntity<GameExecutableResponse> getExecutableMetadata() {
        File executableFile = gameProperties.getFile().getPathToJarFile().toFile();
        GameExecutableResponse response = GameExecutableResponse.builder()
                .exists(executableFile.exists())
                .isFile(executableFile.isFile())
                .configuredPath(executableFile.toString())
                .fileSizeMB(
                        executableFile.exists()
                                ? "" + executableFile.length() * ( 1024 * 1024 )
                                : "0"
                )
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<GameConfigResponse> getConfig() {
        List<String> configLines = gameFileService.getConfig(gameProperties.getFile().getPath());
        GameConfigResponse response = GameConfigResponse.builder().config(configLines).build();
        return ResponseEntity.ok(response);
    }

    // todo update config endpoint
}
