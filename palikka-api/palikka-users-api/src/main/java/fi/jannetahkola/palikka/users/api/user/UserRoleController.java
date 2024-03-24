package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.core.api.exception.BadRequestException;
import fi.jannetahkola.palikka.core.api.exception.model.BadRequestErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.NotFoundErrorModel;
import fi.jannetahkola.palikka.users.api.role.model.RoleModel;
import fi.jannetahkola.palikka.users.api.role.model.RoleModelAssembler;
import fi.jannetahkola.palikka.users.api.user.model.UserRolePatchModel;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.role.RoleRepository;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
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

@Tag(name = "User roles")
@Slf4j
@RestController
@RequestMapping(
        value = "/users",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class UserRoleController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final RoleModelAssembler roleModelAssembler;

    @Operation(summary = "Get a user's roles")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleModel.class))))
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = NotFoundErrorModel.class)))
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

    @Operation(summary = "Update a user's roles")
    @ApiResponse(
            responseCode = "202",
            description = "Accepted",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleModel.class))))
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(schema = @Schema(implementation = BadRequestErrorModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = NotFoundErrorModel.class)))
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
