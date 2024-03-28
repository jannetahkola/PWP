package fi.jannetahkola.palikka.core;

import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class TestRedisConfig {
    @Bean
    EmbeddedRedisServer embeddedRedisServer(RedisConnectionFactory redisConnectionFactory,
                                            RedisProperties redisProperties) {
        return new EmbeddedRedisServer(redisConnectionFactory, redisProperties);
    }
}
