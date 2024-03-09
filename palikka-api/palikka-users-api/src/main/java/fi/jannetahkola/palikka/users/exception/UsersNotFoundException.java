package fi.jannetahkola.palikka.users.exception;

import fi.jannetahkola.palikka.core.api.exception.NotFoundException;

public class UsersNotFoundException extends NotFoundException {
    public UsersNotFoundException(String message) {
        super(message);
    }

    public static UsersNotFoundException ofUser(Integer userId) {
        return new UsersNotFoundException(String.format("User with id '%d' not found", userId));
    }

    public static RuntimeException ofRole(Integer roleId) {
        return new UsersNotFoundException(String.format("Role with id '%d' not found", roleId));
    }
}
