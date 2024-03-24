package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockGameProcessTest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForNormalUser;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the asynchronous lifecycle control actions. Depending on the host, test timeout may need to be increased.
 * Remember to stop the game process before each test!
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class GameProcessControllerIT extends WireMockGameProcessTest {
    @Autowired
    TestTokenUtils tokens;

    Header authorizationHeader;

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
        @BeforeEach
        void beforeEach() {
            stubForNormalUser(wireMockServer);
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(2));
        }

        @Test
        void givenGetProcessStatusRequest_whenNoTokenOrRoles_thenForbiddenResponse() {
            given()
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(403);

            given()
                    .header(authorizationHeader)
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(403);
        }

        @SneakyThrows
        @Test
        void givenProcessControlRequest_whenNoTokenOrRoles_thenForbiddenResponse() {
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(403);

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(403);
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @BeforeEach
        void beforeEach() {
            stubForAdminUser(wireMockServer);
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(1));
        }

        @Test
        void givenGetProcessStatusRequest_thenOkResponse() {
            given()
                    .header(authorizationHeader)
                    .get("/game/process")
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
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(400)
                    .body("message", equalTo("Invalid request"));

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", null).toString())
                    .post("/game/process")
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
                                .post("/game/process")
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
                    .post("/game/process");

            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            stop();
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void givenProcessControlRequest_whenActionIsStop_andAlreadyDown_thenBadRequestResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
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
                                .post("/game/process")
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

            stop();
        }

        @SneakyThrows
        @Test
        void givenProcessControlRequest_whenActionIsStop_andProcessStillStarting_thenBadRequestResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .thenReturn();

            assertThat(processStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "stop").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(400);

            stop();
        }

        @SneakyThrows
        @Test
        void givenProcessControlRequest_whenActionIsStartThenActionIsStop_thenOkResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            assertThat(processStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("starting"));

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("up"));

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "stop").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("down"));

            stop();
        }
    }
}
