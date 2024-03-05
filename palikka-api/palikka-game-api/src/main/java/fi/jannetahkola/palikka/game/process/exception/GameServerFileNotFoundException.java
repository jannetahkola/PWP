package fi.jannetahkola.palikka.game.process.exception;

import java.nio.file.Path;

public class GameServerFileNotFoundException extends RuntimeException {
    public GameServerFileNotFoundException(Path path) {
        super(String.format("Game server file not found at '%s'", path));
    }
}
