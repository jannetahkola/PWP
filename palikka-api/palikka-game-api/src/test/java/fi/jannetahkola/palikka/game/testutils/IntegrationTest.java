package fi.jannetahkola.palikka.game.testutils;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import fi.jannetahkola.palikka.core.testutils.redis.EmbeddedRedisServer;
import fi.jannetahkola.palikka.core.testutils.token.EnableTestTokenGenerator;
import fi.jannetahkola.palikka.core.testutils.token.TestTokenGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@EnableTestTokenGenerator
public abstract class IntegrationTest {
    @Autowired
    protected TestTokenGenerator testTokenGenerator;

    static EmbeddedRedisServer embeddedRedisServer;

    static {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setHost("localhost");
        redisProperties.setPort(6379);
        embeddedRedisServer = new EmbeddedRedisServer(redisProperties);
    }

    // Use a static port because the instance is created again between @Nested test classes
    @RegisterExtension
    protected static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().port(58244).globalTemplating(true))
            .build();

    @BeforeAll
    static void startRedis() {
        embeddedRedisServer.start();
    }

    @AfterAll
    static void stopRedis() {
        embeddedRedisServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("palikka.redis.host", embeddedRedisServer::getHost);
        registry.add("palikka.redis.port", embeddedRedisServer::getPort);
    }
}
