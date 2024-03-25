package fi.jannetahkola.palikka.users;

import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocalUsersClient implements UsersClient {
    private final UserRepository userRepository;

    @Transactional // Required for some reason to lazily fetch the roles
    @Override
    public User getUser(Integer userId) {
        return userRepository.findById(userId)
                .map(e -> User.builder()
                        .id(e.getId())
                        .username(e.getUsername())
                        .active(e.getActive())
                        .roles(e.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet()))
                        .build())
                .orElse(null);
    }

    @Override
    public Collection<Role> getUserRoles(Integer userId) {
        // todo
        return null;
    }
}
