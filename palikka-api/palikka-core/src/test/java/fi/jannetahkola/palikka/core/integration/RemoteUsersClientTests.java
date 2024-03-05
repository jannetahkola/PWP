package fi.jannetahkola.palikka.core.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import fi.jannetahkola.palikka.core.config.UsersIntegrationConfig;
import fi.jannetahkola.palikka.core.integration.users.User;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "logging.level.fi.jannetahkola.palikka.core.integration=debug",
                "palikka.integration.users-api.base-uri=http://localhost:8080"
        })
@ExtendWith(OutputCaptureExtension.class)
@Import(UsersIntegrationConfig.class)
class RemoteUsersClientTests {
    private static WireMockServer wireMockServer;

    @Autowired
    RemoteUsersClient usersClient;

    @BeforeAll
    public static void beforeAll() {
        // TODO User dynamci property source
        wireMockServer = new WireMockServer();
        WireMock.configureFor("localhost", 8080);
        wireMockServer.start();
    }

    @AfterAll
    public static void afterAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void givenGetUserRequest_thenOkResponse() {
        stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok_user.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        User user = usersClient.getUser(1);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1);
        assertThat(user.getUsername()).isEqualTo("mock-user-1");
        assertThat(user.getActive()).isTrue();
        assertThat(user.getRoles()).isPresent();
        assertThat(user.getRoles().get()).containsAll(Set.of("ROLE_USER"));
    }

    @Test
    void givenGetUserRequest_thenNotFoundResponse(CapturedOutput capturedOutput) {
        stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        assertThat(usersClient.getUser(1)).isNull();
        assertThat(capturedOutput.getAll()).contains("Request 'GET http://localhost:8080/users/1' failed on exception. Status=404 NOT_FOUND");
    }

    @Test
    void givenGetUserRequest_whenInvalidResponseBody_thenError(CapturedOutput capturedOutput) {
        stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        User user = usersClient.getUser(1);
        assertThat(user).isNull();
        assertThat(capturedOutput.getAll()).contains("Request 'GET http://localhost:8080/users/1' failed on invalid response.");
    }
}
