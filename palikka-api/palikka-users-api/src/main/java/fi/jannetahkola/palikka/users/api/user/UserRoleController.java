package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.core.api.exception.BadRequestException;
import fi.jannetahkola.palikka.users.api.role.model.RoleModel;
import fi.jannetahkola.palikka.users.api.role.model.RoleModelAssembler;
import fi.jannetahkola.palikka.users.api.user.model.UserRolePostModel;
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
import org.springframework.hateoas.*;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

// todo could make links conditional based on role
// todo move description to README since this is the case for role privileges as well
/**
 * The "GET /roles/{id}" endpoint exists solely to render the link for "DELETE roles/{id}" correctly with HAL-FORMS. Affordances
 * do not render templated URIs - all null values are ignored. Thus adding the DELETE endpoint into the link list of
 * "GET /roles" (where we don't refer to individual roles) would result in a link referring to "DELETE /roles", which
 * isn't a valid endpoint.
 * <br>
 * To make some use of the aforementioned endpoint, roles returned through this controller will always
 * link back here. See {@link RoleModelAssembler#toModel(RoleEntity, Integer)}.
 *
 * @see <a href="https://github.com/spring-projects/spring-hateoas/issues/1728">Path variable resolved as empty string</a>
 */
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
    @Parameter(
            in = ParameterIn.PATH,
            name = "user_id",
            description = "Identifier of the user",
            example = "1")
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
            value = "/{user_id}/roles",
            produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @PreAuthorize(
            "hasAnyRole('ROLE_ADMIN', 'ROLE_SYSTEM') " +
                    "or (" +
                        "hasAnyRole('ROLE_USER', 'ROLE_VIEWER') " +
                            "&& #userId == authentication.principal.id" +
                    ")")
    public ResponseEntity<CollectionModel<RoleModel>> getUserRoles(@PathVariable("user_id") Integer userId) {
        Set<RoleEntity> roleEntities = userRepository.findById(userId)
                .map(UserEntity::getRoles)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));
        List<RoleModel> userRoles = roleEntities.stream()
                .map(roleEntity -> roleModelAssembler.toModel(roleEntity, userId))
                .sorted(Comparator.comparing(RoleModel::getId))
                .toList();
        return ResponseEntity
                .ok()
                .body(CollectionModel.of(
                        userRoles,
                        linkTo(methodOn(UserRoleController.class).getUserRoles(userId)).withSelfRel()
                                .andAffordance(afford(methodOn(UserRoleController.class).postUserRoles(userId, null)))
                ));
    }

    @Operation(summary = "Get a role associated with a user")
    @Parameter(
            in = ParameterIn.PATH,
            name = "user_id",
            description = "Identifier of the user",
            example = "1")
    @Parameter(
            in = ParameterIn.PATH,
            name = "role_id",
            description = "Identifier of the role",
            example = "1")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RoleModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @GetMapping(
            value = "/{user_id}/roles/{role_id}",
            produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @PreAuthorize(
            "hasAnyRole('ROLE_ADMIN') " +
                    "or (" +
                        "hasAnyRole('ROLE_USER', 'ROLE_VIEWER') " +
                            "&& #userId == authentication.principal.id" +
                    ")")
    public ResponseEntity<RoleModel> getUserRole(@PathVariable("user_id") Integer userId,
                                                 @PathVariable("role_id") Integer roleId) {
        RoleEntity roleEntity = userRepository.findById(userId)
                .map(UserEntity::getRoles)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId))
                .stream()
                .filter(role -> role.getId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));
        return ResponseEntity.ok(roleModelAssembler.toModel(roleEntity, userId));
    }

    @Operation(summary = "Create a new role association for a user")
    @Parameter(
            in = ParameterIn.PATH,
            name = "user_id",
            description = "Identifier of the user",
            example = "1")
    @ApiResponse(
            responseCode = "201",
            description = "Created",
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
    @PostMapping(
            value = "/{user_id}/roles",
            produces = MediaTypes.HAL_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CollectionModel<RoleModel>> postUserRoles(@PathVariable("user_id") Integer userId,
                                                                    @Valid @RequestBody UserRolePostModel postModel) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));

        validateTargetUserIsNotRoot(userEntity);

        List<RoleModel> updatedUserRoles = roleRepository.findById(postModel.getRoleId())
                .map(roleEntity -> {
                    log.info("Creating role associations for user={}, roles={}", userId, postModel.getRoleId());
                    userEntity.addRole(roleEntity);
                    return userRepository.save(userEntity)
                            .getRoles().stream()
                            .map(userRole -> roleModelAssembler.toModel(userRole, userId))
                            .sorted(Comparator.comparing(RoleModel::getId))
                            .toList();
                })
                .orElseThrow(() -> UsersNotFoundException.ofRole(postModel.getRoleId()));

        return ResponseEntity
                .status(201)
                .body(CollectionModel.of(updatedUserRoles,
                        linkTo(methodOn(UserRoleController.class).postUserRoles(userId, null)).withSelfRel()));
    }

    @Operation(summary = "Delete a role association from a user")
    @Parameter(
            in = ParameterIn.PATH,
            name = "user_id",
            description = "Identifier of the user",
            example = "1")
    @Parameter(
            in = ParameterIn.PATH,
            name = "role_ids",
            description = "Identifier of the role",
            example = "1")
    @ApiResponse(
            responseCode = "204",
            description = "No Content")
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
    @DeleteMapping(
            value = "/{user_id}/roles/{role_id}",
            produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteUserRoles(@PathVariable("user_id") Integer userId,
                                                @PathVariable("role_id") Integer roleId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));

        validateTargetUserIsNotRoot(userEntity);

        log.info("Removing role associations from user={}, roles={}", userId, roleId);

        userEntity.getRoles().stream()
                .filter(role -> role.getId().equals(roleId))
                .findAny().ifPresent(userEntity::removeRole);
        userRepository.save(userEntity);

        return ResponseEntity.noContent().build();
    }

    private static void validateTargetUserIsNotRoot(UserEntity userEntity) {
        if (Boolean.TRUE.equals(userEntity.getRoot())) {
            throw new BadRequestException("Root user not updatable");
        }
    }
}
