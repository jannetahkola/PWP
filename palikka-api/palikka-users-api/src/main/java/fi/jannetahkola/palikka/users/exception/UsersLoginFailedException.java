package fi.jannetahkola.palikka.users.exception;

public class UsersLoginFailedException extends RuntimeException {
    public UsersLoginFailedException(String message) {
        super(message);
    }
}
