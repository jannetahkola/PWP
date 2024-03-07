package fi.jannetahkola.palikka.mock.gamefileserver.web;

import fi.jannetahkola.palikka.mock.gamefileserver.web.config.properties.GameFileServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class GameFileController {
    private final GameFileServerProperties gameFileServerProperties;

    @GetMapping("/server")
    public ResponseEntity<Resource> getServerFile() {
        try {
            File file = new File(gameFileServerProperties.getPath());

            log.info("Loading file={}", file.getAbsolutePath());

            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=server.jar");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn("File not found", e);
            return ResponseEntity
                    .notFound()
                    .build();
        }
    }
}
