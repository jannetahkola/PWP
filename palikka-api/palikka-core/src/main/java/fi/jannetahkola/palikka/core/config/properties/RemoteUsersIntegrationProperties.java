package fi.jannetahkola.palikka.core.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Data
@ConfigurationProperties("palikka.integration.users-api")
@Validated
public class RemoteUsersIntegrationProperties {
    @NotNull
    private URI baseUri;
}
