package fi.jannetahkola.palikka.game.service;

import fi.jannetahkola.palikka.game.config.properties.GameServerProperties;
import fi.jannetahkola.palikka.game.process.GameProcess;
import fi.jannetahkola.palikka.game.process.GameProcessExecutable;
import fi.jannetahkola.palikka.game.process.exception.GameServerFileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServerProcessService {
    private static final AtomicReference<GameServerStatus> gameServerStatus = new AtomicReference<>(GameServerStatus.DOWN);
    private static final List<Consumer<String>> inputListeners = new ArrayList<>();

    private final GameServerProperties gameServerProperties;
    private final ProcessFactory processFactory;

    private GameProcess gameProcess;

    @Async
    public void start() {
        if (!GameServerStatus.DOWN.equals(gameServerStatus.get())) {
            // TODO throw or something
        }
        if (gameProcess != null && gameProcess.isActive()) {
            // TODO Throw or something
        }
        gameServerStatus.set(GameServerStatus.STARTING);

        GameServerProperties.FileProperties fileProperties = gameServerProperties.getFile();
        Path pathToFile = fileProperties.getPathToFile();
        if (pathToFile.toFile().exists()) {
            throw new GameServerFileNotFoundException(pathToFile);
        }

        GameProcessExecutable executable = () -> processFactory
                .newGameProcess(fileProperties.getStartCommand(), pathToFile);
        GameProcess.GameProcessHooks hooks = GameProcess.GameProcessHooks.builder()
                .onGameStarted(() -> gameServerStatus.set(GameServerStatus.UP))
                .onGameExited(() -> gameServerStatus.set(GameServerStatus.DOWN))
                .onInput(input -> inputListeners.forEach(listener -> listener.accept(input))) // TODO Publish with futures?
                .build();
        gameProcess = new GameProcess(executable, hooks);
        try {
            gameProcess.start();
        } catch (InterruptedException e) {
            log.error("Failed to start game server", e);
            gameServerStatus.set(GameServerStatus.DOWN);
            Thread.currentThread().interrupt();
        }
    }

    public String getGameServerStatus() {
        return gameServerStatus.get().getValue();
    }

    public void registerInputListener(Consumer<String> listener) {
        inputListeners.add(listener);
    }

    public enum GameServerStatus {
        UP("up"),
        DOWN("down"),
        STARTING("starting"),
        STOPPING("stopping");

        String value;

        GameServerStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value.toLowerCase(Locale.ROOT);
        }
    }
}
