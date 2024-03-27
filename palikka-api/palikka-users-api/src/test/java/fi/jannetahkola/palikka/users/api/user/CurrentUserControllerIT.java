package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.endsWith;

class CurrentUserControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetCurrentUserRequest_whenNoToken_thenForbiddenResponse() {
            given()
                    .get("/current-user")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);;
        }

        @Test
        void givenGetCurrentUserRequest_whenSystemToken_thenForbiddenResponse() {
            given()
                    .header(newSystemToken())
                    .get("/current-user")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE);;
        }

        @ParameterizedTest
        @MethodSource("userParams")
        void givenGetCurrentUserRequest_whenAnyRole_thenOkResponse(Integer user) {
            given()
                    .header(newToken(user))
                    .get("/current-user")
                    .then().assertThat()
                    .statusCode(200)
                    .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
        }

        static Stream<Arguments> userParams() {
            return Stream.of(
                    Arguments.of(Named.of("ADMIN", 1)),
                    Arguments.of(Named.of("USER", 2)),
                    Arguments.of(Named.of("VIEWER", 3))
            );
        }
    }

    @Nested
    class ResourceFunctionalityIT {
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
                    .body("roles", hasSize(1))
                    .body("roles", contains("ROLE_ADMIN"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN))
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"))
                    .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
        }
    }
}
