package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.api.user.model.UserRolePatchModel;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
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
class UserRoleControllerIT extends IntegrationTest {

    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetUserRolesRequest_whenNoToken_thenForbiddenResponse() {
            given()
                    .get("/users/" + USER_ID_ADMIN + "/roles")
                    .then().log().all().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenGetUserRolesRequest_whenNotAdmin_andNotRequestedForTheirOwnRole_thenForbiddenResponse() {
            given()
                    .header(newViewerToken())
                    .get("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            given()
                    .header(newUserToken())
                    .get("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenGetUserRolesRequest_whenNotAdminButRequestedForSelf_thenOkResponse() {
            given()
                    .header(newViewerToken())
                    .get("/users/" + USER_ID_VIEWER + "/roles")
                    .then().assertThat()
                    .statusCode(200);
            given()
                    .header(newUserToken())
                    .get("/users/" + USER_ID_USER + "/roles")
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenGetUserRolesRequest_whenSystemOrAdmin_andRequestedForAnyRole_thenOkResponse() {
            given()
                    .header(newSystemToken())
                    .get("/users/" + USER_ID_VIEWER + "/roles")
                    .then().assertThat()
                    .statusCode(200);
            given()
                    .header(newAdminToken())
                    .get("/users/" + USER_ID_USER + "/roles")
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenPatchUserRolesRequest_whenNoToken_thenForbiddenResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(2).build())
                    .build();
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenPatchUserRolesRequest_whenNoAllowedRole_thenForbiddenResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(2).build())
                    .build();
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenAllRolesOptionsRequest_thenAllowedMethodsReturned() {
            given()
                    .header(newAdminToken())
                    .options("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(200)
                    .header(HttpHeaders.ALLOW, containsString("GET"))
                    .header(HttpHeaders.ALLOW, containsString("PATCH"))
                    .header(HttpHeaders.ALLOW, not(containsString("POST")))
                    .header(HttpHeaders.ALLOW, not(containsString("PUT")));
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
                    .body("_embedded.roles[0].privileges[0].category", not(emptyOrNullString()))
                    .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                    .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/roles/1"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"));
        }

        @Test
        void givenPatchUserRolesRequest_thenAcceptedResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(1).build())
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.DELETE)
                                    .roleId(2).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_USER + "/roles")
                    .then().assertThat()
                    .statusCode(202)
                    .body("_embedded.roles", hasSize(1))
                    .body("_embedded.roles[0].id", equalTo(1))
                    .body("_embedded.roles[0].privileges", not(empty()))
                    .body("_embedded.roles[0].privileges[0].category", not(emptyOrNullString()))
                    .body("_embedded.roles[0].privileges[0].name", not(emptyOrNullString()))
                    .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/roles/1"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_USER + "/roles"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
        }

        @Test
        void givenPatchUserRolesRequest_whenUserNotFound_thenNotFoundResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(2).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/999/roles")
                    .then().assertThat()
                    .statusCode(404)
                    .body("detail", equalTo("User with id '999' not found"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenPatchUserRolesRequest_whenRoleNotFound_thenAcceptedResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(999).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_USER + "/roles")
                    .then().assertThat()
                    .statusCode(202)
                    .body("_embedded.roles", hasSize(1))
                    .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
        }

        @Test
        void givenPatchUserRolesRequest_whenTargetUserIsRoot_thenBadRequestResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(2).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", equalTo("Root user not updatable"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));;
        }

        @ParameterizedTest
        @MethodSource("invalidPatchUserRolesParameters")
        void givenPatchUserRolesRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                                     String expectedMessageSubstring) {
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .patch("/users/" + USER_ID_USER + "/roles")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", containsString(expectedMessageSubstring))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));;
        }

        @SneakyThrows
        static Stream<Arguments> invalidPatchUserRolesParameters() {
            return Stream.of(
                    Arguments.of(
                            Named.of(
                                    "Missing action",
                                    new JSONObject().put(
                                            "patches",
                                            new JSONArray().put(
                                                    new JSONObject()
                                                            .put("role_id", 1)))),
                            "patches[].action: must not be null"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Invalid action",
                                    new JSONObject().put(
                                                    "patches",
                                                    new JSONArray().put(
                                                            new JSONObject()
                                                                    .put("role_id", 1)
                                                                    .put("action", "unknown")))),
                            "No enum constant"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing role",
                                    new JSONObject().put(
                                            "patches",
                                            new JSONArray().put(
                                                    new JSONObject()
                                                            .put("action", "delete")))),
                            "patches[].roleId: must not be null"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Empty patch list",
                                    new JSONObject().put(
                                            "patches",
                                            new JSONArray())),
                            "patches: size must be between 1 and 20"
                    ),
                    Arguments.of(
                            Named.of(
                                    "No patch list",
                                    new JSONObject()),
                            "patches: must not be null"
                    )
            );
        }
    }
}
