package fi.jannetahkola.palikka.mock.gamefileserver.web.config;

import fi.jannetahkola.palikka.mock.gamefileserver.web.config.properties.GameFileServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GameFileServerProperties.class)
public class GameFileServerConfig {
}
