package fi.jannetahkola.palikka.core.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "palikka.redis")
public class RedisProperties {
    private String host = "localhost";
    private Integer port = 6379;
}
