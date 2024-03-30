package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.api.file.model.*;
import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.exception.GameFileException;
import fi.jannetahkola.palikka.game.service.GameFileService;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/game/files")
@Validated
@RequiredArgsConstructor
public class GameFileController {
    private final GameProperties gameProperties;
    private final GameFileService gameFileService;
    private final GameProcessService gameProcessService;

    @PostMapping("/executable/download")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> downloadExecutable(@Valid @RequestBody GameFileDownloadRequest request) {
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
                .getExecutableDownloadStatus()).withRel("status").toUri().toString();
        return ResponseEntity
                .noContent()
                .header(HttpHeaders.LOCATION, linkToGetDownloadStatus)
                .build();
    }

    @GetMapping("executable/download")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<GameFileDownloadResponse> getExecutableDownloadStatus() {
        GameFileDownloadResponse response =
                GameFileDownloadResponse.builder()
                        .status(gameFileService.getDownloadStatus())
                        .build();
        log.info("Returning download status={}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/executable/meta")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<GameExecutableResponse> getExecutableMetadata() {
        File executableFile = gameProperties.getFile().getPathToJarFile().toFile();
        GameExecutableResponse response = GameExecutableResponse.builder()
                .exists(executableFile.exists())
                .isFile(executableFile.isFile())
                .configuredPath(executableFile.toString())
                .fileSizeMB(
                        executableFile.exists()
                                ? executableFile.length() / ( 1024 * 1024 )
                                : 0
                )
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping(
            value = "/config",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<GameConfigResponse> getConfig() {
        List<String> configLines = gameFileService.getConfig(gameProperties.getFile().getPath());
        GameConfigResponse response = GameConfigResponse.builder().config(configLines).build();
        return ResponseEntity.ok(response);
    }

    @PutMapping(
            value = "/config",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<GameConfigResponse> putConfig(@Valid @RequestBody GameConfigUpdateRequest request) {
        final long startTime = System.currentTimeMillis();
        log.info("Updating config");
        List<String> configLines = gameFileService.writeConfig(gameProperties.getFile().getPath(), request.getConfig());
        log.info("Config update successful ({} ms)", System.currentTimeMillis() - startTime);
        GameConfigResponse response = GameConfigResponse.builder().config(configLines).build();
        return ResponseEntity.accepted().body(response);
    }

    @PutMapping(value = "/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> uploadIcon(@RequestPart("file") MultipartFile multipartFile) {
        // todo default max size is 1MB, test
        final long startTime = System.currentTimeMillis();
        log.info("Uploading icon");
        gameFileService.storeFile(gameProperties.getFile().getPath(), multipartFile);
        log.info("Icon upload successful ({} ms)", System.currentTimeMillis() - startTime);
        return ResponseEntity.accepted().build();
    }
}
