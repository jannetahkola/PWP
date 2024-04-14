package fi.jannetahkola.palikka.users.api.user;

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

class CurrentUserControllerSecurityIT extends IntegrationTest {
    @Test
    void givenGetCurrentUserRequest_whenNoToken_thenForbiddenResponse() {
        given()
                .get("/current-user")
                .then().assertThat()
                .spec(fullAuthenticationRequiredResponse());
    }

    @Test
    void givenGetCurrentUserRequest_whenSystemToken_thenForbiddenResponse() {
        given()
                .header(newSystemBearerTokenHeader())
                .get("/current-user")
                .then().assertThat()
                .spec(accessDeniedResponse());
    }

    @ParameterizedTest
    @MethodSource("userParams")
    void givenGetCurrentUserRequest_whenAnyRole_thenOkResponse(Integer user) {
        given()
                .header(newBearerTokenHeader(user))
                .get("/current-user")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
    }

    static Stream<Arguments> userParams() {
        return Stream.of(
                Arguments.of(Named.of("ADMIN", 1)),
                Arguments.of(Named.of("USER", 2)),
                Arguments.of(Named.of("VIEWER", 3))
        );
    }
}
