package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModel;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModelAssembler;
import fi.jannetahkola.palikka.users.api.role.model.RolePrivilegePatchModel;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Role privileges")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Validated
public class RolePrivilegeController {
    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;

    private final PrivilegeModelAssembler privilegeModelAssembler;

    @Operation(summary = "Get all privileges associated with a role")
    @GetMapping(value = "/{id}/privileges", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER', 'ROLE_SYSTEM')")
    public ResponseEntity<CollectionModel<PrivilegeModel>> getRolePrivileges(@PathVariable("id") Integer roleId,
                                                                             Authentication authentication) {
        RoleEntity roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));

        if (!AuthorizationUtil.hasAnyAuthority(authentication,
                "ROLE_ADMIN", "ROLE_SYSTEM", roleEntity.getName())) {
            throw new AccessDeniedException("Access Denied");
        }

        return ResponseEntity
                .ok()
                .body(CollectionModel.of(
                        roleEntity.getPrivileges().stream().map(privilegeModelAssembler::toModel).toList(),
                        linkTo(methodOn(RolePrivilegeController.class).getRolePrivileges(roleId, null)).withSelfRel()
        ));
    }

    @Operation(
            summary = "Modify a role's privilege associations",
            description = """
                    Applies a list of patch operations to a role's privilege associations. Supported operations are 'add' and 'delete'.
                    
                    - If the patch operations list references the same privilege for both actions, the 'delete' action is applied
                    - Patch operations referencing privileges that do not exist are ignored
                    
                    The API may respond with accepted status even if no patches have been applied. This occurs
                     when the patch list doesn't contain any valid modifying operations.
                    """)
    @Parameter(
            in = ParameterIn.PATH,
            name = "id",
            description = "Identifier of the role")
    @ApiResponse(
            responseCode = "202",
            description = "Accepted",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = PrivilegeModel.class))))
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
            value = "/{id}/privileges",
            produces = MediaTypes.HAL_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CollectionModel<PrivilegeModel>> patchRolePrivileges(
            @PathVariable("id") Integer roleId,
            @Valid @RequestBody RolePrivilegePatchModel patchModel) {
        RoleEntity roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));

        Map<RolePrivilegePatchModel.Action, List<RolePrivilegePatchModel.RolePrivilegePatch>> patchesByActionMap =
                patchModel.getPatches().stream()
                        .collect(Collectors.groupingBy(RolePrivilegePatchModel.RolePrivilegePatch::getAction));

        if (patchesByActionMap.containsKey(RolePrivilegePatchModel.Action.ADD)) {
            privilegeRepository
                    .findAllById(
                            patchesByActionMap.get(RolePrivilegePatchModel.Action.ADD).stream()
                                    .map(RolePrivilegePatchModel.RolePrivilegePatch::getPrivilegeId)
                                    .toList())
                    .forEach(roleEntity::addPrivilege);
        }

        if (patchesByActionMap.containsKey(RolePrivilegePatchModel.Action.DELETE)) {
            privilegeRepository
                    .findAllById(
                            patchesByActionMap.get(RolePrivilegePatchModel.Action.DELETE).stream()
                                    .map(RolePrivilegePatchModel.RolePrivilegePatch::getPrivilegeId)
                                    .toList())
                    .forEach(roleEntity::removePrivilege);
        }

        List<PrivilegeModel> updatedRolePrivileges = roleRepository.save(roleEntity)
                .getPrivileges().stream()
                .map(privilegeModelAssembler::toModel)
                .toList();

        return ResponseEntity
                .accepted()
                .body(CollectionModel.of(updatedRolePrivileges,
                        linkTo(methodOn(RolePrivilegeController.class).patchRolePrivileges(roleId, null)).withSelfRel()));
    }
}
