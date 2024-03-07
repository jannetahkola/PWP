package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.service.FileDownloaderService;
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
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForNormalUser;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(GameServerFileControllerIT.GameServerFileControllerTestConfiguration.class)
class GameServerFileControllerIT extends WireMockTest {
    @Autowired
    TestTokenUtils tokens;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
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
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2))
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenNoTokenOrRole_thenForbiddenResponse() {
            stubForNormalUser(wireMockServer);

            JSONObject json = new JSONObject().put("download_uri", "https://test.com");
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/server/download")
                    .then().assertThat()
                    .statusCode(403);
            given()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/server/download")
                    .then().assertThat()
                    .statusCode(403);
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Autowired
        FileDownloaderService fileDownloaderService;

        Header authorizationHeader;

        @BeforeEach
        void beforeEach() {
            stubForAdminUser(wireMockServer);
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(1));
            ((MockFileDownloaderService) fileDownloaderService).setCountDownLatch(null);
        }

        @SneakyThrows
        @Test
        void givenGetDownloadStatusRequest_thenOkResponse() {
            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenSuccess_thenOkResponseWithCorrectStatus() {
            JSONObject json = new JSONObject().put("download_uri", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE) // TODO If content type missing, returns 403. Change?
                    .body(json.toString())
                    .post("/server/download")
                    .then().assertThat()
                    .statusCode(200);
            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("success"));

            // Check that status is reset after
            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_whenFail_thenOkResponseWithCorrectStatus() {
            JSONObject json = new JSONObject().put("download_uri", "https://test.com/fail");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/server/download")
                    .then().assertThat()
                    .statusCode(200);
            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("failed"));

            // Check that status is reset after
            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartDownloadRequest_thenStatusSwitchedCorrectly_andOkResponse() {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ((MockFileDownloaderService) fileDownloaderService).setCountDownLatch(countDownLatch);

            JSONObject json = new JSONObject().put("download_uri", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/server/download")
                    .then().assertThat()
                    .statusCode(200);
            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("working"));

            countDownLatch.countDown();

            given()
                    .header(authorizationHeader)
                    .get("/server/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("success"));
        }
    }

    @TestConfiguration
    public static class GameServerFileControllerTestConfiguration {
        @Bean
        @Primary
        FileDownloaderService fileDownloaderService() {
            return new MockFileDownloaderService();
        }
    }

    @Setter
    public static class MockFileDownloaderService extends FileDownloaderService {
        private CountDownLatch countDownLatch;

        @Override
        public void download(URI downloadUri, File toFile) throws IOException {
            if (downloadUri.getPath().contains("fail")) {
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
    }
}
