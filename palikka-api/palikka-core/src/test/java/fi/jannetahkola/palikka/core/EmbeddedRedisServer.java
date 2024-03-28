package fi.jannetahkola.palikka.core;

import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import redis.embedded.RedisServer;

@Slf4j
public class EmbeddedRedisServer {
    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisServer redisServer;

    @SneakyThrows
    public EmbeddedRedisServer(RedisConnectionFactory redisConnectionFactory,
                               RedisProperties redisProperties) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisServer = new RedisServer(redisProperties.getPort());
    }

    @SneakyThrows
    public void start() {
        redisServer.start();
        redisConnectionFactory.getConnection().serverCommands().flushAll();
        log.info("Embedded Redis STARTED");
    }

    @SneakyThrows
    public void stop() {
        redisServer.stop();
        log.info("Embedded Redis STOPPED");
    }
}
