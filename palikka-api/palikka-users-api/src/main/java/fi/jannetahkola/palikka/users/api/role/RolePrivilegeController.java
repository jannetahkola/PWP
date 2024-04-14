package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModel;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModelAssembler;
import fi.jannetahkola.palikka.users.api.role.model.RolePrivilegePostModel;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeRepository;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.role.RoleRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Tag(name = "Role privileges")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Validated
@Slf4j
public class RolePrivilegeController {
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    private final PrivilegeModelAssembler privilegeModelAssembler;

    @Operation(summary = "Get all privileges associated with a role")
    @GetMapping(
            value = "/{role_id}/privileges",
            produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER', 'ROLE_SYSTEM')")
    public ResponseEntity<CollectionModel<PrivilegeModel>> getRolePrivileges(@PathVariable("role_id") Integer roleId,
                                                                             Authentication authentication) {
        RoleEntity roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));
        if (!AuthorizationUtil.hasAnyAuthority(authentication,
                "ROLE_ADMIN", "ROLE_SYSTEM", roleEntity.getName())) {
            throw new AccessDeniedException("Access Denied");
        }
        List<PrivilegeModel> rolePrivileges = roleEntity.getPrivileges().stream()
                .map(privilege -> privilegeModelAssembler.toModel(privilege, roleId))
                .sorted(Comparator.comparing(PrivilegeModel::getDomain))
                .sorted(Comparator.comparing(PrivilegeModel::getName))
                .toList();
        return ResponseEntity
                .ok()
                .body(CollectionModel.of(
                        rolePrivileges,
                        linkTo(methodOn(RolePrivilegeController.class).getRolePrivileges(roleId, null)).withSelfRel()
                                .andAffordance(afford(methodOn(RolePrivilegeController.class).postRolePrivileges(roleId, null)))
        ));
    }

    @Operation(summary = "Get a privilege associated with a role")
    @Parameter(
            in = ParameterIn.PATH,
            name = "role_id",
            description = "Identifier of the role",
            example = "1")
    @Parameter(
            in = ParameterIn.PATH,
            name = "privilege_id",
            description = "Identifier of the privilege",
            example = "1")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = PrivilegeModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not Found",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @GetMapping(
            value = "/{role_id}/privileges/{privilege_id}",
            produces = {MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER', 'ROLE_SYSTEM')")
    public ResponseEntity<PrivilegeModel> getRolePrivilege(@PathVariable("role_id") Integer roleId,
                                                           @PathVariable("privilege_id") Integer privilegeId,
                                                           Authentication authentication) {
        RoleEntity roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));

        if (!AuthorizationUtil.hasAnyAuthority(authentication,
                "ROLE_ADMIN", "ROLE_SYSTEM", roleEntity.getName())) {
            throw new AccessDeniedException("Access Denied");
        }

        PrivilegeEntity privilegeEntity = roleEntity.getPrivileges()
                .stream()
                .filter(privilege -> privilege.getId().equals(privilegeId))
                .findFirst()
                .orElseThrow(() -> UsersNotFoundException.ofPrivilege(privilegeId));
        return ResponseEntity.ok(privilegeModelAssembler.toModel(privilegeEntity, roleId));
    }

    @Operation(summary = "Create a new privilege association for a role")
    @Parameter(
            in = ParameterIn.PATH,
            name = "role_id",
            description = "Identifier of the role",
            example = "1")
    @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PrivilegeModel.class))))
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
            value = "/{role_id}/privileges",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CollectionModel<PrivilegeModel>> postRolePrivileges(@PathVariable("role_id") Integer roleId,
                                                                              @Valid @RequestBody RolePrivilegePostModel postModel) {
        RoleEntity roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));
        List<PrivilegeModel> updatedRolePrivileges = privilegeRepository.findById(postModel.getPrivilegeId())
                        .map(privilegeEntity -> {
                            log.info("Creating privilege associations for role={}, privileges={}", roleId, postModel.getPrivilegeId());
                            roleEntity.addPrivilege(privilegeEntity);
                            return roleRepository.save(roleEntity)
                                    .getPrivileges().stream()
                                    .map(rolePrivilege -> privilegeModelAssembler.toModel(rolePrivilege, roleId))
                                    .sorted(Comparator.comparing(PrivilegeModel::getDomain))
                                    .sorted(Comparator.comparing(PrivilegeModel::getName))
                                    .toList();
                        })
                        .orElseThrow(() -> UsersNotFoundException.ofPrivilege(postModel.getPrivilegeId()));
        return ResponseEntity
                .status(201)
                .body(CollectionModel.of(updatedRolePrivileges,
                        linkTo(methodOn(RolePrivilegeController.class).postRolePrivileges(roleId, null)).withSelfRel()));
    }

    @Operation(summary = "Delete a privilege association from a role")
    @Parameter(
            in = ParameterIn.PATH,
            name = "role_id",
            description = "Identifier of the role",
            example = "1")
    @Parameter(
            in = ParameterIn.PATH,
            name = "privilege_id",
            description = "Identifier of the privilege",
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
            value = "/{role_id}/privileges/{privilege_id}",
            produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRolePrivileges(@PathVariable("role_id") Integer roleId,
                                                     @PathVariable("privilege_id") Integer privilegeId) {

        RoleEntity roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));

        log.info("Removing privilege associations from role={}, privileges={}", roleEntity, privilegeId);

        roleEntity.getPrivileges().stream()
                .filter(privilege -> privilege.getId().equals(privilegeId))
                .findAny().ifPresent(roleEntity::removePrivilege);
        roleRepository.save(roleEntity);

        return ResponseEntity.noContent().build();
    }
}
