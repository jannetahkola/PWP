package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.process.GameProcess;
import fi.jannetahkola.palikka.game.service.GameServerProcessService;
import fi.jannetahkola.palikka.game.service.PathValidator;
import fi.jannetahkola.palikka.game.service.ProcessFactory;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForNormalUser;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests the asynchronous lifecycle control actions. Depending on the host, test timeout may need to be increased.
 * Remember to stop the game process before each test!
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameServerProcessControllerIT extends WireMockTest {
    static final String SERVER_START_LOG = "[19:56:37] [Server thread/INFO]: Done (13.324s)! For help, type \"help\"";

    @Value("#{T(java.lang.Long).parseLong('${palikka.test.timeout-in-millis}')}")
    Long testTimeoutMillis;

    @Autowired
    TestTokenUtils tokens;

    @Autowired
    GameServerProcessService gameServerProcessService;

    @MockBean
    ProcessFactory processFactory;

    @MockBean
    PathValidator pathValidator;

    Header authorizationHeader;

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
        @BeforeEach
        void beforeEach() {
            stubForNormalUser(wireMockServer);
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2));
        }

        @Test
        void givenGetProcessStatusRequest_whenNoTokenOrRoles_thenForbiddenResponse() {
            given()
                    .get("/server/process")
                    .then().assertThat()
                    .statusCode(403);

            given()
                    .header(authorizationHeader)
                    .get("/server/process")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenProcessControlRequest_whenNoTokenOrRoles_thenForbiddenResponse() {
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(403);

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(403);
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        CountDownLatch processStartLatch;
        CountDownLatch processExitLatch;
        CountDownLatch gameStartLatch;
        PipedOutputStream gameOut;

        @BeforeEach
        void beforeEach() {
            stubForAdminUser(wireMockServer);
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

        @SneakyThrows
        @Test
        void givenProcessControlRequestWithInvalidParams_thenBadRequestResponse() {
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "invalid").toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(400)
                    .body("message", equalTo("Invalid request"));

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", null).toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(400)
                    .body("message", equalTo("action: must not be null"));
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void givenProcessControlRequest_whenActionsIsStart_andAlreadyUp_thenBadRequestResponse() {
            mockGameProcess();

            List<CompletableFuture<Response>> startFutures = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                startFutures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return given()
                                .header(authorizationHeader)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .body(new JSONObject().put("action", "start").toString())
                                .post("/server/process")
                                .thenReturn();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            CompletableFuture.allOf(startFutures.toArray(new CompletableFuture[0])).get(testTimeoutMillis, TimeUnit.MILLISECONDS);

            List<Response> responses = startFutures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            long successfulResponses = responses.stream().filter(r -> r.getStatusCode() == 200).count();
            long badRequestResponses = responses.stream().filter(r -> r.getStatusCode() == 400).count();
            assertThat(successfulResponses).isEqualTo(1);
            assertThat(badRequestResponses).isEqualTo(2);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "stop").toString())
                    .post("/server/process");
            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void givenProcessControlRequest_whenActionIsStop_andAlreadyDown_thenBadRequestResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/server/process")
                    .thenReturn();

            assertThat(processStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            List<CompletableFuture<Response>> startFutures = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                startFutures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return given()
                                .header(authorizationHeader)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .body(new JSONObject().put("action", "stop").toString())
                                .post("/server/process")
                                .thenReturn();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }

            CompletableFuture.allOf(startFutures.toArray(new CompletableFuture[0])).get(testTimeoutMillis, TimeUnit.MILLISECONDS);

            List<Response> responses = startFutures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            long successfulResponses = responses.stream().filter(r -> r.getStatusCode() == 200).count();
            long badRequestResponses = responses.stream().filter(r -> r.getStatusCode() == 400).count();
            assertThat(successfulResponses).isEqualTo(1);
            assertThat(badRequestResponses).isEqualTo(2);

            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
        }

        @SneakyThrows
        @Test
        void givenProcessControlRequest_whenActionIsStop_andProcessStillStarting_thenBadRequestResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/server/process")
                    .thenReturn();

            assertThat(processStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "stop").toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(400);

            gameServerProcessService.stopForcibly();
            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
        }

        @SneakyThrows
        @Test
        void givenProcessControlRequest_whenActionIsStartThenActionIsStop_thenOkResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(200);

            assertThat(processStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/server/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("starting"));

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/server/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("up"));


            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "stop").toString())
                    .post("/server/process")
                    .then().assertThat()
                    .statusCode(200);

            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/server/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("down"));
        }

        /**
         * Output something as if the game server process would log it.
         * @param output Output to write
         */
        @SneakyThrows
        void outputAsServerProcess(String output) {
            gameOut.write((output + "\n").getBytes(StandardCharsets.UTF_8));
        }

        void mockGameProcess() throws IOException {
            gameOut = new PipedOutputStream();
            var out = new PipedOutputStream(new PipedInputStream());

            Process mockProcess = Mockito.mock(Process.class);
            when(mockProcess.onExit()).thenReturn(new CompletableFuture<>());
            when(mockProcess.destroyForcibly()).thenAnswer(invocationOnMock -> {
                mockProcess.onExit().complete(mockProcess); // Same as what would normally happen
                return mockProcess;
            });
            when(mockProcess.getInputStream()).thenReturn(new PipedInputStream(gameOut));
            when(mockProcess.getOutputStream()).thenReturn(out);

            processStartLatch = new CountDownLatch(1);
            processExitLatch = new CountDownLatch(1);
            gameStartLatch = new CountDownLatch(1);

            when(processFactory.newGameProcess(any(), any(), any())).thenAnswer(invocationOnMock -> {
                GameProcess.GameProcessHooks hooks =
                        invocationOnMock.getArgument(2, GameProcess.GameProcessHooks.class);
                Runnable onProcessStarted = hooks.getOnProcessStarted()
                        .map(runnable -> (Runnable) () -> {
                            processStartLatch.countDown();
                            runnable.run();
                        })
                        .orElse(processStartLatch::countDown);
                Runnable onProcessExited = hooks.getOnProcessExited()
                        .map(runnable -> (Runnable) () -> {
                            processExitLatch.countDown();
                            runnable.run();
                        })
                        .orElse(processExitLatch::countDown);
                Runnable onGameStarted = hooks.getOnGameStarted()
                        .map(runnable -> (Runnable) () -> {
                            gameStartLatch.countDown();
                            runnable.run();
                        })
                        .orElse(gameStartLatch::countDown);
                // Simulate how the game would exit when receiving "stop"
                Consumer<String> onInput = hooks.getOnInput()
                        .map(runnable -> (Consumer<String>) (s) -> {
                            runnable.accept(s);
                            if (s.equals("stop")) {
                                mockProcess.onExit().complete(mockProcess);
                            }
                        })
                        .orElse((s) -> {
                            if (s.equals("stop")) {
                                mockProcess.onExit().complete(mockProcess);
                            }
                        });
                GameProcess.GameProcessHooks newHooks = hooks.toBuilder()
                        .onProcessStarted(onProcessStarted)
                        .onProcessExited(onProcessExited)
                        .onGameStarted(onGameStarted)
                        .onInput(onInput)
                        .build();
                return new GameProcess(() -> mockProcess, newHooks);
            });

            when(pathValidator.validatePathExistsAndIsAFile(any())).thenReturn(true);
        }
    }
}
