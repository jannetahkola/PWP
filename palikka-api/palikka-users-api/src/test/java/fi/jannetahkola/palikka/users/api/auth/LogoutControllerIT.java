package fi.jannetahkola.palikka.users.api.auth;

import fi.jannetahkola.palikka.users.testutils.IntegrationTest;
import fi.jannetahkola.palikka.users.testutils.SqlForUsers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@SqlForUsers
class LogoutControllerIT extends IntegrationTest {
    @Test
    void test() {
        given()
                .header(newUserToken())
                .post("/auth/logout")
                .then().assertThat()
                .statusCode(200);
    }
}
