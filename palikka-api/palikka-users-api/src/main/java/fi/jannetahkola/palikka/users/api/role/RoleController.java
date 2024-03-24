package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.core.api.exception.model.NotFoundErrorModel;
import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.role.model.RoleModel;
import fi.jannetahkola.palikka.users.api.role.model.RoleModelAssembler;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.role.RoleRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Roles")
@RestController
@RequestMapping(
        value = "/roles",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class RoleController {
    private final RoleRepository roleRepository;
    private final RoleModelAssembler roleModelAssembler;

    @Operation(summary = "Get all roles")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<CollectionModel<RoleModel>> getRoles(Authentication authentication) {
        List<RoleEntity> roles = roleRepository.findAll();
        if (!AuthorizationUtil.hasAuthority(authentication, "ROLE_ADMIN")) {
            roles = roleRepository.findAll().stream()
                    .filter(role -> AuthorizationUtil.hasAuthority(authentication, role.getName()))
                    .toList();
        }
        return ResponseEntity
                .ok(roleModelAssembler.toCollectionModel(roles));
    }

    @Operation(summary = "Get a role")
    @Parameter(
            in = ParameterIn.PATH,
            name = "id",
            description = "Identifier of the role")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = RoleModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = NotFoundErrorModel.class)))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<RoleModel> getRole(@PathVariable("id") Integer roleId, Authentication authentication) {
        RoleModel roleModel = roleRepository.findById(roleId)
                .map(roleModelAssembler::toModel)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));
        // Check that user has either admin role, or the requested role
        if (!AuthorizationUtil.hasAnyAuthority(authentication, "USER_ADMIN", roleModel.getName())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN.value())
                    .build();
        }
        return ResponseEntity
                .ok()
                .body(roleModel);
    }
}
