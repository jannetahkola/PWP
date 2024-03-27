package fi.jannetahkola.palikka.users.api.privilege;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
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

@SqlForUsers
class PrivilegeControllerIT extends IntegrationTest {

    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetPrivilegesRequest_whenNoToken_thenForbiddenResponse() {
            given()
                    .get("/privileges")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenGetPrivilegesRequest_whenSystemToken_thenForbiddenResponse() {
            given()
                    .header(newSystemToken())
                    .get("/privileges")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @ParameterizedTest
        @MethodSource("usersWithRolesAllowedToGetPrivileges")
        void givenGetPrivilegesRequest_whenAllowedRole_thenOkResponse(Integer user) {
            given()
                    .header(newToken(user))
                    .get("/privileges")
                    .then().assertThat()
                    .statusCode(200)
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
        }

        static Stream<Arguments> usersWithRolesAllowedToGetPrivileges() {
            return Stream.of(
                    Arguments.of(Named.of("ADMIN", USER_ID_ADMIN)),
                    Arguments.of(Named.of("USER", USER_ID_USER)),
                    Arguments.of(Named.of("VIEWER", USER_ID_VIEWER))
            );
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenGetPrivilegesRequest_whenLimitedRole_thenResultsFiltered_andOkResponse() {
            given()
                    .header(newUserToken())
                    .get("/privileges")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.privileges", hasSize(4))
                    .body("_links.self.href", endsWith("/users-api/privileges{?search}"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
        }

        @Test
        void givenGetPrivilegesRequest_whenSearchQueryProvided_thenFilteredBySearchQuery_andOkResponse() {
            given()
                    .header(newAdminToken())
                    .param("search", "ban")
                    .get("/privileges")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.privileges", hasSize(3))
                    .body("_links.self.href", endsWith("/users-api/privileges{?search}"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
        }

        @ParameterizedTest
        @MethodSource("invalidSearchQueries")
        void givenGetPrivilegesRequest_whenInvalidSearchQueryProvided_thenBadRequestResponse(String searchQuery) {
            given()
                    .header(newAdminToken())
                    .param("search", searchQuery)
                    .get("/privileges")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", containsString("Search query must match"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        static Stream<Arguments> invalidSearchQueries() {
            return Stream.of(
                    Arguments.of(Named.of("too short", "")),
                    Arguments.of(Named.of("too long", "1234567")),
                    Arguments.of(Named.of("invalid characters", "test_")),
                    Arguments.of(Named.of("invalid characters", "test@")),
                    Arguments.of(Named.of("invalid characters", "test "))
            );
        }
    }
}
