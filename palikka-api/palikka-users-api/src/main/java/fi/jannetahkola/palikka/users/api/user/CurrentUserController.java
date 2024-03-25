package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.core.api.exception.model.NotFoundErrorModel;
import fi.jannetahkola.palikka.users.api.user.model.UserModel;
import fi.jannetahkola.palikka.users.api.user.model.UserModelAssembler;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Current user")
@RestController
@RequestMapping(
        value = "/current-user",
        produces = MediaTypes.HAL_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CurrentUserController {
    private final UserRepository userRepository;
    private final UserModelAssembler userModelAssembler;

    @Operation(
            summary = "Get current user",
            description = "Returns the user authenticated with the provided bearer access token")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = UserModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = NotFoundErrorModel.class)))
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER')")
    @GetMapping
    public ResponseEntity<UserModel> getCurrentUser(Authentication authentication) {
        UserModel userModel = userRepository.findById(Integer.valueOf(authentication.getName()))
                .map(userModelAssembler::toModel)
                .orElseThrow(() -> UsersNotFoundException
                        .ofUser(Integer.valueOf(authentication.getName())));
        return ResponseEntity.ok().body(userModel);
    }
}
