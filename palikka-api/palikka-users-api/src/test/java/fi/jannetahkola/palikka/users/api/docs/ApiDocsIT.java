package fi.jannetahkola.palikka.users.api.docs;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true",
                "spring.sql.init.data-locations="
        })
class ApiDocsIT extends IntegrationTest {
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
