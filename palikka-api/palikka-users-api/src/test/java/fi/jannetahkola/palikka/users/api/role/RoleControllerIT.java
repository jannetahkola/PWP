package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class RoleControllerIT extends IntegrationTest {
    @Test
    void givenAllRolesOptionsRequest_thenAllowedMethodsReturned() {
        given()
                .header(newAdminToken())
                .options("/roles")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ALLOW, containsString("GET"))
                .header(HttpHeaders.ALLOW, not(containsString("POST")))
                .header(HttpHeaders.ALLOW, not(containsString("PUT")))
                .header(HttpHeaders.ALLOW, not(containsString("PATCH")));
    }

    @Test
    void givenSingleRoleOptionsRequest_thenAllowedMethodsReturned() {
        given()
                .header(newAdminToken())
                .options("/roles/1")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ALLOW, containsString("GET"))
                .header(HttpHeaders.ALLOW, not(containsString("POST")))
                .header(HttpHeaders.ALLOW, not(containsString("PUT")))
                .header(HttpHeaders.ALLOW, not(containsString("PATCH")));
    }

    @Test
    void givenGetRolesRequest_thenOkResponse() {
        given()
                .header(newAdminToken())
                .get("/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles", hasSize(3))
                .body("_embedded.roles[0].privileges", not(empty()))
                .body("_embedded.roles[0].privileges[0].domain", not(emptyOrNullString()))
                .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                .body("_embedded.roles[0]._links.self.href", not(emptyOrNullString()))
                .body("_links.self.href", endsWith("/users-api/roles"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
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
                .body("privileges", not(empty()))
                .body("privileges[0].domain", not(emptyOrNullString()))
                .body("privileges[0].name", not(emptyOrNullString()))
                .body("_links.self.href", endsWith("/users-api/roles/1"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    void givenGetRoleRequest_whenRoleNotFound_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .get("/roles/999")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Role with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenGetRolesRequest_whenLimitedRole_thenResultsFiltered_andOkResponse() {
        given()
                .header(newUserToken())
                .get("/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles", hasSize(1))
                .body("_embedded.roles[0].id", equalTo(2))
                .body("_embedded.roles[0].name", equalTo("ROLE_USER"))
                .body("_embedded.roles[0].privileges", not(empty()))
                .body("_embedded.roles[0].privileges[0].domain", not(emptyOrNullString()))
                .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                .body("_embedded.roles[0]._links.self.href", not(emptyOrNullString()))
                .body("_links.self.href", endsWith("/users-api/roles"))
                .body("_links.role.href", endsWith("/users-api/roles/{id}"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }
}
