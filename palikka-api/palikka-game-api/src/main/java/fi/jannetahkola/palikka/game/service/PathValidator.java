package fi.jannetahkola.palikka.game.service;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class PathValidator {
    public boolean validatePathExistsAndIsAFile(Path pathToFile) {
        return pathToFile.toFile().exists() && pathToFile.toFile().isFile();
    }
}
