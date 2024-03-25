package fi.jannetahkola.palikka.users.api;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.endsWith;

@SqlForUsers
class EntryPointControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @ParameterizedTest
        @MethodSource("userParams")
        void givenGetEntryPointRequest_whenAnyUserRole_thenOkResponse(Integer user) {
            given()
                    .header(newToken(user))
                    .get("/")
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenGetEntryPointRequest_whenSystemToken_thenOkResponse() {
            given()
                    .header(newSystemToken())
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
                    .header(newAdminToken())
                    .get("/")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_links.self.href", endsWith("/users-api/"))
                    .body("_links.users.href", endsWith("/users-api/users"))
                    .body("_links.current_user.href", endsWith("/users-api/current-user"));
        }
    }
}
