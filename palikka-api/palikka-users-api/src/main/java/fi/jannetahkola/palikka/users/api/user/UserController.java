package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.user.model.UserModel;
import fi.jannetahkola.palikka.users.api.user.model.UserModelAssembler;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users-api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserRepository userRepository;
    private final UserModelAssembler userModelAssembler;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CollectionModel<UserModel>> getUsers() {
        return ResponseEntity
                .ok(userModelAssembler.toCollectionModel(userRepository.findAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SYSTEM') or hasRole('ROLE_ADMIN') or (hasRole('ROLE_USER') and #userId == authentication.principal)")
    public ResponseEntity<UserModel> getUser(@PathVariable("id") Integer userId) {
        UserModel userModel = userRepository.findById(userId)
                .map(userModelAssembler::toModel)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));
        return ResponseEntity
                .ok()
                .body(userModel);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserModel> postUser(@Validated(UserModel.PostGroup.class) @RequestBody UserModel userToPost) {
        if (userRepository.existsByUsername(userToPost.getUsername())) {
            throw new ConflictException(
                    String.format("User with username '%s' already exists", userToPost.getUsername()));
        }

        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hash(userToPost.getPassword(), salt);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userToPost.getUsername());
        userEntity.setActive(userToPost.getActive());
        userEntity.setSalt(salt);
        userEntity.setPassword(hash);

        UserModel createdUser = userModelAssembler.toModel(userRepository.save(userEntity));

        return ResponseEntity
                .created(createdUser.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(createdUser);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_USER') and #userId == authentication.principal)")
    public ResponseEntity<UserModel> putUser(@PathVariable("id") Integer userId,
                                             @Validated(UserModel.PutGroup.class) @RequestBody UserModel userToPut,
                                             Authentication authentication) {
        UserEntity existingUserEntity = userRepository
                .findById(userId)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));

        if (userRepository.existsByUsernameExcept(userToPut.getUsername(), existingUserEntity.getUsername())) {
            throw new ConflictException(
                    String.format("User with username '%s' already exists", userToPut.getUsername()));
        }

        // Default to current value, but allow update if current user has privileges (silent)
        Boolean isActive = existingUserEntity.getActive();
        if (userToPut.getActive() != null && AuthorizationUtil.hasAnyAuthority(authentication, "ROLE_ADMIN")) {
            isActive = userToPut.getActive();
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setUsername(userToPut.getUsername());
        userEntity.setActive(isActive);
        userEntity.setSalt(existingUserEntity.getSalt());
        userEntity.setPassword(existingUserEntity.getPassword());

        if (userToPut.getPassword() != null) {
            // TODO Password updated at timestamp
            // TODO Don't allow setting the same password?
            log.info("Updating password for user id '{}'", userEntity.getId());
            String salt = CryptoUtils.generateSalt();
            String hash = CryptoUtils.hash(userToPut.getPassword(), salt);
            userEntity.setSalt(salt);
            userEntity.setPassword(hash);
        }

        UserModel updatedUser = userModelAssembler.toModel(userRepository.save(userEntity));

        return ResponseEntity
                .accepted()
                .body(updatedUser);
    }
}
