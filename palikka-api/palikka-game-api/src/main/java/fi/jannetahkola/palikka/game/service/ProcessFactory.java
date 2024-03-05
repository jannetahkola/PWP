package fi.jannetahkola.palikka.game.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class ProcessFactory {
    public Process newGameProcess(String command, Path pathToFile) {
        try {
            return new ProcessBuilder()
                    .command(command.split(" "))
                    .directory(pathToFile.toFile())
                    .start();
        } catch (IOException e) {
            log.error("Game process start failed", e);
            throw new RuntimeException(e);
        }
    }
}
