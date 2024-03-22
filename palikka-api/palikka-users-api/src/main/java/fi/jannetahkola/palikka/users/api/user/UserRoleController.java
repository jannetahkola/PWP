package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.core.api.exception.BadRequestException;
import fi.jannetahkola.palikka.users.api.role.model.RoleModel;
import fi.jannetahkola.palikka.users.api.role.model.RoleModelAssembler;
import fi.jannetahkola.palikka.users.api.user.model.UserRolePatchModel;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.role.RoleRepository;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RestController
@RequestMapping("/users-api/users")
@RequiredArgsConstructor
@Validated
public class UserRoleController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final RoleModelAssembler roleModelAssembler;

    @GetMapping("/{user-id}/roles")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal")
    public ResponseEntity<CollectionModel<RoleModel>> getUserRoles(@PathVariable("user-id") Integer userId) {
        Set<RoleEntity> roleEntities = userRepository.findById(userId)
                .map(UserEntity::getRoles)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));
        return ResponseEntity
                .ok()
                .body(CollectionModel.of(
                        roleEntities.stream().map(roleModelAssembler::toModel).toList(),
                        linkTo(methodOn(UserRoleController.class).getUserRoles(userId)).withSelfRel()));
    }

    @PatchMapping("/{user-id}/roles")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CollectionModel<RoleModel>> patchUserRoles(@PathVariable("user-id") Integer userId,
                                                                     @Valid @RequestBody UserRolePatchModel patchModel) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));

        if (Boolean.TRUE.equals(userEntity.getRoot())) {
            throw new BadRequestException("Root user not updatable");
        }

        Map<UserRolePatchModel.Action, List<UserRolePatchModel.UserRolePatch>> patchesByActionMap =
                patchModel.getPatches().stream()
                        .collect(Collectors.groupingBy(UserRolePatchModel.UserRolePatch::getAction));

        if (patchesByActionMap.containsKey(UserRolePatchModel.Action.ADD)) {
            roleRepository
                    .findAllById(
                            patchesByActionMap.get(UserRolePatchModel.Action.ADD).stream()
                                    .map(UserRolePatchModel.UserRolePatch::getRoleId)
                                    .toList()
                    )
                    .forEach(userEntity::addRole);
        }

        // TODO Note that if both add & delete contain the same role, delete will be applied
        if (patchesByActionMap.containsKey(UserRolePatchModel.Action.DELETE)) {
            roleRepository
                    .findAllById(
                            patchesByActionMap.get(UserRolePatchModel.Action.DELETE).stream()
                                    .map(UserRolePatchModel.UserRolePatch::getRoleId)
                                    .toList()
                    )
                    .forEach(userEntity::removeRole);
        }

        List<RoleModel> updatedUserRoles = userRepository.save(userEntity)
                .getRoles().stream()
                .map(roleModelAssembler::toModel)
                .toList();

        return ResponseEntity
                .accepted()
                .body(CollectionModel.of(updatedUserRoles));
    }
}
