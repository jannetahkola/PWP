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
@ConfigurationProperties("palikka.jwt")
@Validated
public class JwtProperties {
    /**
     * Token configurations. By default, nothing is configured.
     */
    @NotNull
    TokenPropertiesGroup token = new TokenPropertiesGroup();

    /**
     * Key store configuration. By default, nothing is configured.
     */
    @NotNull
    JwtProperties.KeyStorePropertiesGroup keystore = new KeyStorePropertiesGroup();

    @Data
    @Valid
    public static class KeyStorePropertiesGroup {
        /**
         * Required when {@link TokenProperties#getSigning()} has been configured.
         */
        KeyStoreProperties signing;

        /**
         * Required when {@link TokenProperties#getVerification()} has been configured.
         */
        KeyStoreProperties verification;
    }

    @Data
    @Valid
    public static class TokenPropertiesGroup {
        /**
         * Configuration to support user tokens.
         */
        TokenProperties user;

        /**
         * Configuration to support system tokens.
         */
        TokenProperties system;
    }

    @Data
    @Valid
    public static class TokenProperties {
        /**
         * Configures signing support, i.e. the service can both produce and consume signed tokens. If this is provided,
         * {@link TokenProperties#getVerification()} is ignored.
         */
        TokenKeyProperties signing;

        /**
         * Configures verification support, i.e. the service can consume signed tokens.
         */
        TokenKeyProperties verification;

        @NotBlank
        String issuer;
    }

    @Data
    @Valid
    @ToString(exclude = "keyPass")
    public static class TokenKeyProperties {
        @NotBlank
        String keyAlias;

        String keyPass;

        /**
         * Required when {@link TokenProperties#getSigning()} is configured.
         */
        Duration validityTime;
    }

    @Data
    @Valid
    @ToString(exclude = "pass")
    public static class KeyStoreProperties {
        @NotBlank
        String path;

        @NotBlank
        String pass;

        @NotBlank
        String type;
    }
}
