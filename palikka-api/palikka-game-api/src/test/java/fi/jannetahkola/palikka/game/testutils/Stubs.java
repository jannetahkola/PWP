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

    public static void stubForAdminUser(WireMockExtension wireMockServer) {
        wireMockServer.stubFor(
                get(urlMatching("/users/1"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok_admin.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubForNormalUser(WireMockExtension wireMockServer) {
        wireMockServer.stubFor(
                get(urlMatching("/users/2"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withBodyFile("user_ok_user.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubForUserNotFound(WireMockExtension wireMockServer, int userId) {
        wireMockServer.stubFor(
                get(urlMatching("/users/" + userId))
                        .willReturn(
                                aResponse()
                                        .withStatus(HttpStatus.NOT_FOUND.value())
                                        .withBodyFile("user_notfound.json")
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }
}
