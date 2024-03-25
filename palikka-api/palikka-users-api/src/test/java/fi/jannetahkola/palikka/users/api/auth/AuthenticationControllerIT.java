package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SqlForUsers
class AuthenticationControllerIT extends IntegrationTest {
    @SneakyThrows
    @Test
    void givenLoginRequest_whenCredentialsValid_thenOkResponse() {
        String json = new JSONObject()
                .put("username", "mock-user")
                .put("password", "password")
                .toString();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/auth/login")
                .then().assertThat()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("expires_at", endsWith("Z"))
                .body("_links.self.href", endsWith("/users-api/auth/login"))
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
    }

    @SneakyThrows
    @Test
    void givenLoginRequest_whenCredentialsInvalid_thenBadRequestResponse() {
        String json = new JSONObject()
                .put("username", "wrong-user")
                .put("password", "password")
                .toString();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/auth/login")
                .then().assertThat()
                .statusCode(400)
                .body("detail", equalTo("Login failed"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }

    // todo just use redis for storing these as they need to be cleared often anyways?
    @Test
    void givenLogoutRequest_thenTokenInvalidated_andOkResponse() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(newUserToken())
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);

//            given()
//                    .header(newUserToken())
//                    .post("/auth/logout")
//                    .then().assertThat()
//                    .statusCode(403);
    }

    @Test
    void givenLogoutRequest_withoutToken_thenForbiddenResponse() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(403)
                .body("detail", equalTo("Full authentication is required to access this resource"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }
}
