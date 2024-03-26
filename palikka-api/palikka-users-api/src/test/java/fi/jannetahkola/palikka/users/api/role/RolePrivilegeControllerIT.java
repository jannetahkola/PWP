package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.users.api.role.model.RolePrivilegePatchModel;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeRepository;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Set;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.hateoas.client.Hop.rel;

@SqlForUsers
class RolePrivilegeControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetRolePrivilegesRequest_whenNoToken_thenForbiddenResponse() {
            given()
                    .get("/roles/1/privileges")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"));
        }

        @Test
        void givenGetRolePrivilegesRequest_whenAdminOrSystem_andRequestedForAnyRole_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/roles/2/privileges")
                    .then().assertThat()
                    .statusCode(200);
            given()
                    .header(newSystemToken())
                    .get("/roles/3/privileges")
                    .then().assertThat()
                    .statusCode(200);
        }

        @ParameterizedTest
        @MethodSource("usersAndOwnRolesWithLimitedAccessToGetRolePrivileges")
        void givenGetRolePrivilegesRequest_whenLimitedRole_andRequestedForOwnRole_thenOkResponse(Integer user,
                                                                                                 Integer ownRoleId) {
            given()
                    .header(newToken(user))
                    .get("/roles/" + ownRoleId + "/privileges")
                    .then().assertThat()
                    .statusCode(200);
        }

        static Stream<Arguments> usersAndOwnRolesWithLimitedAccessToGetRolePrivileges() {
            return Stream.of(
                    Arguments.of(Named.of("USER", 2), 2),
                    Arguments.of(Named.of("VIEWER", 3), 3)
            );
        }

        @ParameterizedTest
        @MethodSource("usersAndNotOwnRolesWithLimitedAccessToGetRolePrivileges")
        void givenGetRolePrivilegesRequest_whenLimitedRole_andNotRequestedForOwnRole_thenForbiddenResponse(Integer user,
                                                                                                           Integer notOwnRoleId) {
            given()
                    .header(newToken(user))
                    .get("/roles/" + notOwnRoleId + "/privileges")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"));
        }

        static Stream<Arguments> usersAndNotOwnRolesWithLimitedAccessToGetRolePrivileges() {
            return Stream.of(
                    Arguments.of(Named.of("USER", 2), 1),
                    Arguments.of(Named.of("VIEWER", 3), 2)
            );
        }

        @Test
        void givenPatchRolePrivilegesRequest_whenNoToken_thenForbiddenResponse() {
            RolePrivilegePatchModel patch = RolePrivilegePatchModel.builder()
                    .patch(
                            RolePrivilegePatchModel.RolePrivilegePatch.builder()
                                    .action(RolePrivilegePatchModel.Action.DELETE)
                                    .privilegeId(1).build())
                    .build();
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/roles/1/privileges")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @ParameterizedTest
        @MethodSource("usersWithNoAllowedRolesToPatchUserPrivileges")
        void givenPatchRolePrivilegesRequest_whenNoAllowedRole_thenForbiddenResponse(Integer user) {
            RolePrivilegePatchModel patch = RolePrivilegePatchModel.builder()
                    .patch(
                            RolePrivilegePatchModel.RolePrivilegePatch.builder()
                                    .action(RolePrivilegePatchModel.Action.DELETE)
                                    .privilegeId(1).build())
                    .build();
            given()
                    .header(newToken(user))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/roles/1/privileges")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        static Stream<Arguments> usersWithNoAllowedRolesToPatchUserPrivileges() {
            return Stream.of(
                    Arguments.of(Named.of("USER", 2)),
                    Arguments.of(Named.of("VIEWER", 3))
            );
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenGetRolePrivilegesOptionsRequest_thenAllowedMethodsReturned() {
            given()
                    .header(newAdminToken())
                    .options("/roles/2/privileges")
                    .then().assertThat()
                    .statusCode(200)
                    .header(HttpHeaders.ALLOW, containsString("PATCH"))
                    .header(HttpHeaders.ALLOW, not(containsString("POST")))
                    .header(HttpHeaders.ALLOW, not(containsString("PUT")));
        }

        @Test
        void givenPatchRolePrivilegesRequest_thenAcceptedResponse(@Autowired PrivilegeRepository privilegeRepository) {
            Integer existingPrivilegeId = privilegeRepository.findByName("weather").orElseThrow().getId();
            Integer newPrivilegeId = privilegeRepository.findByName("op").orElseThrow().getId();
            RolePrivilegePatchModel patch = RolePrivilegePatchModel.builder()
                    .patch(
                            RolePrivilegePatchModel.RolePrivilegePatch.builder()
                                    .action(RolePrivilegePatchModel.Action.ADD)
                                    .privilegeId(newPrivilegeId).build())
                    .patch(
                            RolePrivilegePatchModel.RolePrivilegePatch.builder()
                                    .action(RolePrivilegePatchModel.Action.DELETE)
                                    .privilegeId(existingPrivilegeId).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/roles/2/privileges")
                    .then().assertThat()
                    .statusCode(202)
                    .body("_embedded.privileges", hasSize(4))
                    .body("_links.self.href", endsWith("/roles/2/privileges"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));

            URI baseUri = URI.create(RestAssured.baseURI + ":" + RestAssured.port).resolve("/users-api/roles");
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setBearerAuth(tokens.generateToken(USER_ID_ADMIN));

            Traverson traverson = new Traverson(baseUri, MediaTypes.HAL_JSON);
            EntityModel<Role> roleCollectionModel = traverson
                    .follow(rel("role").withParameter("id", 2))
                    .withHeaders(httpHeaders)
                    .toObject(new TypeReferences.EntityModelType<>() {});
            assertThat(roleCollectionModel).isNotNull();
            assertThat(roleCollectionModel.getContent()).isNotNull();
            Set<Privilege> privileges = roleCollectionModel.getContent().getPrivileges().orElseThrow();
            assertThat(privileges.stream().anyMatch(privilege -> privilege.getId().equals(newPrivilegeId))).isTrue();
            assertThat(privileges.stream().noneMatch(privilege -> privilege.getId().equals(existingPrivilegeId))).isTrue();
        }

        @Test
        void givenPatchRolePrivilegesRequest_whenRoleNotFound_thenNotFoundResponse() {
            RolePrivilegePatchModel patch = RolePrivilegePatchModel.builder()
                    .patch(
                            RolePrivilegePatchModel.RolePrivilegePatch.builder()
                                    .action(RolePrivilegePatchModel.Action.DELETE)
                                    .privilegeId(1).build())
                    .build();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(patch)
                    .patch("/roles/999/privileges")
                    .then().assertThat()
                    .statusCode(404)
                    .body("detail", equalTo("Role with id '999' not found"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @ParameterizedTest
        @MethodSource("invalidPatchRolePrivilegesParameters")
        void givenPatchRolePrivilegesRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                                          String expectedMessageSubstring) {
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .patch("/roles/2/privileges")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", containsString(expectedMessageSubstring))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));;
        }

        @SneakyThrows
        static Stream<Arguments> invalidPatchRolePrivilegesParameters() {
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
                            "patches[].privilegeId: must not be null"
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
