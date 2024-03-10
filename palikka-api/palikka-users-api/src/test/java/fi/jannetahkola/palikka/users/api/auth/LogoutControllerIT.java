package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@SqlForUsers
class LogoutControllerIT extends IntegrationTest {
    @Nested
    class ResourceFunctionalityTests {
        @Test
        @Disabled("fails until token blacklist fully supported") // todo just use redis for storing these as they need to be cleared often anyways?
        void givenLogoutRequest_thenTokenInvalidated_andOkResponse() {
            given()
                    .header(newUserToken())
                    .post("/auth/logout")
                    .then().assertThat()
                    .statusCode(200);

            given()
                    .header(newUserToken())
                    .post("/auth/logout")
                    .then().assertThat()
                    .statusCode(403);
        }
    }
}
