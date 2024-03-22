package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.api.user.model.UserRolePatchModel;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SqlForUsers
class UserRoleControllerIT extends IntegrationTest {

    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetUserRolesRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            given()
                    .get("/users-api/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .get("/users-api/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetUserRolesRequest_whenNoRoleButRequestedForSelf_thenOkResponse() {
            given()
                    .header(newUserToken())
                    .get("/users-api/users/" + USER_ID_USER + "/roles")
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenPatchUserRolesRequest_whenNoTokenOrRole_thenForbiddenResponse() {
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
                    .patch("/users-api/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users-api/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(403);
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenGetUserRolesRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users-api/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.roles", hasSize(1))
                    .body("_links.self.href", endsWith("/users-api/users/1/roles"));
        }

        @Test
        void givenPatchUserRolesRequest_thenAcceptedResponse() {
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
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users-api/users/" + USER_ID_ADMIN + "/roles")
                    .then().assertThat()
                    .statusCode(202)
                    .body("_embedded.roles", hasSize(1))
                    .body("_embedded.roles[0].id", equalTo(2));
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
                    .patch("/users-api/users/3/roles")
                    .then().assertThat()
                    .statusCode(404)
                    .body("message", equalTo("User with id '3' not found"));
        }

        @Test
        void givenPatchUserRolesRequest_whenRoleNotFound_thenAcceptedResponse() {
            UserRolePatchModel patch = UserRolePatchModel.builder()
                    .patch(
                            UserRolePatchModel.UserRolePatch.builder()
                                    .action(UserRolePatchModel.Action.ADD)
                                    .roleId(3).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/users-api/users/" + USER_ID_ADMIN + "/roles")
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
                    .patch("/users-api/users/" + USER_ID_ROOT + "/roles")
                    .then().assertThat()
                    .statusCode(400)
                    .body("message", equalTo("Root user not updatable"));
        }

        @Test
        void givenPatchUserRolesRequest_whenParametersInvalid_thenBadRequestResponse() {
            // TODO Parameterized test
        }
    }
}
