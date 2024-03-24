package fi.jannetahkola.palikka.users.api;

import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import fi.jannetahkola.palikka.users.testutils.TestTokenUtils;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "spring.sql.init.data-locations=",
                "palikka.jwt.keystore.signing.path=keystore-dev.p12",
                "palikka.jwt.keystore.signing.pass=password",
                "palikka.jwt.keystore.signing.type=pkcs12",
                "palikka.jwt.token.user.issuer=palikka-dev-usr",
                "palikka.jwt.token.user.signing.key-alias=jwt-usr",
                "palikka.jwt.token.user.signing.key-pass=password",
                "palikka.jwt.token.user.signing.validity-time=10s",
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD) // Clear db
@SqlForUsers
class CorsIT {
    @Autowired
    TestTokenUtils tokens;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.basePath = "/users-api";
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @SneakyThrows
    @Test
    void givenPostRequest_thenCorrectCorsHeadersAreReturned() {
        JSONObject json = new JSONObject();
        json.put("username", "mock-user");
        json.put("password", "password");
        given()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ORIGIN, "http://localhost")
                .body(json.toString())
                .post("/auth/login")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, equalTo("http://localhost"))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, equalTo("true"));
    }

    @Test
    void givenOptionsRequestToPostEndpoint_thenCorrectCorsHeadersAreReturned() {
        given()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ORIGIN, "http://localhost")
                .options("/auth/login")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, equalTo("http://localhost"))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, equalTo("true"))
                .header(HttpHeaders.ALLOW, equalTo("POST,OPTIONS"));
    }
}
