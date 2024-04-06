package fi.jannetahkola.palikka.users.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.core.testutils.redis.EmbeddedRedisServer;
import fi.jannetahkola.palikka.core.config.properties.RedisProperties;
import fi.jannetahkola.palikka.core.testutils.token.EnableTestTokenGenerator;
import fi.jannetahkola.palikka.core.testutils.token.TestTokenGenerator;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Sql(
        scripts = {
                "classpath:data/truncate_data.sql",
                "classpath:data/seed_privileges.sql",
                "classpath:data/seed_roles.sql",
                "classpath:data/seed_users.sql",
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@EnableTestTokenGenerator
public abstract class IntegrationTest {
    protected static final int USER_ID_ADMIN = 1;
    protected static final int USER_ID_USER = 2;
    protected static final int USER_ID_VIEWER = 3;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestTokenGenerator testTokenGenerator;

    // Same instance is shared between each test class by using manual lifecycle control instead of @Container
    // https://java.testcontainers.org/test_framework_integration/junit_5/
    // https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#manually-startingstopping-containers
    static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16.2-alpine")
                .withDatabaseName("palikka_users")
                .withUsername("user")
                .withPassword("pass")
                // Initialize schema immediately for Hibernate validation
                .withInitScript("data/schema.sql");
        postgres.start();
    }

    static EmbeddedRedisServer embeddedRedisServer;

    static {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setHost("localhost");
        redisProperties.setPort(6379);
        embeddedRedisServer = new EmbeddedRedisServer(redisProperties);
    }

    @BeforeAll
    static void beforeAll() {
        embeddedRedisServer.start();
    }

    @AfterAll
    static void afterAll() {
        embeddedRedisServer.stop();
    }

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.basePath = "/users-api";
        RestAssured.port = localServerPort;
        RestAssured.config().getObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> objectMapper);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("palikka.redis.host", embeddedRedisServer::getHost);
        registry.add("palikka.redis.port", embeddedRedisServer::getPort);
    }

    protected Header newBearerTokenHeader(Integer userId) {
        return new Header(HttpHeaders.AUTHORIZATION, testTokenGenerator.generateBearerToken(userId));
    }

    protected Header newAdminToken() {
        return newBearerTokenHeader(USER_ID_ADMIN);
    }

    protected Header newUserToken() {
        return newBearerTokenHeader(USER_ID_USER);
    }

    protected Header newViewerToken() {
        return newBearerTokenHeader(USER_ID_VIEWER);
    }

    protected Header newSystemBearerTokenHeader() {
        return new Header(HttpHeaders.AUTHORIZATION, "Bearer " + testTokenGenerator.generateSystemToken());
    }
}
