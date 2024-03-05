package fi.jannetahkola.palikka.users.api.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
public class LoginController {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping
    public void login(@RequestBody LoginRequest loginRequest) {
        userRepository
                .findByUsername(loginRequest.getUsername())
                .filter(user -> CryptoUtils.validatePassword(loginRequest.getPassword(), user.getSalt(), user.getPassword()))
                .map(user -> {
                    JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
                    claims.subject(String.valueOf(user.getId()));
                    String token = jwtService.sign(claims).orElseThrow();
                    LoginResponse loginResponse = new LoginResponse();
                    loginResponse.setToken(token);
                    return loginResponse;
                })
                .orElseThrow(); // TODO Custom exception + handler
    }

    @Data
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
