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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "User roles")
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserRoleController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final RoleModelAssembler roleModelAssembler;

    @Operation(summary = "Get all roles associated with a user")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleModel.class))))
    @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @GetMapping(
            value = "/{user-id}/roles",
            produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @PreAuthorize(
            "hasAnyRole('ROLE_ADMIN', 'ROLE_SYSTEM') " +
                    "or (hasAnyRole('ROLE_USER', 'ROLE_VIEWER') " +
                    "&& #userId == authentication.principal.id)")
    public ResponseEntity<CollectionModel<RoleModel>> getUserRoles(@PathVariable("user-id") Integer userId) {
        Set<RoleEntity> roleEntities = userRepository.findById(userId)
                .map(UserEntity::getRoles)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));
        return ResponseEntity
                .ok()
                .body(CollectionModel.of(
                        roleEntities.stream().map(roleModelAssembler::toModel).toList(),
                        linkTo(methodOn(UserRoleController.class).getUserRoles(userId)).withSelfRel()
                                // todo template is bad, fix somehow
                                .andAffordance(afford(methodOn(UserRoleController.class).patchUserRoles(userId, null)))));
    }

    @Operation(
            summary = "Modify a user's role associations",
            description = """
                    Applies a list of patch operations to a user's role associations. Supported operations are 'add' and 'delete'.
                    
                    - If the patch operations list references the same role for both actions, the 'delete' action is applied
                    - Patch operations referencing roles that do not exist are ignored
                    - Root users' role associations are not modifiable. Request to do so will result in an error
                    
                    The API may respond with accepted status even if no patches have been applied. This occurs
                     when the patch list doesn't contain any valid modifying operations.
                    """)
    @Parameter(
            in = ParameterIn.PATH,
            name = "id",
            description = "Identifier of the user")
    @ApiResponse(
            responseCode = "202",
            description = "Accepted",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoleModel.class))))
    @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @PatchMapping(
            value = "/{user-id}/roles",
            produces = MediaTypes.HAL_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
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
                .body(CollectionModel.of(updatedUserRoles,
                        linkTo(methodOn(UserRoleController.class).patchUserRoles(userId, null)).withSelfRel()));
    }
}
