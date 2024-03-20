package fi.jannetahkola.palikka.game.service;

import fi.jannetahkola.palikka.game.exception.GameFileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameFileService {
    private static final AtomicReference<DownloadStatus> downloadStatus = new AtomicReference<>(DownloadStatus.IDLE);

    private final GameFileProcessor gameFileProcessor;

    @Async("threadPoolTaskExecutor")
    public void startDownloadAsync(URL downloadUrl, File toFile) {
        final long startTime = System.currentTimeMillis();

        log.info("Downloading server from URL={} to path={}", downloadUrl, toFile.toPath());
        downloadStatus.set(DownloadStatus.WORKING);

        try {
            gameFileProcessor.downloadFile(downloadUrl, toFile);

            log.info("Server download done ({} ms)", System.currentTimeMillis() - startTime);

            // Optional operation, errors ignored
            gameFileProcessor.acceptEula(toFile);

            downloadStatus.set(DownloadStatus.SUCCESS);
        } catch (Exception e) {
            downloadStatus.set(DownloadStatus.FAILED);
            log.error("Server download failed ({} ms)", System.currentTimeMillis() - startTime, e);
        }
    }

    public String getDownloadStatus() {
        return downloadStatus.getAndUpdate(currentStatus ->
                DownloadStatus.SUCCESS.equals(currentStatus) || DownloadStatus.FAILED.equals(currentStatus)
                        ? DownloadStatus.IDLE
                        : currentStatus)
                .getValue();
    }

    public List<String> getConfig(String pathToDir) {
        try {
            return gameFileProcessor.readFile(pathToDir, "server.properties");
        } catch (IOException e) {
            // Use message from the original exception since we are throwing them ourselves
            throw new GameFileException(e.getMessage(), e);
        }
    }

    public enum DownloadStatus {
        IDLE("idle"),
        WORKING("working"),
        SUCCESS("success"),
        FAILED("failed");

        final String value;

        DownloadStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
