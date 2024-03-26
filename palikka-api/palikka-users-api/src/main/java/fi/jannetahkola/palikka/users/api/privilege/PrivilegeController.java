package fi.jannetahkola.palikka.users.api.privilege;

import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModel;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModelAssembler;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Privileges")
@RestController
@RequestMapping("/privileges")
@RequiredArgsConstructor
@Validated
public class PrivilegeController {
    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeModelAssembler privilegeModelAssembler;

    @Operation(summary = "Get all privileges", description = "Results may be filtered depending on the user's authorities")
    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER')")
    public ResponseEntity<CollectionModel<PrivilegeModel>> getPrivileges(Authentication authentication) {
        List<PrivilegeEntity> privileges = privilegeRepository.findAll();
        if (!AuthorizationUtil.hasAuthority(authentication, "ROLE_ADMIN")) {
            privileges = privileges.stream()
                    .filter(privilege -> AuthorizationUtil.hasAuthority(authentication, privilege.getAsAuthority()))
                    .toList();
        }
        return ResponseEntity.ok()
                .body(privilegeModelAssembler.toCollectionModel(privileges));
    }
}
