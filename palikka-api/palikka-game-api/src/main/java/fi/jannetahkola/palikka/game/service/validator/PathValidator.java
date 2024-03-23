package fi.jannetahkola.palikka.game.service.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Component
public class PathValidator {
    public boolean validatePathExistsAndIsAFile(Path pathToFile) {
        File file = pathToFile.toFile();
        boolean result = file.exists() && file.isFile();
        if (result) {
            File parentDirectory = file.getParentFile();
            log.info("Listing files in '{}':", parentDirectory.getName());
            Arrays.stream(Objects.requireNonNull(file.getParentFile().listFiles()))
                    .toList().forEach(f -> log.info(f.getName()));
        }
        return result;
    }
}
