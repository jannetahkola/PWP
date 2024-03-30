package fi.jannetahkola.palikka.game.testutils;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class Stubs {
    private Stubs() {
        // util
    }

    public static final int USER_ID_ANONYMOUS = -1;
    public static final int USER_ID_SYSTEM = 0;
    public static final int USER_ID_ADMIN = 1;
    public static final int USER_ID_USER = 2;
    public static final int USER_ID_VIEWER = 3;

    public static void stubForUser(Integer userId, WireMockExtension wireMockServer) {
        String file = null;
        if (userId == USER_ID_ADMIN) {
            file = "user_ok_admin.json";
        }
        if (userId == USER_ID_USER) {
            file = "user_ok_user.json";
        }
        if (userId == USER_ID_VIEWER) {
            file = "user_ok_viewer.json";
        }
        if (file == null) {
            throw new IllegalArgumentException("Unknown user id " + userId);
        }
        stubForUsers(wireMockServer);
        stubForUserRoles(wireMockServer, userId);
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/" + userId))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile(file)
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }

    public static void stubForAdminUser(WireMockExtension wireMockServer) {
        stubForUser(USER_ID_ADMIN, wireMockServer);
    }

    public static void stubForNormalUser(WireMockExtension wireMockServer) {
        stubForUser(USER_ID_USER, wireMockServer);
    }

    public static void stubForViewerUser(WireMockExtension wireMockServer) {
        stubForUser(USER_ID_VIEWER, wireMockServer);
    }

    public static void stubForUserNotFound(WireMockExtension wireMockServer, int userId) {
        stubForUsers(wireMockServer);
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/" + userId))
                        .willReturn(
                                aResponse()
                                        .withStatus(HttpStatus.NOT_FOUND.value())
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }

    public static void stubForUsers(WireMockExtension wireMockServer) {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("users_ok.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }

    public static void stubForUserRoles(WireMockExtension wireMockServer, int userId) {
        String file = null;
        if (userId == USER_ID_ADMIN) {
            file = "user_roles_ok_admin.json";
        }
        if (userId == USER_ID_USER) {
            file = "user_roles_ok_user.json";
        }
        if (userId == USER_ID_VIEWER) {
            file = "user_roles_ok_viewer.json";
        }
        if (file == null) {
            throw new IllegalArgumentException("Unknown user id " + userId);
        }
        wireMockServer.stubFor(
                get(urlMatching(String.format("/users-api/users/%d/roles", userId)))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile(file)
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE)));
    }
}
