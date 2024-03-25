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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;

@SqlForUsers
class UserRoleControllerIT extends IntegrationTest {

    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetUserRolesRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
            given()
                    .get("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newViewerToken())
                    .get("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .get("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetUserRolesRequest_whenNoAllowedRoleButRequestedForSelf_thenOkResponse() {
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
        void givenPatchUserRolesRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(2).build())
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.DELETE)
                                    .roleId(1).build())
                    .build();
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
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
                    .body("_embedded.roles[0]._links.self.href", endsWith("/users-api/roles/1"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_USER + "/roles"));
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
                    .body("message", equalTo("User with id '999' not found"));
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
                    .body("_embedded.roles", hasSize(1));
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
                    .body("message", equalTo("Root user not updatable"));
        }

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("invalidPatchUserRolesParameters")
        void givenPatchUserRolesRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                                     String expectedMessageSubstring) {
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .patch("/users/" + USER_ID_USER + "/roles")
                    .then().log().all().assertThat()
                    .statusCode(400)
                    .body("message", equalTo(expectedMessageSubstring));
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
                            "Invalid request"
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
                            "patches: size must be between 1 and 2147483647"
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
