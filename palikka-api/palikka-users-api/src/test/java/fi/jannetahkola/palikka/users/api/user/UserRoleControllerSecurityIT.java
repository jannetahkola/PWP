package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.api.user.model.UserRolePostModel;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.http.Header;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.accessDeniedResponse;
import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.fullAuthenticationRequiredResponse;
import static io.restassured.RestAssured.given;

class UserRoleControllerSecurityIT extends IntegrationTest {
    @Test
    void givenGetUserRolesRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetUserRolesRequest_whenLimitedRole_andNotRequestedForSelf_thenForbiddenResponse() {
        given()
                .header(newViewerToken())
                .get("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newUserToken())
                .get("/users/" + USER_ID_ADMIN + "/roles")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenGetUserRolesRequest_whenLimitedRole_andRequestedForSelf_thenOkResponse() {
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
    void givenGetUserRolesRequest_whenSystemOrAdmin_andRequestedForAnyUser_thenOkResponse() {
        given()
                .header(newSystemBearerTokenHeader())
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
    void givenGetUserRoleRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/users/" + USER_ID_ADMIN + "/roles/1")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetUserRoleRequest_whenLimitedRole_andNotRequestedForSelf_thenForbiddenResponse() {
        given()
                .header(newViewerToken())
                .get("/users/" + USER_ID_ADMIN + "/roles/1")
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newUserToken())
                .get("/users/" + USER_ID_ADMIN + "/roles/1")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenGetUserRoleRequest_whenLimitedRole_andRequestedForSelf_thenOkResponse() {
        given()
                .header(newViewerToken())
                .get("/users/" + USER_ID_VIEWER + "/roles/3")
                .then().assertThat()
                .statusCode(200);
        given()
                .header(newUserToken())
                .get("/users/" + USER_ID_USER + "/roles/2")
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    void givenPostUserRolesRequest_whenNoToken_thenForbiddenResponse() {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(1).build();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @ParameterizedTest
    @MethodSource("usersNotAllowedToPost")
    void givenPostUserRolesRequest_whenNoAllowedRole_thenForbiddenResponse(Integer user) {
        UserRolePostModel postModel = UserRolePostModel.builder().roleId(1).build();
        Header authHeader = user == -1 ? newSystemBearerTokenHeader() : newBearerTokenHeader(user);
        given()
                .header(authHeader)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(postModel)
                .post("/users/" + USER_ID_USER + "/roles")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenDeleteUserRolesRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .delete("/users/" + USER_ID_USER + "/roles/2")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @ParameterizedTest
    @MethodSource("usersNotAllowedToDelete")
    void givenDeleteUserRolesRequest_whenNoAllowedRole_thenForbiddenResponse(Integer user) {
        Header authHeader = user == -1 ? newSystemBearerTokenHeader() : newBearerTokenHeader(user);
        given()
                .header(authHeader)
                .delete("/users/" + USER_ID_USER + "/roles/2")
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
