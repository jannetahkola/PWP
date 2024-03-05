package fi.jannetahkola.palikka.game.process.exception;

public class GameProcessAlreadyActiveException extends RuntimeException {
    public GameProcessAlreadyActiveException(String message) {
        super(message);
    }
}
