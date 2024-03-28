package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.core.auth.data.RevokedTokenEntity;
import fi.jannetahkola.palikka.core.auth.data.RevokedTokenRepository;
import fi.jannetahkola.palikka.core.config.properties.JwtProperties;
import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import io.restassured.http.Header;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
                .header(newSystemToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(403)
                .body("detail", equalTo("Access Denied"))
                .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }
}
