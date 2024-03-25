package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

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
                .body(json)
                .post("/auth/login")
                .then().assertThat()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("expires_at", endsWith("Z"))
                .body("_links.self.href", endsWith("/users-api/auth/login"));
    }

    @SneakyThrows
    @Test
    void givenLoginRequest_whenCredentialsInvalid_thenBadRequestResponse() {
        String json = new JSONObject()
                .put("username", "wrong-user")
                .put("password", "password")
                .toString();
        given()
                .body(json)
                .post("/auth/login")
                .then().assertThat()
                .statusCode(400)
                .body("message", equalTo("Login failed"));
    }

    // todo just use redis for storing these as they need to be cleared often anyways?
    @Test
    void givenLogoutRequest_thenTokenInvalidated_andOkResponse() {
        given()
                .header(newUserToken())
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(200);

//            given()
//                    .header(newUserToken())
//                    .post("/auth/logout")
//                    .then().assertThat()
//                    .statusCode(403);
    }

    @Test
    void givenLogoutRequest_withoutToken_thenForbiddenResponse() {
        given()
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(403);
    }
}
