package fi.jannetahkola.palikka.users.api.user;

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

import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.accessDeniedResponse;
import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.fullAuthenticationRequiredResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class UserControllerSecurityIT extends IntegrationTest {

    @Test
    void givenGetUserRequest_whenSystemToken_thenOkResponse() {
        given()
                .header(newSystemBearerTokenHeader())
                .get("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    void givenGetUserRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetUserRequest_whenNoAllowedRole_thenForbiddenResponse() {
        given()
                .header(newViewerToken())
                .get("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newUserToken())
                .get("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenGetUserRequest_whenLimitedRoleButRequestedForSelf_thenOkResponse() {
        given()
                .header(newViewerToken())
                .get("/users/" + USER_ID_VIEWER)
                .then().assertThat()
                .statusCode(200);
        given()
                .header(newUserToken())
                .get("/users/" + USER_ID_USER)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    void givenGetUsersRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/users")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @ParameterizedTest
    @MethodSource("usersWithRolesAllowedToGetUsers")
    void givenGetUsersRequest_whenAllowedRole_thenOkResponse(Integer user) {
        given()
                .header(user == -1 ? newSystemBearerTokenHeader() : newBearerTokenHeader(user))
                .get("/users")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    static Stream<Arguments> usersWithRolesAllowedToGetUsers() {
        return Stream.of(
                Arguments.of(Named.of("ADMIN", 1)),
                Arguments.of(Named.of("SYSTEM", -1))
        );
    }

    @ParameterizedTest
    @MethodSource("usersWithRolesNotAllowedToGetUsers")
    void givenGetUsersRequest_whenNoAllowedRole_thenForbiddenResponse(Integer user) {
        given()
                .header(newBearerTokenHeader(user))
                .get("/users")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    static Stream<Arguments> usersWithRolesNotAllowedToGetUsers() {
        return Stream.of(
                Arguments.of(Named.of("USER", 2)),
                Arguments.of(Named.of("VIEWER", 3))
        );
    }

    @SneakyThrows
    @Test
    void givenGetPostUserRequest_whenNoToken_thenForbiddenResponse() {
        String json = new JSONObject()
                .put("username", "viewer")
                .put("password", "mock-pass")
                .put("active", true)
                .toString();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/users")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @SneakyThrows
    @Test
    void givenGetPostUserRequest_whenNoAllowedRole_thenForbiddenResponse() {
        String json = new JSONObject()
                .put("username", "viewer")
                .put("password", "mock-pass")
                .put("active", true)
                .toString();
        given()
                .header(newViewerToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/users")
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newUserToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/users")
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newSystemBearerTokenHeader())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/users")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @SneakyThrows
    @Test
    void givenPutUserRequest_whenNoToken_thenForbiddenResponse() {
        String json = new JSONObject()
                .put("username", "mock-user-updated")
                .put("password", "new-pass")
                .put("active", false)
                .toString();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .put("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @SneakyThrows
    @Test
    void givenPutUserRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
        String json = new JSONObject()
                .put("username", "mock-user-updated")
                .put("password", "new-pass")
                .put("active", false)
                .toString();
        given()
                .header(newViewerToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .put("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newUserToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .put("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newSystemBearerTokenHeader())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .put("/users/" + USER_ID_ADMIN)
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("nonAdminUsers") // No root user since they can't be updated
    void givenPutUserRequest_whenNoAllowedRoleButRequestedForSelf_thenAcceptedResponse(Integer user) {
        String json = new JSONObject()
                .put("username", "mock-user-updated-" + user)
                .put("active", false) // Cannot update unless admin
                .toString();
        Header authHeader = newBearerTokenHeader(user);
        given()
                .header(authHeader)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .put("/users/" + user)
                .then().assertThat()
                .statusCode(202)
                .body("username", equalTo("mock-user-updated-" + user))
                .body("active", equalTo(true));
    }

    static Stream<Arguments> nonAdminUsers() {
        return Stream.of(
                Arguments.of(Named.of("USER", 2)),
                Arguments.of(Named.of("VIEWER", 3))
        );
    }
}
