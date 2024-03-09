package fi.jannetahkola.palikka.core.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import fi.jannetahkola.palikka.core.TestSpringBootConfig;
import fi.jannetahkola.palikka.core.config.UsersIntegrationConfig;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import fi.jannetahkola.palikka.core.integration.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "logging.level.fi.jannetahkola.palikka.core.integration=debug"
        })
// Specify context explicitly so other test apps won't get loaded
@ContextConfiguration(classes = TestSpringBootConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@Import(UsersIntegrationConfig.class)
class RemoteUsersClientIT {
    @Autowired
    RemoteUsersClient usersClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("palikka.integration.users-api.base-uri", () -> wireMockServer.baseUrl());
    }

    @Test
    void givenGetUserRequest_thenOkResponse() {
        wireMockServer.stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok.json")
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
        wireMockServer.stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        assertThat(usersClient.getUser(1)).isNull();
        assertThat(capturedOutput.getAll()).contains(
                "Request 'GET http://localhost:" + wireMockServer.getPort()
                        + "/users/1' failed on exception. Status=404 NOT_FOUND");
    }

    @Test
    void givenGetUserRequest_whenInvalidResponseBody_thenError(CapturedOutput capturedOutput) {
        wireMockServer.stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        User user = usersClient.getUser(1);
        assertThat(user).isNull();
        assertThat(capturedOutput.getAll()).contains(
                "Request 'GET http://localhost:" + wireMockServer.getPort()
                        + "/users/1' failed on invalid response.");
    }
}
