package fi.jannetahkola.palikka.core.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import fi.jannetahkola.palikka.core.TestSpringBootConfig;
import fi.jannetahkola.palikka.core.config.meta.EnableRemoteUsersIntegration;
import fi.jannetahkola.palikka.core.integration.users.Privilege;
import fi.jannetahkola.palikka.core.integration.users.RemoteUsersClient;
import fi.jannetahkola.palikka.core.integration.users.Role;
import fi.jannetahkola.palikka.core.integration.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Collection;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "logging.level.fi.jannetahkola.palikka.core.integration=debug",

                "palikka.jwt.keystore.signing.path=keystore-dev.p12",
                "palikka.jwt.keystore.signing.pass=password",
                "palikka.jwt.keystore.signing.type=pkcs12",

                "palikka.jwt.token.system.signing.key-alias=jwt-sys",
                "palikka.jwt.token.system.signing.key-pass=password",
                "palikka.jwt.token.system.signing.validity-time=10s",
                "palikka.jwt.token.system.issuer=palikka-dev-system"
        })
// Specify context explicitly so other test apps won't get loaded
@ContextConfiguration(classes = {TestSpringBootConfig.class, RestTemplateAutoConfiguration.class})
@ExtendWith(OutputCaptureExtension.class)
@EnableRemoteUsersIntegration
class RemoteUsersClientIT {
    @Autowired
    RemoteUsersClient usersClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().globalTemplating(true))
            .build();

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("palikka.integration.users-api.base-uri", () -> wireMockServer.baseUrl());
    }

    @Test
    void givenGetUserRequest_thenOkResponse() {
        stubForUsersOkResponse();
        stubForUserOkResponse();

        User user = usersClient.getUser(1);
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1);
        assertThat(user.getUsername()).isEqualTo("mock-user-1");
        assertThat(user.getActive()).isTrue();
        assertThat(user.getRoot()).isTrue();
        assertThat(user.getRoles()).isPresent();
        assertThat(user.getRoles().get()).containsAll(Set.of("ROLE_ADMIN"));
    }

    @Test
    void givenGetUserRequest_thenNotFoundResponse(CapturedOutput capturedOutput) {
        stubForUsersOkResponse();
        stubForUserNotFoundResponse();

        assertThat(usersClient.getUser(1)).isNull();
        assertThat(capturedOutput.getAll()).contains("<< GET user - response has constraint violations");
    }

    @Test
    void givenGetUserRequest_whenInvalidResponseBody_thenError(CapturedOutput capturedOutput) {
        stubForUsersOkResponse();
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));

        User user = usersClient.getUser(1);
        assertThat(user).isNull();
        assertThat(capturedOutput.getAll()).contains("<< GET user - response has constraint violations");
    }

    @Test
    void givenGetUserRolesRequest_thenOkResponse() {
        stubForUsersOkResponse();
        stubForUserOkResponse();
        stubForUserRolesOkResponse();

        Collection<Role> userRoles = usersClient.getUserRoles(1);
        assertThat(userRoles).isNotEmpty();

        Role role = userRoles.stream().findAny().orElseThrow();
        assertThat(role.getId()).isNotNull();
        assertThat(role.getName()).isNotNull();
        assertThat(role.getPrivileges()).isNotEmpty();

        role.getPrivileges().orElseThrow().forEach(privilege -> {
            assertThat(privilege.getId()).isNotNull();
            assertThat(privilege.getDomain()).isNotNull();
            assertThat(privilege.getName()).isNotNull();
        });
    }

    @Test
    void givenGetUserRolesRequest_whenUserNotFound_thenSomething(CapturedOutput capturedOutput) {
        stubForUsersOkResponse();
        stubForUserNotFoundResponse();
        stubForUserRolesOkResponse();

        Collection<Role> userRoles = usersClient.getUserRoles(1);
        assertThat(userRoles).isEmpty();
        assertThat(capturedOutput.getAll()).contains(
                "java.lang.IllegalStateException: Expected to find link with rel 'roles' in response");
    }

    static void stubForUsersOkResponse() {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("users_ok.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }

    static void stubForUserOkResponse() {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }

    static void stubForUserRolesOkResponse() {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/1/roles"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_roles_ok.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }

    static void stubForUserNotFoundResponse() {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }
}
