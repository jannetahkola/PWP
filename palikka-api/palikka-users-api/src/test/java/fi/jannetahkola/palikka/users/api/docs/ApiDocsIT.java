package fi.jannetahkola.palikka.users.api.docs;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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
class ApiDocsIT {
    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.basePath = "/users-api";
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void testApiDocsAvailable() {
        given()
                .get("/v3/api-docs")
                .then().assertThat()
                .statusCode(200)
                .body("openapi", not(emptyOrNullString()));
    }

    @Test
    void testSwaggerUiAvailable() {
        given()
                .get("/swagger-ui/index.html")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.TEXT_HTML_VALUE))
                .body(not(emptyOrNullString()));
    }
}
