package fi.jannetahkola.palikka.game.service;

import fi.jannetahkola.palikka.game.config.properties.GameServerProperties;
import fi.jannetahkola.palikka.game.process.GameProcess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerProcessService {
    private static final AtomicReference<GameServerStatus> gameServerStatus = new AtomicReference<>(GameServerStatus.DOWN);
    private static final List<Consumer<String>> gameServerProcessInputListeners = new ArrayList<>();
    private static final GameServerProcessLogger gameServerProcessLogger = new GameServerProcessLogger();

    private final GameServerProperties gameServerProperties;
    private final ProcessFactory processFactory;
    private final PathValidator pathValidator;

    private GameProcess gameProcess;

    /**
     * Checks if the process can be started. If true, sets the status to {@link GameServerStatus#STARTING}.
     * @return True if the process can be started.
     */
    public boolean initStart() {
        boolean result = true;
        if (!GameServerStatus.DOWN.equals(gameServerStatus.get())) {
            result = false;
            log.info("Cannot start - expected game server process to be DOWN, was {}", gameServerStatus.get());
        }
        if (!pathValidator.validatePathExistsAndIsAFile(gameServerProperties.getFile().getPathToJarFile())) {
            // Path is validated actively instead of service start up so newly
            // downloaded server files are accounted for.
            result = false;
            log.info("Cannot start - invalid game server file path");
        }
        if (result)
            gameServerStatus.set(GameServerStatus.STARTING);
        return result;
    }

    /**
     * Checks if the process can be stopped. If true, sets the status to {@link GameServerStatus#STOPPING}.
     * @return True if the process can be stopped.
     */
    public boolean initStop() {
        boolean result = true;
        if (!GameServerStatus.UP.equals(gameServerStatus.get())) {
            result = false;
            log.info("Cannot stop - expected game server process status to be UP, was {}", gameServerStatus.get());
        }
        if (result)
            gameServerStatus.set(GameServerStatus.STOPPING);
        return result;
    }

    /**
     * Starts the process. Due to this being asynchronous, {@link GameServerProcessService#initStop()} must be
     * called before this for synchronous error handling.
     */
    @Async("threadPoolTaskExecutor")
    public void startAsync() {
        var fileProperties = gameServerProperties.getFile();

        GameProcess.GameProcessHooks hooks = GameProcess.GameProcessHooks.builder()
                .onProcessStarted(() -> gameServerStatus.set(GameServerStatus.STARTING))
                .onGameStarted(() -> gameServerStatus.set(GameServerStatus.UP))
                .onGameExited(() -> gameServerStatus.set(GameServerStatus.STOPPING))
                .onProcessExited(() -> gameServerStatus.set(GameServerStatus.DOWN))
                .onInput(input -> {
                    gameServerProcessLogger.log(input);
                    gameServerProcessInputListeners.forEach(listener ->
                            // Publish to listeners asynchronously to be safe
                            CompletableFuture.runAsync(() -> {
                                log.debug("Publishing game server process input to listener");
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
            log.error("Failed to start game server", e);
            gameServerStatus.set(GameServerStatus.DOWN);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the process. Due to this being asynchronous, {@link GameServerProcessService#initStop()} must be
     * called before this for synchronous error handling.
     */
    @Async("threadPoolTaskExecutor")
    public void stopAsync() {
        try {
            if (gameProcess.stop(gameServerProperties.getStopTimeoutInMillis())) {
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
        if (gameProcess == null || gameServerStatus.get().equals(GameServerStatus.DOWN)) return;
        try {
            if (gameProcess.stopForcibly(gameServerProperties.getStopTimeoutInMillis())) {
                log.info("Process forceful stop success");
            } else {
                log.error("Process forceful stop failure - time out");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted during force stop", e);
            Thread.currentThread().interrupt();
        }
    }

    public String getGameServerStatus() {
        return gameServerStatus.get().getValue();
    }

    public void registerInputListener(Consumer<String> listener) {
        gameServerProcessInputListeners.add(listener);
    }

    public enum GameServerStatus {
        UP("up"),
        DOWN("down"),
        STARTING("starting"),
        STOPPING("stopping");

        final String value;

        GameServerStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
