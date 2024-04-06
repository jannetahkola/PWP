package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
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

class UserControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
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
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenGetUserRequest_whenNoAllowedRole_thenForbiddenResponse() {
            given()
                    .header(newViewerToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            given()
                    .header(newUserToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
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
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
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
                    .statusCode(403);
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
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
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
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            given()
                    .header(newSystemBearerTokenHeader())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
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
                    .statusCode(403)
                    .body("detail", equalTo("Full authentication is required to access this resource"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
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
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
            given()
                    .header(newSystemBearerTokenHeader())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403)
                    .body("detail", equalTo("Access Denied"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenNoAllowedRoleButRequestedForSelf_thenAcceptedResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-updated")
                    .put("active", false) // Cannot update unless admin
                    .toString();
            given()
                    .header(newUserToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(202)
                    .body("username", equalTo("mock-user-updated"))
                    .body("active", equalTo(true));
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Test
        void givenAllUsersOptionsRequest_thenAllowedMethodsReturned() {
            given()
                    .header(newAdminToken())
                    .options("/users")
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .header(HttpHeaders.ALLOW, containsString("GET"))
                    .header(HttpHeaders.ALLOW, containsString("POST"))
                    .header(HttpHeaders.ALLOW, not(containsString("PUT")));
        }

        @Test
        void givenSingleUserOptionsRequest_thenAllowedMethodsReturned() {
            given()
                    .header(newAdminToken())
                    .options("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(200)
                    .header(HttpHeaders.ALLOW, containsString("GET"))
                    .header(HttpHeaders.ALLOW, containsString("PUT"))
                    .header(HttpHeaders.ALLOW, not(containsString("POST")));
        }

        @Test
        void givenGetUsersRequest_whenAcceptHalFormsHeaderGiven_thenResponseContainsTemplate() {
            // todo check template contents
            given()
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    .header(newAdminToken())
                    .get("/users")
                    .then().assertThat()
                    .body("_templates.default.method", equalTo("POST"))
                    .statusCode(200);
        }

        @Test
        void givenGetUserRequest_whenAcceptHalFormsHeaderGiven_thenResponseContainsTemplate() {
            given()
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    .header(newAdminToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .body("_templates.default.method", equalTo("PUT"))
                    .statusCode(200);
        }

        @Test
        void givenGetUserRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().log().all().assertThat()
                    .statusCode(200)
                    .body("id", equalTo(1))
                    .body("username", equalTo("admin"))
                    .body("password", nullValue())
                    .body("active", equalTo(true))
                    .body("root", equalTo(true))
                    .body("roles", hasSize(1))
                    .body("roles", contains("ROLE_ADMIN"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN))
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
        }

        @Test
        void givenGetUserRequest_whenUserNotFound_thenNotFoundResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users/9999")
                    .then().assertThat()
                    .statusCode(404)
                    .body("detail", equalTo("User with id '9999' not found"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenGetUsersRequest_thenOkResponse() {
            given()
                    .header(newAdminToken())
                    .get("/users")
                    .then().assertThat()
                    .statusCode(200)
                    .body("_embedded.users", hasSize(3))
                    .body("_links.self.href", endsWith("/users"))
                    .body("_links.user.href", endsWith("/users/{id}"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPostUserRequest_thenOkResponse(@Autowired UserRepository userRepository) {
            final int expectedUserId = 4;

            // Using JSONObject because password won't be included during serialization in the model
            String json = new JSONObject()
                    .put("username", "mock-user-updated")
                    .put("password", "mock-pass")
                    .put("active", true)
                    .put("root", true) // Cannot be set by clients, should be false
                    .put("roles", "[\"ROLE_USER\"]") // Cannot be set during POST, should be empty
                    .toString();

            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .post("/users")
                    .then().assertThat()
                    .statusCode(201)
                    .header(HttpHeaders.LOCATION, endsWith("/users/" + expectedUserId))
                    .body("id", equalTo(expectedUserId))
                    .body("username", equalTo("mock-user-updated"))
                    .body("password", nullValue())
                    .body("salt", nullValue())
                    .body("active", equalTo(true))
                    .body("root", equalTo(false))
                    .body("created_at", endsWith("Z"))
                    .body("last_updated_at", nullValue())
                    .body("roles", hasSize(0))
                    .body("_links.self.href", endsWith("/users/" + expectedUserId))
                    .body("_links.roles.href", endsWith("/users/" + expectedUserId + "/roles"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));

            UserEntity createdUser = userRepository.findById(expectedUserId).orElseThrow();
            String salt = createdUser.getSalt();
            String expectedPassword = createdUser.getPassword();
            assertThat(CryptoUtils.validatePassword("mock-pass".toCharArray(), salt, expectedPassword)).isTrue();
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
                    .body("detail", containsString(expectedMessageSubstring))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPostUserRequest_whenUsernameTaken_thenConflictResponse() {
            String json = new JSONObject()
                    .put("username", "admin")
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
                    .body("detail", equalTo("User with username 'admin' already exists"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_thenAcceptedResponse(@Autowired UserRepository userRepository) {
            String json = new JSONObject()
                    .put("username", "mock-user-updated")
                    .put("password", "new-pass")
                    .put("active", false) // Admin can update
                    .put("root", true) // Cannot be updated, should stay false
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(202)
                    .body("username", equalTo("mock-user-updated"))
                    .body("password", nullValue())
                    .body("salt", nullValue())
                    .body("active", equalTo(false))
                    .body("root", equalTo(false))
                    .body("created_at", endsWith("Z"))
                    .body("last_updated_at", endsWith("Z"))
                    .body("roles", hasSize(1))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_USER))
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_USER + "/roles"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));

            UserEntity updatedUser = userRepository.findById(USER_ID_USER).orElseThrow();
            String salt = updatedUser.getSalt();
            String expectedPassword = updatedUser.getPassword();
            assertThat(CryptoUtils.validatePassword("new-pass".toCharArray(), salt, expectedPassword)).isTrue();
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenUserNotFound_thenNotFoundResponse() {
            String json = new JSONObject()
                    .put("username", "viewer")
                    .put("active", false)
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/9999")
                    .then().assertThat()
                    .statusCode(404)
                    .body("detail", equalTo("User with id '9999' not found"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenTargetUserIsRoot_thenBadRequestResponse() {
            String json = new JSONObject()
                    .put("username", "viewer")
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", equalTo("Root user not updatable"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenParametersInvalid_thenBadRequestResponse() {
            String json = new JSONObject().toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", equalTo("username: must not be blank"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_thenOtherFieldsNotChanged_andAcceptedResponse(@Autowired UserRepository userRepository) {
            String json = new JSONObject()
                    .put("username", "mock-user-updated")
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(202)
                    .body("username", equalTo("mock-user-updated"))
                    .body("password", nullValue())
                    .body("salt", nullValue())
                    .body("active", equalTo(true))
                    .body("root", equalTo(false))
                    .body("last_updated_at", not(nullValue()))
                    .body("roles", hasSize(1))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaTypes.HAL_JSON_VALUE));

            // Check password stays the same
            UserEntity updatedUser = userRepository.findById(USER_ID_USER).orElseThrow();
            assertThat(updatedUser.getSalt()).isEqualTo("pa2agTlplJ9FsYEmElH4iA==");
            assertThat(updatedUser.getPassword()).isEqualTo("AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=");
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenUsernameTaken_thenConflictResponse() {
            String json = new JSONObject()
                    .put("username", "admin")
                    .put("active", false)
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(409)
                    .body("detail", equalTo("User with username 'admin' already exists"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenPostUserRequest_withoutContentType_thenUnsupportedMediaTypeResponse() {
            given()
                    .header(newAdminToken())
                    .noContentType()
                    .post("/users")
                    .then().assertThat()
                    .statusCode(415)
                    .body("detail", equalTo("Content-Type 'null' is not supported."))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenPutUserRequest_withoutContentType_thenUnsupportedMediaTypeResponse() {
            given()
                    .header(newAdminToken())
                    .noContentType()
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(415)
                    .body("detail", equalTo("Content-Type 'null' is not supported."))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        private static Stream<Arguments> invalidPostUserParameters() {
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
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing active",
                                    new JSONObject()
                                            .put("username", "username")
                                            .put("password", "password")),
                            "active: must not be null"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing active",
                                    new JSONObject()
                                            .put("username", "username")
                                            .put("active", "")
                                            .put("password", "password")),
                            "active: must not be null"
                    )
            );
        }
    }
}
