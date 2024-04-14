package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.users.api.role.model.RolePrivilegePostModel;
import fi.jannetahkola.palikka.users.data.privilege.PrivilegeRepository;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.http.Header;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.accessDeniedResponse;
import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.fullAuthenticationRequiredResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class RolePrivilegeControllerSecurityIT extends IntegrationTest {
    @Test
    void givenGetRolePrivilegesRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/roles/1/privileges")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetRolePrivilegesRequest_whenAdminOrSystem_andRequestedForAnyRole_thenOkResponse() {
        given()
                .header(newAdminToken())
                .get("/roles/2/privileges")
                .then().assertThat()
                .statusCode(200);
        given()
                .header(newSystemBearerTokenHeader())
                .get("/roles/3/privileges")
                .then().assertThat()
                .statusCode(200);
    }

    @ParameterizedTest
    @MethodSource("usersAndOwnRolesWithLimitedAccessToGetRolePrivileges")
    void givenGetRolePrivilegesRequest_whenLimitedRole_andRequestedForOwnRole_thenOkResponse(Integer user,
                                                                                             Integer ownRoleId) {
        given()
                .header(newBearerTokenHeader(user))
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
                .header(newBearerTokenHeader(user))
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
    void givenGetRolePrivilegeRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/roles/1/privileges/1")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetRolePrivilegeRequest_whenLimitedRole_andRequestedForOwnRole_thenOkResponse(@Autowired PrivilegeRepository privilegeRepository) {
        final int roleId = 2;
        Integer privilegeId = privilegeRepository.findAllByRoleId(roleId).stream()
                .findAny().orElseThrow().getId();
        given()
                .header(newUserToken())
                .get("/roles/2/privileges/" + privilegeId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    void givenGetRolePrivilegeRequest_whenLimitedRole_andNotRequestedForOwnRole_thenForbiddenResponse() {
        given()
                .header(newUserToken())
                .get("/roles/1/privileges/1")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenGetRolePrivilegeRequest_whenSystemOrAdmin_andRequestedForAnyRole_thenOkResponse(@Autowired PrivilegeRepository privilegeRepository) {
        final int notOwnRoleId = 2;
        Integer privilegeId = privilegeRepository.findAllByRoleId(notOwnRoleId).stream()
                .findAny().orElseThrow().getId();
        given()
                .header(newSystemBearerTokenHeader())
                .get("/roles/" + notOwnRoleId + "/privileges/" + privilegeId)
                .then().assertThat()
                .statusCode(200);
        given()
                .header(newAdminToken())
                .get("/roles/" + notOwnRoleId + "/privileges/" + privilegeId)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    void givenPostRolePrivilegesRequest_whenNoToken_thenForbiddenResponse() {
        RolePrivilegePostModel postModel = RolePrivilegePostModel.builder().privilegeId(1).build();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/roles/1/privileges")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @ParameterizedTest
    @MethodSource("usersNotAllowedToPost")
    void givenPostRolePrivilegesRequest_whenNoAllowedRole_thenForbiddenResponse(Integer user) {
        RolePrivilegePostModel postModel = RolePrivilegePostModel.builder().privilegeId(1).build();
        Header authHeader = user == -1 ? newSystemBearerTokenHeader() : newBearerTokenHeader(user);
        given()
                .header(authHeader)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/roles/1/privileges")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenDeleteRolePrivilegesRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .delete("/roles/1/privileges/1")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @ParameterizedTest
    @MethodSource("usersNotAllowedToDelete")
    void givenDeleteRolePrivilegesRequest_whenNoAllowedRole_thenForbiddenResponse(Integer user) {
        Header authHeader = user == -1 ? newSystemBearerTokenHeader() : newBearerTokenHeader(user);
        given()
                .header(authHeader)
                .delete("/roles/1/privileges/1")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    static Stream<Arguments> usersNotAllowedToPost() {
        return Stream.of(
                Arguments.of(Named.of("SYSTEM", -1)),
                Arguments.of(Named.of("USER", USER_ID_USER)),
                Arguments.of(Named.of("VIEWER", USER_ID_VIEWER))
        );
    }

    static Stream<Arguments> usersNotAllowedToDelete() {
        return Stream.of(
                Arguments.of(Named.of("SYSTEM", -1)),
                Arguments.of(Named.of("USER", USER_ID_USER)),
                Arguments.of(Named.of("VIEWER", USER_ID_VIEWER))
        );
    }
}
