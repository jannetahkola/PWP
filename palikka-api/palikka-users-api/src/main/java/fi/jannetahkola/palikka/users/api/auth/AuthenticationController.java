package fi.jannetahkola.palikka.users.api.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.api.exception.model.BadRequestErrorModel;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.users.data.auth.TokenEntity;
import fi.jannetahkola.palikka.users.data.auth.TokenRepository;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersLoginFailedException;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Predicate;

@Tag(name = "Authentication")
@Slf4j
@RestController
@RequestMapping(
        path = "/auth",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class AuthenticationController {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    // todo tests
    // todo return expiry time so we don't have to parse token in client
    @SecurityRequirements
    @Operation(
            summary = "Log in",
            description = "Attempts to log in a user with the provided " +
                    "credentials. An access token is granted and returned on successful login")
    @ApiResponse(
            responseCode = "200",
            description = "Log in successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Log in failed. No further information is provided for security reasons",
            content = @Content(schema = @Schema(implementation = BadRequestErrorModel.class)))
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = userRepository
                .findByUsername(loginRequest.getUsername())
                .filter(user -> passwordMatches(loginRequest).test(user))
                .filter(user -> isUserActive(loginRequest).test(user))
                .map(user -> {
                    JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
                    claims.subject(String.valueOf(user.getId()));
                    return jwtService.sign(claims, PalikkaJwtType.USER)
                            .map(LoginResponse::new)
                            .orElse(null);
                })
                .orElseThrow(() -> new UsersLoginFailedException("Login failed"));
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Log out")
    @PostMapping("/logout")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        jwtService.parse(authorizationHeader.split("Bearer ")[1]) // Auth filter has validated the format already
                .ifPresent(claims -> {
                    log.debug("Logging out user id '{}'", claims.getSubject());
                    TokenEntity tokenEntity = new TokenEntity();
                    tokenEntity.setTokenId(claims.getJWTID());
                    tokenEntity.setAddedOn(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("UTC")));
                    tokenRepository.save(tokenEntity);
                    log.debug("Logged out user id '{}'", claims.getSubject());
                });
    }

    private static Predicate<UserEntity> passwordMatches(LoginRequest loginRequest) {
        return user -> {
            if (!CryptoUtils.validatePassword(loginRequest.getPassword(), user.getSalt(), user.getPassword())) {
                log.debug("Invalid password provided, username={}", loginRequest.getUsername());
                return false;
            }
            return true;
        };
    }

    private static Predicate<UserEntity> isUserActive(LoginRequest loginRequest) {
        return user -> {
            if (Boolean.FALSE.equals(user.getActive())) {
                log.debug("User not active, username={}", loginRequest.getUsername());
                return false;
            }
            return true;
        };
    }

    @Schema(description = "Successful log in response")
    @Value
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LoginResponse {
        @Schema(
                description = "Granted access token",
                example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiIsInB0eXAiOiJVU0VSIiwia2lkIjoiand0LXVzciJ9.eyJpc3MiOiJwYWxpa2thLWRldi11c3IiLCJzdWIiOiIxIiwiZXhwIjoxNzExMjM5Njk5LCJpYXQiOjE3MTEyMzYwOTksImp0aSI6IjZkZWVlNGFjLTNkZDMtNDAyNS04NTZlLTk5ZmFkYjNlMjcxYiJ9.oT-0UTqbrDDkKypwk-reyOJSIC96XioiveVRq55FFcudzzYY0IP2p_413_7-Omf0HwfHYVq9YslwKjbMRMEB1QLd8WhF16OGJVKT1uA4eaWWSWwzGZwxSermhJtLXcz7gR4129WRDGwgyHocWXT0L_-rWiSoYAEsCeboBOYmN5suOr_a0d1iJOvmWM87AERp6aH22qKYYOh8KHRQxvQc6xOIq8c1NMiRBznEhfv4Tuv9Gk7CgOADY0CcpZjVQx0TJLGU3BZVKES6GUatwWCy9CxhuKYFPDbqb4XqJq7RvORf24HSHFqD0zkIcKPPnS1O5bfHJXgXhL4y0BVPhxVARQ")
        @NotBlank
        String token;
    }

    @Schema(description = "Log in parameters")
    @Data
    @Valid
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginRequest {
        @Schema(description = "Unique username of the user")
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z\\d-]{6,20}$")
        String username;

        @Schema(description = "Password of the user")
        @NotBlank
        @Pattern(regexp = "^[^\\s]{6,20}$")
        String password;
    }
}
