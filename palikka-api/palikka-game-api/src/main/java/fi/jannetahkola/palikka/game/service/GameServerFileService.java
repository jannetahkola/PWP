package fi.jannetahkola.palikka.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerFileService {
    private static final AtomicReference<DownloadStatus> downloadStatus = new AtomicReference<>(DownloadStatus.IDLE);

    private final GameServerFileProcessor gameServerFileProcessor;

    @Async("threadPoolTaskExecutor")
    public void startDownloadAsync(URI downloadUri, File toFile) {
        final long startTime = System.currentTimeMillis();

        log.info("Start download");
        downloadStatus.set(DownloadStatus.WORKING);

        try {
            log.info("Server download path={}", toFile.toPath());

            gameServerFileProcessor.downloadFile(downloadUri, toFile);

            log.info("Server download done ({} ms)", System.currentTimeMillis() - startTime);

            gameServerFileProcessor.acceptEula(toFile); // Errors ignored, file was still downloaded

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
