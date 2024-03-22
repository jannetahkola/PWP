package fi.jannetahkola.palikka.users.api.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.core.auth.jwt.PalikkaJwtType;
import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users-api/auth/login")
@RequiredArgsConstructor
public class LoginController {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    // todo tests
    @PostMapping // todo return expiry time so we don't have to parse token in client
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = userRepository
                .findByUsername(loginRequest.getUsername())
                .filter(user -> {
                    if (!CryptoUtils.validatePassword(loginRequest.getPassword(), user.getSalt(), user.getPassword())) {
                        log.debug("Invalid password for username '{}'", loginRequest.getUsername());
                        return false;
                    }
                    return true;
                })
                .filter(UserEntity::getActive)
                .map(user -> {
                    JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
                    claims.subject(String.valueOf(user.getId()));
                    String token = jwtService.sign(claims, PalikkaJwtType.USER).orElseThrow();
                    return new LoginResponse(token);
                })
                .orElseThrow();// TODO Custom exception + handler
        return ResponseEntity
                .ok(loginResponse);
    }

    @Value
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class LoginResponse {
        String token;
    }

    @Data
    @Valid
    public static class LoginRequest {
        @NotBlank
        String username;

        @NotBlank
        String password;
    }
}
