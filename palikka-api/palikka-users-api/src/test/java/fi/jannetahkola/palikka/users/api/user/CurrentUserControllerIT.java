package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.endsWith;

@SqlForUsers
class CurrentUserControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetCurrentUserRequest_whenNoToken_thenForbiddenResponse() {
            given()
                    .get("/current-user")
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetCurrentUserRequest_whenSystemToken_thenForbiddenResponse() {
            given()
                    .header(newSystemToken())
                    .get("/current-user")
                    .then().assertThat()
                    .statusCode(403);
        }

        // todo system token, check why not found returns 403 and fix(?)

        @ParameterizedTest
        @MethodSource("userParams")
        void givenGetCurrentUserRequest_whenAnyRole_thenOkResponse(Integer user) {
            given()
                    .header(newToken(user))
                    .get("/current-user")
                    .then().assertThat()
                    .statusCode(200);
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
                    .body("username", equalTo("mock-user"))
                    .body("password", nullValue())
                    .body("active", equalTo(true))
                    .body("roles", hasSize(1))
                    .body("roles", contains("ROLE_ADMIN"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN))
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"));
        }
    }
}
