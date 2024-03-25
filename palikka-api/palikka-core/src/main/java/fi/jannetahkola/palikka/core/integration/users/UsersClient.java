package fi.jannetahkola.palikka.core.integration.users;

import java.util.Collection;

public interface UsersClient {
    User getUser(Integer userId);

    Collection<Role> getUserRoles(Integer userId);
}
