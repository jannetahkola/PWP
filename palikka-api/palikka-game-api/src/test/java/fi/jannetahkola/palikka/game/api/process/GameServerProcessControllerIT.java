package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.service.ProcessFactory;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameServerProcessControllerIT extends WireMockTest {

    @Autowired
    TestTokenUtils tokens;

    @MockBean
    ProcessFactory processFactory;

    Header authorizationHeader;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        stubForAdminUser();
        authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(1));
    }

    @Test
    void givenGetProcessStatusRequest_thenOkResponse() {
        given()
                .header(authorizationHeader)
                .get("/server/process")
                .then().assertThat()
                .statusCode(200) // TODO 403 if method/content-type not supported, change?
                .body("status", equalTo("down"));
    }

    @Test
    void givenStartProcessRequest_thenOkResponse() {
        Process mockProcess = Mockito.mock(Process.class);
        when(processFactory.newGameProcess(any(), any())).thenReturn(mockProcess);

        // TODO POST
    }
}
