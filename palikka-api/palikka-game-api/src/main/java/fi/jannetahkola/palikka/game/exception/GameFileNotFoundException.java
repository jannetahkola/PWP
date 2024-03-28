package fi.jannetahkola.palikka.game.exception;

public class GameFileNotFoundException extends RuntimeException {
    public GameFileNotFoundException(String message) {
        super(message);
    }
}
