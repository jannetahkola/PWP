package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.role.model.RoleModel;
import fi.jannetahkola.palikka.users.api.role.model.RoleModelAssembler;
import fi.jannetahkola.palikka.users.data.role.RoleEntity;
import fi.jannetahkola.palikka.users.data.role.RoleRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users-api/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {
    private final RoleRepository roleRepository;
    private final RoleModelAssembler roleModelAssembler;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @Cacheable("roleCache")
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    @Cacheable("roleCache")
    public ResponseEntity<RoleModel> getRole(@PathVariable("id") Integer roleId, Authentication authentication) {
        RoleModel roleModel = roleRepository.findById(roleId)
                .map(roleModelAssembler::toModel)
                .orElseThrow(() -> UsersNotFoundException.ofRole(roleId));
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
