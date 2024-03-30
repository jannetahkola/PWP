package fi.jannetahkola.palikka.users.api.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenEntity;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.exception.UsersLoginFailedException;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import fi.jannetahkola.palikka.users.validation.Password;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Predicate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Authentication")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthenticationController {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

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
            // todo actually now there may be, test and update
            description = "Log in failed. No further information is provided for security reasons",
            content = @Content(
                    schema = @Schema(implementation = ProblemDetail.class),
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    @PostMapping(
            value = "/login",
            produces = MediaTypes.HAL_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = userRepository
                .findByUsername(loginRequest.getUsername())
                .filter(user -> passwordMatches(loginRequest).test(user))
                .filter(user -> isUserActive(loginRequest).test(user))
                .map(user -> {
                    JWTClaimsSet.Builder initialClaims =
                            new JWTClaimsSet.Builder().subject(String.valueOf(user.getId()));
                    return jwtService.sign(initialClaims, PalikkaJwtType.USER)
                            .map(signedToken -> {
                                try {
                                    JWTClaimsSet claims = signedToken.getJWTClaimsSet();
                                    log.debug("Successfully logged in user id={}", claims.getSubject());
                                    return new LoginResponse(
                                            signedToken.serialize(),
                                            claims.getExpirationTime()
                                                    .toInstant().atOffset(ZoneOffset.UTC));
                                } catch (ParseException e) {
                                    log.error("", e);
                                }
                                return null;
                            })
                            .orElse(null);
                })
                .orElseThrow(() -> new UsersLoginFailedException("Login failed"));
        return ResponseEntity.ok(loginResponse.add(
                linkTo(methodOn(AuthenticationController.class).login(null)).withSelfRel()
        ));
    }

    @Operation(summary = "Log out")
    @PostMapping(
            value = "/logout",
            produces = MediaTypes.HAL_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("not hasRole('ROLE_SYSTEM')")
    @SuppressWarnings("squid:S1452") // No model type, links only
    public ResponseEntity<RepresentationModel<?>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String token = authorizationHeader.split("Bearer ")[1];
        // Only get claims instead of verifying since we are already authenticated
        jwtService.getClaims(token).ifPresentOrElse(claims -> {
            String jwtId = claims.getJWTID();
            if (!revokedTokenRepository.existsById(jwtId)) {
                RevokedTokenEntity revokedTokenEntity = new RevokedTokenEntity();
                revokedTokenEntity.setTokenId(jwtId);
                revokedTokenEntity.setTtlSeconds(
                        jwtService.getProperties()
                                .getToken()
                                .getUser()
                                .getSigning()
                                .getValidityTime()
                                .getSeconds());
                revokedTokenRepository.save(revokedTokenEntity);
                log.debug("Successfully revoked token={}", token);
            } else {
                log.debug("Failed to revoke token - already revoked, token={}", token);
            }
        }, () -> log.debug("Failed to revoke token - invalid token"));
        return ResponseEntity.ok(new RepresentationModel<>().add(
                linkTo(methodOn(AuthenticationController.class).logout(null)).withSelfRel()
        ));
    }

    private static Predicate<UserEntity> passwordMatches(LoginRequest loginRequest) {
        return user -> {
            boolean valid = CryptoUtils.validatePassword(loginRequest.getPassword(), user.getSalt(), user.getPassword());
            loginRequest.setPassword(null);
            if (!valid) {
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
    @EqualsAndHashCode(callSuper = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LoginResponse extends RepresentationModel<LoginResponse> {
        @Schema(
                description = "Granted access token",
                example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiIsInB0eXAiOiJVU0VSIiwia2lkIjoiand0LXVzciJ9.eyJpc3MiOiJwYWxpa2thLWRldi11c3IiLCJzdWIiOiIxIiwiZXhwIjoxNzExMjM5Njk5LCJpYXQiOjE3MTEyMzYwOTksImp0aSI6IjZkZWVlNGFjLTNkZDMtNDAyNS04NTZlLTk5ZmFkYjNlMjcxYiJ9.oT-0UTqbrDDkKypwk-reyOJSIC96XioiveVRq55FFcudzzYY0IP2p_413_7-Omf0HwfHYVq9YslwKjbMRMEB1QLd8WhF16OGJVKT1uA4eaWWSWwzGZwxSermhJtLXcz7gR4129WRDGwgyHocWXT0L_-rWiSoYAEsCeboBOYmN5suOr_a0d1iJOvmWM87AERp6aH22qKYYOh8KHRQxvQc6xOIq8c1NMiRBznEhfv4Tuv9Gk7CgOADY0CcpZjVQx0TJLGU3BZVKES6GUatwWCy9CxhuKYFPDbqb4XqJq7RvORf24HSHFqD0zkIcKPPnS1O5bfHJXgXhL4y0BVPhxVARQ")
        @NotBlank
        String token;

        @Schema(description = "Expiry time of the granted access token")
        @NotNull
        OffsetDateTime expiresAt;
    }

    @Schema(description = "Log in parameters")
    @Data
    @Valid
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginRequest {
        @Schema(description = "Unique username of the user")
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z\\d-]{3,20}$")
        String username;

        @Schema(description = "Password of the user")
        @Password
        char[] password;
    }
}
