package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.api.user.model.UserRolePostModel;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.http.Header;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Named;
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

class UserRoleControllerIT extends IntegrationTest {
    @Test
    void givenGetUserRolesOptionsRequest_thenAllowedMethodsReturned() {
        given()
                .header(newAdminToken())
                .options("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.ALLOW, containsString("GET"))
                .header(HttpHeaders.ALLOW, containsString("POST"))
                .header(HttpHeaders.ALLOW, not(containsString("PUT")))
                .header(HttpHeaders.ALLOW, not(containsString("PATCH")));
    }

    @Test
    void givenGetUserRolesRequest_whenAcceptHalFormsHeaderGiven_thenResponseContainsTemplate() {
        given()
                .header(newAdminToken())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .get("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/users/1/roles/1"))
                .body("_embedded.roles[0]._links.privileges.href", endsWith("/users-api/roles/1/privileges"))
                .body("_embedded.roles[0]._templates.default.method", equalTo("DELETE"))
                .body("_links.self.href", endsWith("/users-api/users/1/roles"))
                .body("_templates.default.method", equalTo("POST"))
                .body("_templates.default.properties[0].name", equalTo("role_id"))
                .body("_templates.default.properties[0].required", equalTo(true))
                .body("_templates.default.properties[0].type", equalTo("number"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    void givenGetUserRolesRequest_thenOkResponse() {
        given()
                .header(newAdminToken())
                .get("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles", hasSize(1))
                .body("_embedded.roles[0].id", equalTo(1))
                .body("_embedded.roles[0].name", equalTo("ROLE_ADMIN"))
                .body("_embedded.roles[0].privileges", not(empty()))
                .body("_embedded.roles[0].privileges[0].domain", not(emptyOrNullString()))
                .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/users/1/roles/1"))
                .body("_embedded.roles[0]._links.privileges.href", endsWith("/users-api/roles/1/privileges"))
                .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }


    @Test
    void givenGetSingleUserRoleRequest_whenAcceptHalFormsHeaderGiven_thenResponseContainsTemplate() {
        given()
                .header(newAdminToken())
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                .get("/users/" + USER_ID_ADMIN + "/roles/1")
                .then().assertThat()
                .statusCode(200)
                .body("_links.self.href", endsWith("/users-api/users/" + USER_ID_ADMIN + "/roles/1"))
                .body("_links.privileges.href", endsWith("/users-api/roles/1/privileges"))
                .body("_templates.default.method", equalTo("DELETE"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    @Test
    void givenGetSingleUserRoleRequest_thenOkResponse() {
        given()
                .header(newAdminToken())
                .get("/users/" + USER_ID_ADMIN + "/roles/1")
                .then().assertThat()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("_links.self.href", endsWith("/users-api/users/" + USER_ID_ADMIN + "/roles/1"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    void givenGetSingleUserRoleRequest_whenUserNotFound_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .get("/users/999/roles/1")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("User with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenGetSingleUserRoleRequest_whenRoleNotAssociatedWithUser_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .get("/users/" + USER_ID_USER + "/roles/1")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Role with id '1' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenPostUserRolesRequest_thenAssociationCreated_andCreatedResponse() {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(1).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .statusCode(201)
                .body("_embedded.roles", hasSize(2))
                .body("_embedded.roles[0].id", equalTo(1))
                .body("_embedded.roles[0].privileges", not(empty()))
                .body("_embedded.roles[0].privileges[0].domain", not(emptyOrNullString()))
                .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/users/" + USER_ID_USER + "/roles/1"))
                .body("_embedded.roles[1].id", equalTo(2))
                .body("_embedded.roles[1].privileges", not(empty()))
                .body("_embedded.roles[1].privileges[0].domain", not(emptyOrNullString()))
                .body("_embedded.roles[1].privileges[0].name", not(emptyOrNullString()))
                .body("_embedded.roles[1]._links.self.href", endsWith("/users-api/users/" + USER_ID_USER + "/roles/2"))
                .body("_links.self.href", endsWith("/users/" + USER_ID_USER + "/roles"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    void givenPostUserRolesRequest_whenRoleAssociationAlreadyExists_thenCreatedResponse() {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(2).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .statusCode(201)
                .body("_embedded.roles", hasSize(1))
                .body("_embedded.roles[0].id", equalTo(2))
                .body("_embedded.roles[0].privileges", not(empty()))
                .body("_embedded.roles[0].privileges[0].domain", not(emptyOrNullString()))
                .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/users/" + USER_ID_USER + "/roles/2"))
                .body("_links.self.href", endsWith("/users/" + USER_ID_USER + "/roles"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @Test
    void givenPostUserRolesRequest_whenUserNotFound_thenNotFoundResponse() {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(1).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/999/roles")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("User with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenPostUserRolesRequest_whenRoleNotFound_thenNotFoundResponse() {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(999).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("Role with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenPostUserRolesRequest_whenTargetUserIsRoot_thenBadRequestResponse() {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(2).build();
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .statusCode(400)
                .body("detail", equalTo("Root user not updatable"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));;
    }

    @ParameterizedTest
    @MethodSource("invalidPostUserRolesParameters")
    void givenPostUserRolesRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                                String expectedMessageSubstring) {
        given()
                .header(newAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json.toString())
                .post("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .statusCode(400)
                .body("detail", containsString(expectedMessageSubstring))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenDeleteUserRolesRequest_thenAssociationDeleted_andNoContentResponse() {
        Header authHeader = newAdminToken();
        given()
                .header(authHeader)
                .delete("/users/" + USER_ID_USER + "/roles/2")
                .then().assertThat()
                .statusCode(204);
        given()
                .header(authHeader)
                .get("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles", is(nullValue()));
    }

    @Test
    void givenDeleteUserRolesRequest_whenRoleNotAssociatedWithUser_thenNoContentResponse() {
        Header authHeader = newAdminToken();
        given()
                .header(authHeader)
                .delete("/users/" + USER_ID_USER + "/roles/1")
                .then().assertThat()
                .statusCode(204);
        given()
                .header(authHeader)
                .get("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles", hasSize(1));
    }

    @Test
    void givenDeleteUserRolesRequest_whenUserNotFound_thenNotFoundResponse() {
        given()
                .header(newAdminToken())
                .delete("/users/999/roles/2")
                .then().assertThat()
                .statusCode(404)
                .body("detail", equalTo("User with id '999' not found"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenDeleteUserRolesRequest_whenTargetUserIsRoot_thenBadRequestResponse() {
        given()
                .header(newAdminToken())
                .delete("/users/" + USER_ID_ADMIN + "/roles/1")
                .then().assertThat()
                .statusCode(400)
                .body("detail", equalTo("Root user not updatable"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @SneakyThrows
    static Stream<Arguments> invalidPostUserRolesParameters() {
        return Stream.of(
                Arguments.of(
                        Named.of(
                                "Empty role id",
                                new JSONObject().put(
                                        "role_id",
                                        "")),
                        "roleId: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "Blank role id",
                                new JSONObject().put(
                                        "role_id",
                                        " ")),
                        "roleId: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "No role id",
                                new JSONObject()),
                        "roleId: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid role id",
                                new JSONObject().put("role_id", "a")),
                        "Cannot deserialize value of type"
                )
        );
    }
}
