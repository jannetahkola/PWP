package fi.jannetahkola.palikka.users.api.user;

import fi.jannetahkola.palikka.users.data.user.UserEntity;
import fi.jannetahkola.palikka.users.data.user.UserRepository;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import fi.jannetahkola.palikka.users.util.CryptoUtils;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
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

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SqlForUsers
class UserControllerIT extends IntegrationTest {
    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetUserRequest_whenSystemToken_thenOkResponse() {
            given()
                    .header(newSystemToken())
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(200);
        }

        @Test
        void givenGetUserRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
            given()
                    .get("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newViewerToken())
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
        void givenGetUserRequest_whenNoAllowedRoleButRequestedForSelf_thenOkResponse() {
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
        void givenGetUsersRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
            given()
                    .get("/users")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(newViewerToken())
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
        void givenGetPostUserRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .put("password", "mock-pass")
                    .put("active", true)
                    .toString();

            Function<Header, ValidatableResponse> postRequest = (Header authorizationHeader) -> {
                RequestSpecification spec = given();
                if (authorizationHeader != null) {
                    spec.header(authorizationHeader);
                }
                return spec
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .post("/users")
                        .then();
            };

            postRequest.apply(null).assertThat().statusCode(403);
            postRequest.apply(newViewerToken()).assertThat().statusCode(403);
            postRequest.apply(newUserToken()).assertThat().statusCode(403);
            postRequest.apply(newSystemToken()).assertThat().statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenNoTokenOrAllowedRole_thenForbiddenResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-updated")
                    .put("password", "new-pass")
                    .put("active", false)
                    .toString();

            Function<Header, ValidatableResponse> putRequest = (Header authorizationHeader) -> {
                RequestSpecification spec = given();
                if (authorizationHeader != null) {
                    spec.header(authorizationHeader);
                }
                return spec
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .put("/users/" + USER_ID_ADMIN)
                        .then();
            };

            putRequest.apply(null).assertThat().statusCode(403);
            putRequest.apply(newViewerToken()).assertThat().statusCode(403);
            putRequest.apply(newUserToken()).assertThat().statusCode(403);
            putRequest.apply(newSystemToken()).assertThat().statusCode(403);
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
                    .then().assertThat()
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
                    .body("root", equalTo(true))
                    .body("roles", hasSize(1))
                    .body("roles", contains("ROLE_ADMIN"))
                    .body("_links.self.href", endsWith("/users/" + USER_ID_ADMIN))
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_ADMIN + "/roles"));;
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
                    .body("_embedded.users", hasSize(3))
                    .body("_links.self.href", endsWith("/users"))
                    .body("_links.user.href", endsWith("/users/{id}"));
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
                    .body("_links.roles.href", endsWith("/users/" + expectedUserId + "/roles"));

            UserEntity createdUser = userRepository.findById(expectedUserId).orElseThrow();
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
                    .body("_links.roles.href", endsWith("/users/" + USER_ID_USER + "/roles"));

            UserEntity updatedUser = userRepository.findById(USER_ID_USER).orElseThrow();
            String salt = updatedUser.getSalt();
            String expectedPassword = updatedUser.getPassword();
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

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenTargetUserIsRoot_thenBadRequestResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user-3")
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_ADMIN)
                    .then().assertThat()
                    .statusCode(400)
                    .body("message", equalTo("Root user not updatable"));
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
                    .body("message", equalTo("username: must not be blank"));
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
                    .body("roles", hasSize(1));

            // Check password stays the same
            UserEntity updatedUser = userRepository.findById(USER_ID_USER).orElseThrow();
            assertThat(updatedUser.getSalt()).isEqualTo("mock-salt");
            assertThat(updatedUser.getPassword()).isEqualTo("mock-pass");
        }

        @SneakyThrows
        @Test
        void givenPutUserRequest_whenUsernameTaken_thenConflictResponse() {
            String json = new JSONObject()
                    .put("username", "mock-user")
                    .put("active", false)
                    .toString();
            given()
                    .header(newAdminToken())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json)
                    .put("/users/" + USER_ID_USER)
                    .then().assertThat()
                    .statusCode(409)
                    .body("message", equalTo("User with username 'mock-user' already exists"));
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
                                            .put("username", "user")
                                            .put("password", "password")
                                            .put("active", true)),
                            "username: must match"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Invalid username - contains invalid characters",
                                    new JSONObject()
                                            .put("username", "user")
                                            .put("password", "password$")
                                            .put("active", true)),
                            "username: must match"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Invalid username - contains spaces",
                                    new JSONObject()
                                            .put("username", "user")
                                            .put("password", "password ")
                                            .put("active", true)),
                            "username: must match"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Missing password",
                                    new JSONObject()
                                            .put("username", "username")
                                            .put("active", true)),
                            "password: must not be blank"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Blank password",
                                    new JSONObject()
                                            .put("username", "username")
                                            .put("password", "")
                                            .put("active", true)),
                            "password: must not be blank"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Invalid password - too short",
                                    new JSONObject()
                                            .put("username", "username")
                                            .put("password", "pass")
                                            .put("active", true)),
                            "password: must match"
                    ),
                    Arguments.of(
                            Named.of(
                                    "Invalid password - contains spaces",
                                    new JSONObject()
                                            .put("username", "username")
                                            .put("password", "password ")
                                            .put("active", true)),
                            "password: must match"
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
