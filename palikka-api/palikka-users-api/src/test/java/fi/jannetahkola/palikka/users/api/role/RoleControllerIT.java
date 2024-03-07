package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SqlForUsers
class RoleControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetRoleRequest_whenNoTokenOrRole_thenForbiddenRequest() {
            given()
                    .get("/roles/1")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .get("/roles/1")
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetRoleRequest_whenLimitedRole_andRequestedForThatRole_thenOkResponse() {
            given()
                    .header(newUserToken())
                    .get("/roles/2")
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenGetRolesRequest_whenNoToken_thenForbiddenRequest() {
            given()
                    .get("/roles")
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetRolesRequest_whenLimitedRole_thenResultsFiltered_andOkResponse() {
            given()
                    .header(newUserToken())
                    .get("/roles")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.roles", hasSize(1))
                    .body("_embedded.roles[0].name", equalTo("ROLE_USER"));
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenGetRolesRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/roles")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.roles", hasSize(2))
                    .body("_links.self.href", endsWith("/roles"));
        }

        @Test
        void givenGetRoleRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/roles/1")
                    .then().assertThat()
                    .statusCode(200)
                    .body("id", equalTo(1))
                    .body("name", equalTo("ROLE_ADMIN"))
                    .body("description", equalTo("Access to everything"))
                    .body("_links.self.href", endsWith("/roles/1"));
        }

        @Test
        void givenGetRoleRequest_whenRoleNotFound_thenNotFoundResponse() {
            given()
                    .header(newAdminToken())
                    .get("/roles/3")
                    .then().assertThat()
                    .statusCode(404)
                    .body("message", equalTo("Role with id '3' not found"));
        }
    }
}