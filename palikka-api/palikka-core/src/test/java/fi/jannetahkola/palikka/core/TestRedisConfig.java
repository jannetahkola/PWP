package fi.jannetahkola.palikka.core;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class TestRedisConfig {
    @Bean
    @DependsOn("spring.data.redis-org.springframework.boot.autoconfigure.data.redis.RedisProperties")
    EmbeddedRedisServer embeddedRedisServer(RedisConnectionFactory redisConnectionFactory,
                                            RedisProperties redisProperties) {
        return new EmbeddedRedisServer(redisConnectionFactory, redisProperties);
    }
}
