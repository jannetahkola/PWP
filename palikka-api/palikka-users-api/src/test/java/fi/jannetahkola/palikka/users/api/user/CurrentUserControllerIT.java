package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.endsWith;

class CurrentUserControllerIT extends IntegrationTest {
    @Test
    void givenOptionsRequest_thenAllowedMethodsReturned() {
        given()
                .header(newAdminToken())
                .options("/current-user")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ALLOW, containsString("GET"))
                .header(HttpHeaders.ALLOW, not(containsString("POST")))
                .header(HttpHeaders.ALLOW, not(containsString("PUT")))
                .header(HttpHeaders.ALLOW, not(containsString("PATCH")))
                .header(HttpHeaders.ALLOW, not(containsString("DELETE")));
    }

    @Test
    void givenGetCurrentUserRequest_thenOkResponse() {
        given()
                .header(newAdminToken())
                .get("/current-user")
                .then().assertThat()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("username", equalTo("admin"))
                .body("password", nullValue())
                .body("active", equalTo(true))
                .body("roles", hasSize(0))
                .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN))
                .body("_links.user_roles.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"))
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
    }
}
