package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.core.auth.data.RevokedTokenEntity;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.http.Header;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class AuthenticationControllerIT extends IntegrationTest {

    @Autowired
    RevokedTokenRepository revokedTokenRepository;

    @SneakyThrows
    @Test
    void givenLoginRequest_whenCredentialsValid_thenOkResponse() {
        String json = new JSONObject()
                .put("username", "admin")
                .put("password", "password")
                .toString();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/auth/login")
                .then().assertThat()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("expires_at", endsWith("Z"))
                .body("_links.self.href", endsWith("/users-api/auth/login"))
                .body("_links.logout.href", endsWith("/users-api/auth/logout"))
                .body("_links.current_user.href", endsWith("/users-api/current-user"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
    }

    @SneakyThrows
    @Test
    void givenLoginRequest_whenCredentialsInvalid_thenBadRequestResponse() {
        String json = new JSONObject()
                .put("username", "wrong-user")
                .put("password", "password")
                .toString();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .post("/auth/login")
                .then().assertThat()
                .statusCode(400)
                .body("detail", equalTo("Login failed"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @ParameterizedTest
    @MethodSource("invalidLoginParameters")
    void givenLoginRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                        String expectedDetailMessageSubstring) {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(json.toString())
                .post("/auth/login")
                .then().assertThat()
                .statusCode(400)
                .body("detail", containsString(expectedDetailMessageSubstring))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenLogoutRequest_thenTokenRevoked_andOkResponse(@Autowired JwtProperties jwtProperties) {
        Header authHeader = newUserToken();
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(authHeader)
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE);
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(authHeader)
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(403)
                .body("detail", equalTo("Full authentication is required to access this resource"));
        Iterable<RevokedTokenEntity> revokedTokens = revokedTokenRepository.findAll();
        assertThat(revokedTokens).hasSize(1);
        RevokedTokenEntity revokedToken = revokedTokens.iterator().next();
        assertThat(revokedToken.getTtlSeconds())
                .isEqualTo(jwtProperties.getToken().getUser().getSigning().getValidityTime().getSeconds());
    }

    @Test
    void givenLogoutRequest_withoutToken_thenForbiddenResponse() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(403)
                .body("detail", equalTo("Full authentication is required to access this resource"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void givenLogoutRequest_withSystemToken_thenForbiddenResponse() {
        given()
                .header(newSystemBearerTokenHeader())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(403)
                .body("detail", equalTo("Access Denied"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @SneakyThrows
    private static Stream<Arguments> invalidLoginParameters() {
        return Stream.of(
                Arguments.of(
                        Named.of(
                                "Missing username",
                                new JSONObject()
                                        .put("password", "password")
                                        .put("active", true)),
                        "username: must not be blank"
                ),
                Arguments.of(
                        Named.of(
                                "Blank username",
                                new JSONObject()
                                        .put("username", "")
                                        .put("password", "password")
                                        .put("active", true)),
                        "username: must not be blank"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid username - too short",
                                new JSONObject()
                                        .put("username", "us")
                                        .put("password", "password")
                                        .put("active", true)),
                        "username: must match"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid username - too long",
                                new JSONObject()
                                        .put("username", "usernameusernameusername")
                                        .put("password", "password")
                                        .put("active", true)),
                        "username: must match"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid username - contains invalid characters",
                                new JSONObject()
                                        .put("username", "user$")
                                        .put("password", "password")
                                        .put("active", true)),
                        "username: must match"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid username - contains spaces",
                                new JSONObject()
                                        .put("username", "user ")
                                        .put("password", "password")
                                        .put("active", true)),
                        "username: must match"
                ),
                Arguments.of(
                        Named.of(
                                "Missing password",
                                new JSONObject()
                                        .put("username", "username")
                                        .put("active", true)),
                        "password: must not be null"
                ),
                Arguments.of(
                        Named.of(
                                "Blank password",
                                new JSONObject()
                                        .put("username", "username")
                                        .put("password", "")
                                        .put("active", true)),
                        "password: must be between"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid password - too short",
                                new JSONObject()
                                        .put("username", "username")
                                        .put("password", "pass")
                                        .put("active", true)),
                        "password: must be between"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid password - too long",
                                new JSONObject()
                                        .put("username", "username")
                                        .put("password", "passwordpasswordpasswordpassword")
                                        .put("active", true)),
                        "password: must be between"
                ),
                Arguments.of(
                        Named.of(
                                "Invalid password - contains spaces",
                                new JSONObject()
                                        .put("username", "username")
                                        .put("password", "password ")
                                        .put("active", true)),
                        "password: must not contain whitespaces"
                )
        );
    }
}
