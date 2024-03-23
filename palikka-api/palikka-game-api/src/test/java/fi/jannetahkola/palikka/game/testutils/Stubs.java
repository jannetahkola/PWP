package fi.jannetahkola.palikka.game.testutils;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class Stubs {
    private Stubs() {
        // util
    }

    public static final int USER_ID_ADMIN = 1;
    public static final int USER_ID_USER = 2;
    public static final int USER_ID_VIEWER = 3;

    public static void stubForAdminUser(WireMockExtension wireMockServer) {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/" + USER_ID_ADMIN))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok_admin.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubForNormalUser(WireMockExtension wireMockServer) {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/" + USER_ID_USER))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok_user.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubForViewerUser(WireMockExtension wireMockServer) {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/" + USER_ID_VIEWER))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok_viewer.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubForUserNotFound(WireMockExtension wireMockServer, int userId) {
        wireMockServer.stubFor(
                get(urlMatching("/users-api/users/" + userId))
                        .willReturn(
                                aResponse()
                                        .withStatus(HttpStatus.NOT_FOUND.value())
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }
}
