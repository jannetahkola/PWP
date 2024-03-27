package fi.jannetahkola.palikka.game.testutils;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import fi.jannetahkola.palikka.core.EmbeddedRedisServer;
import fi.jannetahkola.palikka.core.EnableRedisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@EnableRedisTestSupport
public abstract class IntegrationTest {
    @Autowired
    EmbeddedRedisServer embeddedRedisServer;

    @RegisterExtension
    protected static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().globalTemplating(true))
            .build();

    @BeforeEach
    void startRedis() {
        embeddedRedisServer.start();
    }

    @AfterEach
    void stopRedis() {
        embeddedRedisServer.stop();
    }
}
