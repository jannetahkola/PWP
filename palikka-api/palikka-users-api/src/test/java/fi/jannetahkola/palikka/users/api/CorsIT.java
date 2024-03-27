package fi.jannetahkola.palikka.users.api;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class CorsIT extends IntegrationTest {

    @SneakyThrows
    @Test
    void givenPostRequest_thenCorrectCorsHeadersAreReturned() {
        JSONObject json = new JSONObject();
        json.put("username", "admin");
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
