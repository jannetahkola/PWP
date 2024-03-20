package fi.jannetahkola.palikka.game.exception;

public class GameFileException extends RuntimeException {
    public GameFileException(String message) {
        super(message);
    }

    public GameFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
