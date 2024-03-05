package fi.jannetahkola.palikka.users.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException ofUser(Integer userId) {
        return new NotFoundException(String.format("User with id '%d' not found", userId));
    }

    public static RuntimeException ofRole(Integer roleId) {
        return new NotFoundException(String.format("Role with id '%d' not found", roleId));
    }
}
