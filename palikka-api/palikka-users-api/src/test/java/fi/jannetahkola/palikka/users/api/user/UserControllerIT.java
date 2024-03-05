package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SqlForUsers
class UserControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetUserRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            given()
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetUserRequest_whenNoRoleButRequestedForSelf_thenOkResponse() {
            given()
                    .header(newUserToken())
                    .get("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenGetUsersRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            given()
                    .get("/users")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .get("/users")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenGetPostUserRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("password", "mock-pass")
                    .put("active", true)
                    .toString();
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("password", "new-pass")
                    .put("active", false)
                    .toString();
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenNoRoleButRequestedForSelf_thenAcceptedResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("active", false) // Cannot update unless admin
                    .toString();
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(202)
                    .body("username", equalTo("mock-user-3"))
                    .body("active", equalTo(true));
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenGetUserRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(200)
                    .body("id", equalTo(1))
                    .body("username", equalTo("mock-user"))
                    .body("password", nullValue())
                    .body("active", equalTo(true))
                    .body("roles", hasSize(1))
                    .body("roles", contains("ROLE_ADMIN"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN))
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"));
        }

        @Test
        void givenGetUserRequest_whenUserNotFound_thenNotFoundResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users/9999")
                    .then().assertThat()
                    .statusCode(404)
                    .body("message", equalTo("User with id '9999' not found"));
        }

        @Test
        void givenGetUsersRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.users", hasSize(2))
                    .body("_links.self.href", endsWith("/users"));
        }

        @SneakyThrows
        @Test
        void givenPostUserRequest_thenOkResponse(@Autowired UserRepository userRepository) {
            // Using JSONObject because password won't be included during serialization in the model
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("password", "mock-pass")
                    .put("active", true)
                    .toString();

            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(201)
                    .header(HttpHeaders.LOCATION, endsWith("/users/3"))
                    .body("id", equalTo(3))
                    .body("username", equalTo("mock-user-3"))
                    .body("password", nullValue())
                    .body("salt", nullValue())
                    .body("active", equalTo(true))
                    .body("_links.self.href", endsWith("/users/3"));

            UserEntity createdUser = userRepository.findById(3).orElseThrow();
            String salt = createdUser.getSalt();
            String expectedPassword = createdUser.getPassword();
            assertThat(CryptoUtils.validatePassword("mock-pass", salt, expectedPassword)).isTrue();
        }

        @ParameterizedTest
        @MethodSource("invalidPostUserParameters")
        void givenPostUserRequest_whenParametersInvalid_thenBadRequestResponse(JSONObject json,
                                                                               String expectedMessageSubstring) {
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/users")
                    .then().assertThat()
                    .statusCode(400)
                    .body("message", containsString(expectedMessageSubstring));
        }

        @SneakyThrows
        @Test
        void givenPostUserRequest_whenUsernameTaken_thenConflictResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user")
                    .put("password", "mock-pass")
                    .put("active", true)
                    .toString();

            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(409)
                    .body("message", equalTo("User with username 'mock-user' already exists"));
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_thenAcceptedResponse(@Autowired UserRepository userRepository) {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("password", "new-pass")
                    .put("active", false) // Admin can update
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(202)
                    .body("username", equalTo("mock-user-3"))
                    .body("password", nullValue())
                    .body("salt", nullValue())
                    .body("active", equalTo(false));

            UserEntity createdUser = userRepository.findById(USER_ID_ADMIN).orElseThrow();
            String salt = createdUser.getSalt();
            String expectedPassword = createdUser.getPassword();
            assertThat(CryptoUtils.validatePassword("new-pass", salt, expectedPassword)).isTrue();
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenUserNotFound_thenNotFoundResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("active", false)
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/9999")
                    .then().assertThat()
                    .statusCode(404)
                    .body("message", equalTo("User with id '9999' not found"));
        }

        @Test
        void givenPutUserRequest_whenParametersInvalid_thenBadRequestResponse() {
            // TODO
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenUsernameTaken_thenConflictResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-2")
                    .put("active", false)
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(409)
                    .body("message", equalTo("User with username 'mock-user-2' already exists"));
        }

        @SneakyThrows
        private static Stream<Arguments> invalidPostUserParameters() {
            return Stream.of(
                    Arguments.of(
                            Named.of(
                                    "Missing username",
                                    new JSONObject()
                                            .put("password", "pass")
                                            .put("active", true)),
                            "username: must not be blank"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Blank username",
                                    new JSONObject()
                                            .put("username", "")
                                            .put("password", "pass")
                                            .put("active", true)),
                            "username: must not be blank"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing password",
                                    new JSONObject()
                                            .put("username", "user")
                                            .put("active", true)),
                            "password: must not be blank"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Blank password",
                                    new JSONObject()
                                            .put("username", "user")
                                            .put("password", "")
                                            .put("active", true)),
                            "password: must not be blank"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing active",
                                    new JSONObject()
                                            .put("username", "user")
                                            .put("password", "pass")),
                            "active: must not be null"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing active",
                                    new JSONObject()
                                            .put("username", "user")
                                            .put("active", "")
                                            .put("password", "pass")),
                            "active: must not be null"
                    )
            );
        }
    }
}
