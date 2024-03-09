package fi.jannetahkola.palikka.game.service;

import fi.jannetahkola.palikka.game.process.GameProcess;
import fi.jannetahkola.palikka.game.process.GameProcessExecutable;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class ProcessFactory {
    public Process newProcess(String command, Path pathToFile) {
        try {
            return new ProcessBuilder()
                    .command(command.split(" "))
                    .directory(pathToFile.toFile())
                    .start();
        } catch (IOException e) {
            log.error("Game process start failed", e);
            throw new RuntimeException(e); // TODO Status won't be reset if error, e.g. eula not accepted
        }
    }

    public GameProcess newGameProcess(String command,
                                      Path pathToFile,
                                      GameProcess.GameProcessHooks hooks) {
        GameProcessExecutable executable =
                () -> newProcess(command, pathToFile);
        return new GameProcess(executable, hooks);
    }
}
