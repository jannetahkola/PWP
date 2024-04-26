package fi.jannetahkola.palikka.users.api;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

class EntryPointControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetEntryPointRequest_whenNoToken_thenOkResponse() {
            given()
                    .get("/")
                    .then().assertThat()
                    .statusCode(200);
        }

        @ParameterizedTest
        @MethodSource("userParams")
        void givenGetEntryPointRequest_whenAnyUserRole_thenOkResponse(Integer user) {
            given()
                    .header(newBearerTokenHeader(user))
                    .get("/")
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenGetEntryPointRequest_whenSystemToken_thenOkResponse() {
            given()
                    .header(newSystemBearerTokenHeader())
                    .get("/")
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
        void givenGetEntryPointRequest_thenCorrectLinksReturned() {
            given()
                    .get("/")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_links.self.href", endsWith("/users-api/"))
                    .body("_links.login.href", endsWith("/users-api/auth/login"))
                    .body("_links.users.href", endsWith("/users-api/users"))
                    .body("_links.roles.href", endsWith("/users-api/roles"))
                    .body("_links.privileges.href", endsWith("/users-api/privileges{?search}"))
                    .body("_links.current_user.href", endsWith("/users-api/current-user"));
        }
    }
}
