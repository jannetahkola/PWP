package fi.jannetahkola.palikka.users;

import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.core.integration.users.UsersClient;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LocalUsersClient implements UsersClient {
    private final UserRepository userRepository;

    @Override
    public User getUser(Integer userId) {
        return userRepository.findById(userId)
                .map(e -> User.builder()
                        .id(e.getId())
                        .username(e.getUsername())
                        .active(e.getActive())
                        .build())
                .orElse(null);
    }

    @Transactional
    @Override
    public Collection<Role> getUserRoles(Integer userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getRoles)
                .map(roles -> roles.stream()
                        .map(role -> {
                            Set<Privilege> privileges = role.getPrivileges()
                                    .stream()
                                    .map(privilege -> Privilege.builder()
                                            .id(privilege.getId())
                                            .category(privilege.getCategory())
                                            .name(privilege.getName()).build())
                                    .collect(Collectors.toSet());
                            return Role.builder()
                                    .id(role.getId())
                                    .name(role.getName())
                                    .privileges(privileges)
                                    .build();
                        })
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }
}
