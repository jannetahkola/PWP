package fi.jannetahkola.palikka.users.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) // New context for each test to clear database between them
@ActiveProfiles("test")
public abstract class IntegrationTest {
    protected static final Header CONTENT_TYPE =
            new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    /**
     * Admin user id. See {@link SqlForUsers}
     */
    protected static final int USER_ID_ADMIN = 1;

    /**
     * Non-admin user id. See {@link SqlForUsers}
     */
    protected static final int USER_ID_USER = 2;

    /**
     * Root user id. See {@link SqlForUsers}
     */
    protected static final int USER_ID_VIEWER = 3;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestTokenUtils tokens;

    @BeforeEach
    public void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.basePath = "/users-api";
        RestAssured.port = localServerPort;
        RestAssured.config().getObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> objectMapper);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected RequestSpecification given() {
        return RestAssured.given().header(CONTENT_TYPE);
    }

    protected Header newToken(Integer userId) {
        return new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(userId));
    }

    protected Header newAdminToken() {
        return newToken(USER_ID_ADMIN);
    }

    protected Header newUserToken() {
        return newToken(USER_ID_USER);
    }

    protected Header newViewerToken() {
        return newToken(USER_ID_VIEWER);
    }

    protected Header newSystemToken() {
        return new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateSystemToken());
    }
}
