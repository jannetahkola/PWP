package fi.jannetahkola.palikka.game.api.process;

import fi.jannetahkola.palikka.game.testutils.GameProcessIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the asynchronous lifecycle control actions. Depending on the host, test timeout may need to be increased.
 * Remember to stop the game process before each test!
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameProcessControllerIT extends GameProcessIntegrationTest {
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
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, "Bearer " + testTokenGenerator.generateToken(2));
        }

        @Test
        void givenGetProcessStatusRequest_whenNoToken_thenForbiddenResponse() {
            given()
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(403);
        }

        @ParameterizedTest
        @MethodSource("allUsers")
        void givenGetProcessStatusRequest_whenAnyRole_thenOkResponse(Integer user) {
            stubForAdminUser(wireMockServer);
            stubForNormalUser(wireMockServer);
            stubForViewerUser(wireMockServer);
            given()
                    .header(HttpHeaders.AUTHORIZATION, testTokenGenerator.generateBearerToken(user))
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(200);
        }

        static Stream<Arguments> allUsers() {
            return Stream.of(
                    Arguments.of(Named.of("ADMIN", 1)),
                    Arguments.of(Named.of("USER", 2)),
                    Arguments.of(Named.of("VIEWER", 3))
            );
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
            authorizationHeader = new Header(HttpHeaders.AUTHORIZATION, testTokenGenerator.generateBearerToken(1));
        }

        @Test
        void givenGetProcessStatusRequest_thenOkResponse() {
            given()
                    .header(authorizationHeader)
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(200)
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
                    .body("detail", containsString("No enum constant"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", null).toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(400)
                    .body("detail", equalTo("action: must not be null"))
                    .header(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE));
        }

        @SneakyThrows
        @RepeatedTest(value = 2, failureThreshold = 1)
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

            CompletableFuture.allOf(startFutures.toArray(
                    new CompletableFuture[0])).get(testTimeoutMillis, TimeUnit.MILLISECONDS);

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

            stop();
        }

        @SneakyThrows
        @RepeatedTest(value = 2, failureThreshold = 1)
        void givenProcessControlRequest_whenActionIsStop_andAlreadyDown_thenBadRequestResponse() {
            mockGameProcess();

            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .thenReturn();

            assertThat(processStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            // Try to stop while still starting -> fail
            given()
                    .header(authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "stop").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(400);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            given()
                    .header(authorizationHeader)
                    .get("/game/process")
                    .then().assertThat()
                    .statusCode(200)
                    .body("status", equalTo("up"));

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

            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            stop(); // should be unnecessary, stopped already
        }
    }
}
