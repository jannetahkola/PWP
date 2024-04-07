package fi.jannetahkola.palikka.game.service.factory;

import fi.jannetahkola.palikka.game.process.GameProcess;
import fi.jannetahkola.palikka.game.process.GameProcessExecutable;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class ProcessFactory {
    public Process newProcess(String command, Path pathToFileDirectory) {
        try {
            return new ProcessBuilder()
                    .command(command.split(" "))
                    .directory(pathToFileDirectory.toFile())
                    .start();
        } catch (IOException e) {
            log.error("Game process start failed", e);
            // TODO Status won't be reset if error, e.g. eula not accepted
            throw new RuntimeException(e);
        }
    }

    public GameProcess newGameProcess(String command,
                                      Path pathToFileDirectory,
                                      GameProcess.GameProcessHooks hooks,
                                      BlockingQueue<String> outputQueue) {
        GameProcessExecutable executable =
                () -> newProcess(command, pathToFileDirectory);
        return new GameProcess(executable, hooks, outputQueue);
    }
}
