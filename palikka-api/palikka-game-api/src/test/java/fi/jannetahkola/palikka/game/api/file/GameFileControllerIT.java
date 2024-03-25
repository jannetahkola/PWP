package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.service.GameFileProcessor;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import lombok.Setter;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForNormalUser;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(GameFileControllerIT.GameFileControllerTestConfiguration.class)
class GameFileControllerIT extends WireMockTest {
    @Autowired
    TestTokenUtils tokens;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.basePath = "/game-api";
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("palikka.integration.users-api.base-uri", () -> wireMockServer.baseUrl());
    }

    @Nested
    class ResourceSecurityIT {
        @Test
        void givenGetDownloadStatusRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            stubForNormalUser(wireMockServer);

            given()
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2))
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            stubForNormalUser(wireMockServer);

            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(403);
        }

        @Test
        void givenGetConfigRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            stubForNormalUser(wireMockServer);

            given()
                    .get("/game/files/config")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2))
                    .get("/game/files/config")
                    .then().assertThat()
                    .statusCode(403);
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Autowired
        GameFileProcessor gameFileProcessor;

        @Autowired
        GameProcessService gameProcessService;

        Header authorizationHeader;

        @BeforeEach
        void beforeEach() {
            stubForAdminUser(wireMockServer);
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(1));
            ((MockGameFileProcessor) gameFileProcessor).setCountDownLatch(null);
            when(gameProcessService.isDown()).thenReturn(true);
        }

        @SneakyThrows
        @Test
        void givenGetDownloadStatusRequest_thenOkResponse() {
            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenSuccess_thenOkResponseWithCorrectStatusAndHeader() {
            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(204)
                    .header(HttpHeaders.LOCATION, endsWith("/game/files/download"));

            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("success"));

            // Check that status is reset after
            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenFail_thenOkResponseWithCorrectStatus() {
            JSONObject json = new JSONObject().put("download_url", "https://test.com/fail");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(204);
            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("failed"));

            // Check that status is reset after
            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_thenStatusSwitchedCorrectly_andOkResponse() {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ((MockGameFileProcessor) gameFileProcessor).setCountDownLatch(countDownLatch);

            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(204);
            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("working"));

            countDownLatch.countDown();

            given()
                    .header(authorizationHeader)
                    .get("/game/files/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("success"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenGameProcessIsNotDown_thenBadRequestResponse() {
            when(gameProcessService.isDown()).thenReturn(false);

            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", is("Game files cannot be modified when game status is not DOWN"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("invalidDownloadUrlArgs")
        void givenStartDownloadRequest_withInvalidParameters_thenBadRequestResponse(String downloadUrl) {
            JSONObject json = new JSONObject().put("download_url", downloadUrl);
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/download")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", is("Invalid download URL"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        // todo synchronization tests

        static Stream<Arguments> invalidDownloadUrlArgs() {
            return Stream.of(
                    Arguments.of("test"),
                    Arguments.of("test.com")
                    // Below are disabled because they throw a jakarta validation errors
//                    Arguments.of(""),
//                    Arguments.of(" ")
            );
        }

        @Test
        void givenGetConfigRequest_thenOkResponse() {
            // todo test error cases
            given()
                    .header(authorizationHeader)
                    .get("/game/files/config")
                    .then().assertThat()
                    .statusCode(200)
                    .body("config", hasSize(0));
        }
    }

    @TestConfiguration
    public static class GameFileControllerTestConfiguration {
        @Bean
        @Primary
        GameFileProcessor gameFileProcessor() {
            return new MockGameFileProcessor();
        }

        @Bean
        @Primary
        GameProcessService gameProcessService() {
            // @MockBean doesn't work in this class for some reason
            return Mockito.mock(GameProcessService.class);
        }
    }

    @Setter
    public static class MockGameFileProcessor extends GameFileProcessor {
        private CountDownLatch countDownLatch;

        @Override
        public void downloadFile(URL downloadUrl, File toFile) throws IOException {
            if (downloadUrl.getPath().contains("fail")) {
                throw new IOException("Download failed");
            }
            if (countDownLatch != null) {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    this.countDownLatch = null;
                }
            }
        }

        @Override
        public void acceptEula(File toFile) {
            // noop
        }

        @Override
        public List<String> readFile(String pathToDir, String fileName) {
            return Collections.emptyList();
        }
    }
}
