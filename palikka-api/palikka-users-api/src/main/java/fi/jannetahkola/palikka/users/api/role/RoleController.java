package fi.jannetahkola.palikka.users.api.role;

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
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {
    private final RoleRepository roleRepository;
    private final RoleModelAssembler roleModelAssembler;

    @Operation(summary = "Get all roles", description = "Results may be filtered depending on the user's authorities")
    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER')")
    public ResponseEntity<CollectionModel<RoleModel>> getRoles(Authentication authentication) {
        List<RoleEntity> roles = roleRepository.findAll();
        if (!AuthorizationUtil.hasAuthority(authentication, "ROLE_ADMIN")) {
            roles = roles.stream()
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
            description = "Not Found",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @GetMapping(value = "/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER')")
    public ResponseEntity<RoleModel> getRole(@PathVariable("id") Integer roleId, Authentication authentication) {
        RoleModel roleModel = roleRepository.findById(roleId)
                .map(roleModelAssembler::toModel)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));
        // Check that user has either admin role, or the requested role
        if (!AuthorizationUtil.hasAnyAuthority(authentication, "USER_ADMIN", roleModel.getName())) {
            throw new AccessDeniedException("Access Denied");
        }
        return ResponseEntity
                .ok()
                .body(roleModel);
    }
}
