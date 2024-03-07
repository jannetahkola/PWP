package fi.jannetahkola.palikka.mock.gamefileserver.web.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("palikka.mock.game-file")
@Validated
public class GameFileServerProperties {
    @NotBlank
    String path;
}
