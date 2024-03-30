package fi.jannetahkola.palikka.game.api.file;

import fi.jannetahkola.palikka.game.api.file.model.GameConfigUpdateRequest;
import fi.jannetahkola.palikka.game.exception.GameFileNotFoundException;
import fi.jannetahkola.palikka.game.service.GameFileProcessor;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import fi.jannetahkola.palikka.game.testutils.IntegrationTest;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.Header;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSpecification;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(GameFileControllerIT.GameFileControllerTestConfiguration.class)
class GameFileControllerIT extends IntegrationTest {
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
        @ParameterizedTest
        @MethodSource("usersNotAllowedToCallModifyingGameFileEndpoints")
        void givenGetExecutableDownloadStatusRequest_whenNoTokenOrRole_thenForbiddenResponse(Integer user) {
            RequestSpecification requestSpec = given();
            setupRequestAuthentication(requestSpec, user, tokens);
            requestSpec
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("usersNotAllowedToCallModifyingGameFileEndpoints")
        void givenStartExecutableDownloadRequest_whenNoTokenOrRole_thenForbiddenResponse(Integer user) {
            RequestSpecification requestSpec = given();
            setupRequestAuthentication(requestSpec, user, tokens);
            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            requestSpec
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(403);
        }

        @ParameterizedTest
        @MethodSource("usersNotAllowedToCallNonModifyingGameFileEndpoints")
        void givenGetExecutableMetadataRequest_whenNoTokenOrRole_thenForbiddenResponse(Integer user) {
            RequestSpecification requestSpec = given();
            setupRequestAuthentication(requestSpec, user, tokens);
            requestSpec
                    .get("/game/files/executable/meta")
                    .then().assertThat()
                    .statusCode(403);
        }

        @ParameterizedTest
        @MethodSource("usersNotAllowedToCallNonModifyingGameFileEndpoints")
        void givenGetConfigRequest_whenNoTokenOrRole_thenForbiddenResponse(Integer user) {
            RequestSpecification requestSpec = given();
            setupRequestAuthentication(requestSpec, user, tokens);
            requestSpec
                    .get("/game/files/config")
                    .then().assertThat()
                    .statusCode(403);
        }

        @ParameterizedTest
        @MethodSource("usersNotAllowedToCallModifyingGameFileEndpoints")
        void givenPutConfigRequest_whenNoTokenOrRole_thenForbiddenResponse(Integer user) {
            RequestSpecification requestSpec = given();
            setupRequestAuthentication(requestSpec, user, tokens);
            GameConfigUpdateRequest request = new GameConfigUpdateRequest();
            request.setConfig("config");
            requestSpec
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .put("/game/files/config")
                    .then().assertThat()
                    .statusCode(403);
        }

        @ParameterizedTest
        @MethodSource("usersNotAllowedToCallModifyingGameFileEndpoints")
        void givenPutIconRequest_whenNoTokenOrRole_thenForbiddenResponse(Integer user) {
            RequestSpecification requestSpec = given();
            setupRequestAuthentication(requestSpec, user, tokens);
            MultiPartSpecification file = new MultiPartSpecBuilder("File content".getBytes())
                    .fileName("file.txt")
                    .controlName("file")
                    .mimeType("text/plain")
                    .build();
            requestSpec
                    .multiPart(file)
                    .put("/game/files/icon")
                    .then().assertThat()
                    .statusCode(403);
        }

        static void setupRequestAuthentication(RequestSpecification requestSpecification, Integer user, TestTokenUtils tokens) {
            if (user > USER_ID_SYSTEM) {
                stubForUser(user, wireMockServer);
                requestSpecification.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(user));
            } else if (user == USER_ID_SYSTEM) {
                requestSpecification.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateSystemToken());
            }
        }

        static Stream<Arguments> usersNotAllowedToCallNonModifyingGameFileEndpoints() {
            return Stream.of(
                    Arguments.of(Named.of("SYSTEM", USER_ID_SYSTEM)),
                    Arguments.of(Named.of("ANONYMOUS", USER_ID_ANONYMOUS))
            );
        }

        static Stream<Arguments> usersNotAllowedToCallModifyingGameFileEndpoints() {
            return Stream.of(
                    Arguments.of(Named.of("USER", USER_ID_USER)),
                    Arguments.of(Named.of("VIEWER", USER_ID_VIEWER)),
                    Arguments.of(Named.of("SYSTEM", USER_ID_SYSTEM)),
                    Arguments.of(Named.of("ANONYMOUS", USER_ID_ANONYMOUS))
            );
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @Autowired
        GameFileProcessor gameFileProcessor;

        @Autowired
        GameProcessService gameProcessService;

        Header authorizationHeader;

        @SneakyThrows
        @BeforeEach
        void beforeEach() {
            stubForAdminUser(wireMockServer);
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(1));

            MockGameFileProcessor gameFileProcessorMock = (MockGameFileProcessor) gameFileProcessor;
            when(gameFileProcessorMock.readFile(any(), any())).thenCallRealMethod();
            doCallRealMethod().when(gameFileProcessorMock).downloadFile(any(), any());
            doCallRealMethod().when(gameFileProcessorMock).acceptEula(any());
            doCallRealMethod().when(gameFileProcessorMock).setCountDownLatch(any());
            gameFileProcessorMock.setCountDownLatch(null);

            when(gameProcessService.isDown()).thenReturn(true);
        }

        @AfterEach
        void afterEach() {
            Mockito.reset((MockGameFileProcessor) gameFileProcessor);
        }

        @SneakyThrows
        @Test
        void givenGetExecutableDownloadStatusRequest_thenOkResponse() {
            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartExecutableDownloadRequest_whenSuccess_thenOkResponseWithCorrectStatusAndHeader() {
            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(204)
                    .header(HttpHeaders.LOCATION, Matchers.endsWith("/game/files/executable/download"));

            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("success"));

            // Check that status is reset after
            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartExecutableDownloadRequest_whenFail_thenOkResponseWithCorrectStatus() {
            JSONObject json = new JSONObject().put("download_url", "https://test.com/fail");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(204);
            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("failed"));

            // Check that status is reset after
            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("idle"));
        }

        @SneakyThrows
        @Test
        void givenStartExecutableDownloadRequest_thenStatusSwitchedCorrectly_andOkResponse() {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ((MockGameFileProcessor) gameFileProcessor).setCountDownLatch(countDownLatch);

            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(204);
            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("working"));

            countDownLatch.countDown();

            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("success"));
        }

        @SneakyThrows
        @Test
        void givenStartExecutableDownloadRequest_whenGameProcessIsNotDown_thenBadRequestResponse() {
            when(gameProcessService.isDown()).thenReturn(false);

            JSONObject json = new JSONObject().put("download_url", "https://test.com");
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", is("Game files cannot be modified when game status is not DOWN"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("invalidDownloadUrlArgs")
        void givenStartExecutableDownloadRequest_withInvalidParameters_thenBadRequestResponse(String downloadUrl) {
            JSONObject json = new JSONObject().put("download_url", downloadUrl);
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(json.toString())
                    .post("/game/files/executable/download")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", is("Invalid download URL"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @Test
        void givenGetExecutableMetadataRequest_thenOkResponse() {
            given()
                    .header(authorizationHeader)
                    .get("/game/files/executable/meta")
                    .then().assertThat()
                    .statusCode(200)
                    .body("exists", equalTo(false));
        }

        @SneakyThrows
        @Test
        void givenGetConfigRequest_whenNotFound_thenNotFoundResponse() {
            when(gameFileProcessor.readFile(any(), any())).thenThrow(new GameFileNotFoundException(""));
            given()
                    .header(authorizationHeader)
                    .get("/game/files/config")
                    .then().assertThat()
                    .statusCode(404);
        }

        @SneakyThrows
        @Test
        void givenGetConfigRequest_whenFound_thenOkResponse() {
            when(gameFileProcessor.readFile(any(), any())).thenReturn(List.of("config"));
            given()
                    .header(authorizationHeader)
                    .get("/game/files/config")
                    .then().assertThat()
                    .statusCode(200)
                    .body("config", hasSize(1));
        }

        @SneakyThrows
        @Test
        void givenPutConfigRequest_thenAcceptedResponse() {
            when(gameFileProcessor.readFile(any(), any())).thenReturn(List.of("updated-config"));
            GameConfigUpdateRequest request = new GameConfigUpdateRequest();
            request.setConfig("updated-config");
            given()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(authorizationHeader)
                    .body(request)
                    .put("/game/files/config")
                    .then().assertThat()
                    .statusCode(202)
                    .body("config", hasSize(1));
        }

        @SneakyThrows
        @Test
        void givenPutIconRequest_whenFileIsNotPNG_thenBadRequestResponse() {
            MultiPartSpecification file = new MultiPartSpecBuilder("File content".getBytes())
                    .fileName("file.txt")
                    .controlName("file")
                    .mimeType("text/plain")
                    .build();
            given()
                    .header(authorizationHeader)
                    .multiPart(file)
                    .put("/game/files/icon")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", equalTo("Provided file is not a PNG"));
        }

        @SneakyThrows
        @Test
        void givenPutIconRequest_whenFileIsEmpty_thenBadRequestResponse() {
            MultiPartSpecification file = new MultiPartSpecBuilder("".getBytes()).build();
            given()
                    .header(authorizationHeader)
                    .multiPart(file)
                    .put("/game/files/icon")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", equalTo("Provided file is missing or invalid"));
        }

        @SneakyThrows
        @Test
        void givenPutIconRequest_thenAcceptedResponse() {
            MultiPartSpecification file = new MultiPartSpecBuilder("File content".getBytes())
                    .fileName("file.png")
                    .controlName("file")
                    .mimeType("image/png")
                    .build();
            given()
                    .header(authorizationHeader)
                    .multiPart(file)
                    .put("/game/files/icon")
                    .then().assertThat()
                    .statusCode(202);
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
    }

    @TestConfiguration
    public static class GameFileControllerTestConfiguration {
        @Bean
        @Primary
        GameFileProcessor gameFileProcessor() {
            // Mock the mock implementation, so we can both mock the methods and have the countdown latch.
            return Mockito.mock(MockGameFileProcessor.class);
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

        @Override
        public void writeFile(String pathToDir, String filename, String fileContent) {
            // noop
        }

        @Override
        public void saveFile(String pathToDir, MultipartFile file) {
            // noop
        }
    }
}
