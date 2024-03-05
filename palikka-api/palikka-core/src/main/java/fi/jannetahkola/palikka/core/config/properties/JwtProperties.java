package fi.jannetahkola.palikka.core.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@ToString(exclude = "keystorePass")
@ConfigurationProperties("palikka.jwt")
@Validated
public class JwtProperties {

    @NotBlank
    String keystorePath;

    @NotBlank
    String keystorePass;

    @NotBlank
    String keystoreType;

    @NotNull
    TokenProperties token;

    @Data
    @Valid
    @ToString(exclude = "keyPass")
    public static class TokenProperties {
        @NotBlank
        String keyAlias;

        @NotBlank
        String keyPass;

        @NotBlank
        String issuer;

        @NotNull
        Duration validityTime;
    }
}
