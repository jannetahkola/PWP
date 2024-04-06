package fi.jannetahkola.palikka.core.testutils.redis;

import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import redis.embedded.RedisServer;

@Slf4j
public class EmbeddedRedisServer {
    private final RedisProperties redisProperties;
    private final RedisServer redisServer;

    @SneakyThrows
    public EmbeddedRedisServer(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
        this.redisServer = new RedisServer(redisProperties.getPort());
    }

    @SneakyThrows
    public void start() {
        redisServer.start();
        log.info("Embedded Redis STARTED");
    }

    @SneakyThrows
    public void stop() {
        redisServer.stop();
        log.info("Embedded Redis STOPPED");
    }

    public String getHost() {
        return redisProperties.getHost();
    }

    public Integer getPort() {
        return redisProperties.getPort();
    }
}
