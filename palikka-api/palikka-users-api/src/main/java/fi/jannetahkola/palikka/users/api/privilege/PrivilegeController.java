package fi.jannetahkola.palikka.users.api.privilege;

import fi.jannetahkola.palikka.core.api.exception.BadRequestException;
import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModel;
import fi.jannetahkola.palikka.users.api.privilege.model.PrivilegeModelAssembler;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeEntity;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

@Tag(name = "Privileges")
@RestController
@RequestMapping("/privileges")
@RequiredArgsConstructor
@Validated
public class PrivilegeController {
    private static final Pattern VALID_SEARCH_PATTERN = Pattern.compile("^[a-zA-Z\\d]{1,6}$");

    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeModelAssembler privilegeModelAssembler;

    @Operation(
            summary = "Get all privileges",
            description = "Results may be filtered depending on the user's authorities")
    @Parameter(
            in = ParameterIn.QUERY,
            name = "search",
            description = """
                    Results are filtered into those containing the search text either in the privilege's domain or name
                    - Case insensitive
                    - Between 1-6 letters and/or numbers
                    """,
            example = "ban")
    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER')")
    public ResponseEntity<CollectionModel<PrivilegeModel>> getPrivileges(@RequestParam(value = "search", required = false) String search,
                                                                         Authentication authentication) {
        List<PrivilegeEntity> privileges;

        if (search != null) {
            if (!VALID_SEARCH_PATTERN.matcher(search).matches()) {
                // Validated manually instead of Jakarta validation API to avoid ConstraintViolationException.
                // We can't inject the web request object with them in controller advice -> difficulties
                // in producing Problem Details.
                // See https://stackoverflow.com/a/69692047
                throw new BadRequestException("Search query must match: " + VALID_SEARCH_PATTERN.pattern());
            }
            privileges = privilegeRepository
                    .findAllByDomainContainingIgnoreCaseOrNameContainingIgnoreCase(search, search);
        } else {
            privileges = privilegeRepository.findAll();
        }

        if (!AuthorizationUtil.hasAuthority(authentication, "ROLE_ADMIN")) {
            privileges = privileges.stream()
                    .filter(privilege -> AuthorizationUtil.hasAuthority(authentication, privilege.getAsAuthority()))
                    .toList();
        }

        return ResponseEntity.ok()
                .body(privilegeModelAssembler.toCollectionModel(privileges));
    }
}
