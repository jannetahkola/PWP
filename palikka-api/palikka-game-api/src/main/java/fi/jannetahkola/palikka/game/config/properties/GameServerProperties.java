package fi.jannetahkola.palikka.game.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("palikka.game.server")
@Validated
public class GameServerProperties {
    @NotBlank
    String host;

    @NotNull
    int port;
}
