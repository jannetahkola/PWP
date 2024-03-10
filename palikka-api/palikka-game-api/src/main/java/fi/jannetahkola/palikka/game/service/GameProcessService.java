package fi.jannetahkola.palikka.game.service;

import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import fi.jannetahkola.palikka.game.process.GameProcess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameProcessService {
    private static final AtomicReference<GameProcessStatus> gameProcessStatus = new AtomicReference<>(GameProcessStatus.DOWN);
    private static final List<Consumer<String>> gameProcessInputListeners = new ArrayList<>();
    private static final GameProcessLogger GAME_PROCESS_LOGGER = new GameProcessLogger();

    private final GameProperties gameProperties;
    private final ProcessFactory processFactory;
    private final PathValidator pathValidator;

    private GameProcess gameProcess;

    /**
     * Checks if the process can be started. If true, sets the status to {@link GameProcessStatus#STARTING}.
     * @return True if the process can be started.
     */
    public boolean initStart() {
        boolean result = true;
        if (!GameProcessStatus.DOWN.equals(gameProcessStatus.get())) {
            result = false;
            log.info("Cannot start - expected game process to be DOWN, was {}", gameProcessStatus.get());
        }
        if (!pathValidator.validatePathExistsAndIsAFile(gameProperties.getFile().getPathToJarFile())) {
            // Path is validated actively instead of service start up so newly
            // downloaded server files are accounted for.
            result = false;
            log.info("Cannot start - invalid game file path");
        }
        if (result)
            gameProcessStatus.set(GameProcessStatus.STARTING);
        return result;
    }

    /**
     * Checks if the process can be stopped. If true, sets the status to {@link GameProcessStatus#STOPPING}.
     * @return True if the process can be stopped.
     */
    public boolean initStop() {
        boolean result = true;
        if (!GameProcessStatus.UP.equals(gameProcessStatus.get())) {
            result = false;
            log.info("Cannot stop - expected game process status to be UP, was {}", gameProcessStatus.get());
        }
        if (result)
            gameProcessStatus.set(GameProcessStatus.STOPPING);
        return result;
    }

    /**
     * Starts the process. Due to this being asynchronous, {@link GameProcessService#initStop()} must be
     * called before this for synchronous error handling.
     */
    @Async("threadPoolTaskExecutor")
    public void startAsync() {
        var fileProperties = gameProperties.getFile();

        GameProcess.GameProcessHooks hooks = GameProcess.GameProcessHooks.builder()
                .onProcessStarted(() -> gameProcessStatus.set(GameProcessStatus.STARTING))
                .onGameStarted(() -> gameProcessStatus.set(GameProcessStatus.UP))
                .onGameExited(() -> gameProcessStatus.set(GameProcessStatus.STOPPING))
                .onProcessExited(() -> gameProcessStatus.set(GameProcessStatus.DOWN))
                .onInput(input -> {
                    GAME_PROCESS_LOGGER.log(input);
                    gameProcessInputListeners.forEach(listener ->
                            // Publish to listeners asynchronously to be safe
                            CompletableFuture.runAsync(() -> {
                                log.debug("Publishing game process input to listener");
                                listener.accept(input);
                            }));
                })
                .build();
        gameProcess = processFactory.newGameProcess(
                fileProperties.getStartCommand(),
                fileProperties.getPathToJarFileDirectory(),
                hooks);
        try {
            gameProcess.start();
        } catch (InterruptedException e) {
            log.error("Failed to start game process", e);
            gameProcessStatus.set(GameProcessStatus.DOWN);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the process. Due to this being asynchronous, {@link GameProcessService#initStop()} must be
     * called before this for synchronous error handling.
     */
    @Async("threadPoolTaskExecutor")
    public void stopAsync() {
        try {
            Duration stopTimeout = gameProperties.getProcess().getStopTimeout();
            if (gameProcess.stop(stopTimeout.toMillis())) {
                log.info("Process graceful stop success");
            } else {
                log.error("Process graceful stop failure - time out");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted during graceful stop", e);
            Thread.currentThread().interrupt();
        }
    }

    public void stopForcibly() {
        if (gameProcess == null || gameProcessStatus.get().equals(GameProcessStatus.DOWN)) return;
        try {
            Duration stopTimeout = gameProperties.getProcess().getStopTimeout();
            if (gameProcess.stopForcibly(stopTimeout.toMillis())) {
                log.info("Process forceful stop success");
            } else {
                log.error("Process forceful stop failure - time out");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted during force stop", e);
            Thread.currentThread().interrupt();
        }
    }

    public String getGameProcessStatus() {
        return gameProcessStatus.get().getValue();
    }

    public void registerInputListener(Consumer<String> listener) {
        gameProcessInputListeners.add(listener);
    }

    public enum GameProcessStatus {
        UP("up"),
        DOWN("down"),
        STARTING("starting"),
        STOPPING("stopping");

        final String value;

        GameProcessStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
