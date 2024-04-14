package fi.jannetahkola.palikka.users.api.role;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;

import java.util.stream.Stream;

import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.accessDeniedResponse;
import static fi.jannetahkola.palikka.users.testutils.ResponseSpecs.fullAuthenticationRequiredResponse;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class RoleControllerSecurityIT extends IntegrationTest {
    @Test
    void givenGetRoleRequest_whenNoToken_thenForbiddenRequest() {
        given()
                .get("/roles/1")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetRoleRequest_whenNoAllowedRole_thenForbiddenRequest() {
        given()
                .header(newViewerToken())
                .get("/roles/1")
                .then().assertThat()
                .spec(accessDeniedResponse());
        given()
                .header(newUserToken())
                .get("/roles/1")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @Test
    void givenGetRoleRequest_whenLimitedRole_andRequestedForThatRole_thenOkResponse() {
        given()
                .header(newUserToken())
                .get("/roles/2")
                .then().assertThat()
                .body("id", equalTo(2))
                .statusCode(200);
    }

    @Test
    void givenGetRolesRequest_whenNoToken_thenForbiddenRequest() {
        given()
                .get("/roles")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @ParameterizedTest
    @MethodSource("usersWithRolesAllowedToGetRoles")
    void givenGetRolesRequest_whenAllowedRole_andOkResponse(Integer user, Integer roleId) {
        given()
                .header(newBearerTokenHeader(user))
                .get("/roles")
                .then().assertThat()
                .statusCode(200)
                .body("_embedded.roles[0].id", equalTo(roleId))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    static Stream<Arguments> usersWithRolesAllowedToGetRoles() {
        return Stream.of(
                Arguments.of(Named.of("ADMIN", USER_ID_ADMIN), 1),
                Arguments.of(Named.of("USER", USER_ID_USER), 2),
                Arguments.of(Named.of("VIEWER", USER_ID_VIEWER), 3)
        );
    }
}
