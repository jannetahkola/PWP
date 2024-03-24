package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.core.api.exception.BadRequestException;
import fi.jannetahkola.palikka.core.api.exception.ConflictException;
import fi.jannetahkola.palikka.core.api.exception.model.BadRequestErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.ConflictErrorModel;
import fi.jannetahkola.palikka.core.api.exception.model.NotFoundErrorModel;
import fi.jannetahkola.palikka.core.util.AuthorizationUtil;
import fi.jannetahkola.palikka.users.api.user.model.UserModel;
import fi.jannetahkola.palikka.users.api.user.model.UserModelAssembler;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersNotFoundException;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Tag(name = "Users")
@Slf4j
@RestController
@RequestMapping(
        value = "/users",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserRepository userRepository;
    private final UserModelAssembler userModelAssembler;

    @Operation(summary = "Get all users")
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<CollectionModel<UserModel>> getUsers() {
        return ResponseEntity
                .ok(userModelAssembler.toCollectionModel(userRepository.findAll()));
    }

    @Operation(summary = "Get a user")
    @Parameter(
            in = ParameterIn.PATH,
            name = "id",
            description = "Identifier of the user")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = UserModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = NotFoundErrorModel.class)))
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ROLE_SYSTEM', 'ROLE_ADMIN') " +
                    "or (hasRole('ROLE_USER') and #userId == authentication.principal) " +
                    "or (hasRole('ROLE_VIEWER') and #userId == authentication.principal)"
    )
    public ResponseEntity<UserModel> getUser(@PathVariable("id") Integer userId) {
        UserModel userModel = userRepository.findById(userId)
                .map(userModelAssembler::toModel)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));
        return ResponseEntity
                .ok()
                .body(userModel);
    }

    @Operation(summary = "Get current user")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = UserModel.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not found",
            content = @Content(schema = @Schema(implementation = NotFoundErrorModel.class)))
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER')")
    public ResponseEntity<UserModel> getCurrentUser(Authentication authentication) {
        UserModel userModel = userRepository.findById(Integer.valueOf(authentication.getName()))
                .map(userModelAssembler::toModel)
                // todo in this case we should log the hell out
                .orElseThrow(() -> UsersNotFoundException.ofUser(Integer.valueOf(authentication.getName())));
        return ResponseEntity
                .ok()
                .body(userModel);
    }

    @Operation(summary = "Create a user")
    @ApiResponse(
            responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(implementation = UserModel.class)),
            headers = {
                    @Header(
                            name = HttpHeaders.LOCATION,
                            description = "Link to the created user",
                            schema = @Schema(implementation = URI.class))})
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(schema = @Schema(implementation = BadRequestErrorModel.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = @Content(schema = @Schema(implementation = ConflictErrorModel.class)))
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserModel> postUser(@Validated(UserModel.PostGroup.class) @RequestBody UserModel userToPost) {
        if (userRepository.existsByUsername(userToPost.getUsername())) {
            throw new ConflictException(
                    String.format("User with username '%s' already exists", userToPost.getUsername()));
        }

        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hash(userToPost.getPassword(), salt);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userToPost.getUsername());
        userEntity.setActive(userToPost.getActive());
        userEntity.setRoot(false);
        userEntity.setSalt(salt);
        userEntity.setPassword(hash);
        userEntity.setCreatedAt(nowUtcTime());

        UserModel createdUser = userModelAssembler.toModel(userRepository.save(userEntity));

        return ResponseEntity
                .created(createdUser.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(createdUser);
    }

    @Operation(summary = "Update a user")
    @Parameter(
            in = ParameterIn.PATH,
            name = "id",
            description = "Identifier of the user")
    @ApiResponse(
            responseCode = "202",
            description = "Accepted",
            content = @Content(schema = @Schema(implementation = UserModel.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(schema = @Schema(implementation = BadRequestErrorModel.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Conflict",
            content = @Content(schema = @Schema(implementation = ConflictErrorModel.class)))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_USER') and #userId == authentication.principal)")
    public ResponseEntity<UserModel> putUser(@PathVariable("id") Integer userId,
                                             @Validated(UserModel.PutGroup.class) @RequestBody UserModel userToPut,
                                             Authentication authentication) {
        UserEntity existingUserEntity = userRepository
                .findById(userId)
                .orElseThrow(() -> UsersNotFoundException.ofUser(userId));

        if (Boolean.TRUE.equals(existingUserEntity.getRoot())) {
            throw new BadRequestException("Root user not updatable");
        }

        if (userRepository.existsByUsernameExcept(userToPut.getUsername(), existingUserEntity.getUsername())) {
            throw new ConflictException(
                    String.format("User with username '%s' already exists", userToPut.getUsername()));
        }

        // Default to current value, but allow update if current user has privileges (silent)
        Boolean isActive = existingUserEntity.getActive();
        if (userToPut.getActive() != null
                && AuthorizationUtil.hasAnyAuthority(authentication, "ROLE_ADMIN")) {
            isActive = userToPut.getActive();
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setUsername(userToPut.getUsername());
        userEntity.setActive(isActive);
        userEntity.setRoot(existingUserEntity.getRoot());
        userEntity.setSalt(existingUserEntity.getSalt());
        userEntity.setPassword(existingUserEntity.getPassword());
        userEntity.setCreatedAt(existingUserEntity.getCreatedAt());
        userEntity.setLastUpdatedAt(nowUtcTime());
        existingUserEntity.getRoles().forEach(userEntity::addRole);

        if (userToPut.getPassword() != null) {
            // New password can be the same as before, but new salt is generated
            log.info("Updating password for user id '{}'", userEntity.getId());
            String salt = CryptoUtils.generateSalt();
            String hash = CryptoUtils.hash(userToPut.getPassword(), salt);
            userEntity.setSalt(salt);
            userEntity.setPassword(hash);
        }

        UserModel updatedUser = userModelAssembler.toModel(userRepository.save(userEntity));

        return ResponseEntity
                .accepted()
                .body(updatedUser);
    }

    private OffsetDateTime nowUtcTime() {
        return OffsetDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
    }
}
