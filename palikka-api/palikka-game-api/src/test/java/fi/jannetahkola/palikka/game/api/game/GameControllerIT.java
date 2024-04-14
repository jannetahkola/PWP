package fi.jannetahkola.palikka.game.api.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.game.api.game.model.GameLifecycleMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameLogMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameUserReplyMessage;
import fi.jannetahkola.palikka.game.testutils.TestStompSessionHandlerAdapter;
import fi.jannetahkola.palikka.game.testutils.GameProcessIntegrationTest;
import fi.jannetahkola.palikka.game.websocket.SessionStore;
import io.restassured.RestAssured;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "logging.level.fi.jannetahkola.palikka.game=debug" })
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
class GameControllerIT extends GameProcessIntegrationTest {

    @Autowired
    ObjectMapper objectMapper;

    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> userReplyQueue = new LinkedBlockingQueue<>();
    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> logMessageQueue = new LinkedBlockingDeque<>();
    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> lifecycleMessageQueue = new LinkedBlockingDeque<>();

    static String webSocketUrl;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.basePath = "/game-api";
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        wireMockServer.resetAll();

        userReplyQueue.clear();
        logMessageQueue.clear();
        lifecycleMessageQueue.clear();;

        webSocketUrl = "ws://localhost:" + localServerPort + "/game-api/ws";
    }

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("palikka.integration.users-api.base-uri", () -> wireMockServer.baseUrl());
    }

    @Nested
    class ResourceSecurityIT {
        @SneakyThrows
        @Test
        void givenConnectRequest_withoutToken_thenForbiddenResponse() {
            try {
                newStompClient()
                        .connectAsync(webSocketUrl, newStompSessionHandler())
                        .get(1000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                assertTrue(e.getMessage().contains("403"));
                return;
            }
            fail("Expected exception but none was thrown");
        }

        @SneakyThrows
        @Test
        void givenConnectRequest_withExpiredToken_thenForbiddenResponse(CapturedOutput capturedOutput) {
            stubForUserNotFound(wireMockServer, USER_ID_ADMIN);

            try {
                String token = testTokenGenerator.generateExpiredToken(USER_ID_ADMIN);
                newStompClient()
                        .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                        .get(1000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                assertTrue(e.getMessage().contains("403"));
                assertThat(capturedOutput.getAll()).contains("Expired JWT");
                return;
            }
            fail("Expected exception but none was thrown");
        }

        @SneakyThrows
        @Test
        void givenConnectRequest_withUnknownUser_thenForbiddenResponse(CapturedOutput capturedOutput) {
            stubForUserNotFound(wireMockServer, 1);

            try {
                String token = testTokenGenerator.generateToken(1);
                newStompClient()
                        .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                        .get(1000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                assertTrue(e.getMessage().contains("403"));
                assertThat(capturedOutput.getAll()).contains("User with id '1' not found");
                return;
            }
            fail("Expected exception but none was thrown");
        }

        @SneakyThrows
        @Test
        void givenConnectRequest_withSystemToken_thenForbiddenResponse() {
            try {
                String token = testTokenGenerator.generateSystemToken();
                newStompClient()
                        .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                        .get(1000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                assertTrue(e.getMessage().contains("403"));
                return;
            }
            fail("Expected exception but none was thrown");
        }

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("sendMessageToGameAllowedUserArgs")
        void givenSendMessageToGame_withAllowedRole_thenMessageOk(Integer user, CapturedOutput capturedOutput) {
            stubForAdminUser(wireMockServer);
            stubForNormalUser(wireMockServer);
            stubForViewerUser(wireMockServer);

            String token = testTokenGenerator.generateToken(user);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), sessionHandler)
                    .get(1000, TimeUnit.MILLISECONDS);
            session.subscribe("/user/queue/reply", sessionHandler);

            GameOutputMessage message = GameOutputMessage.builder().data("haloo").build();
            session.send("/app/game", message);

            assertThat(session.isConnected()).isTrue();
            assertThat(capturedOutput.getAll()).doesNotContain("Failed to authorize message with authorization manager");

            session.disconnect();
            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);
        }

        @SneakyThrows
        @Test
        void givenSendMessageToGame_whenNoRoleToSendCommands_thenDisconnected(CapturedOutput capturedOutput) {
            stubForViewerUser(wireMockServer);

            String token = testTokenGenerator.generateToken(USER_ID_VIEWER);

            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                    .get(1000, TimeUnit.MILLISECONDS);

            StompSession.Receiptable receipt = session.send(
                    "/app/game",
                    GameOutputMessage.builder().data("/weather clear").build()
            );
            assertThat(receipt.getReceiptId()).isNull();

            TestStompSessionHandlerAdapter.Frame frame = logMessageQueue.poll(1000, TimeUnit.MILLISECONDS);
            assertThat(frame).isNull();

            assertThat(capturedOutput.getAll()).contains("Failed to authorize message with authorization manager");

            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);
            assertThat(session.isConnected()).isFalse(); // should disconnect session
        }

        @SneakyThrows
        @Test
        void givenSendMessageToGame_whenNoAuthorityForCommand_thenErrorReplyReceived() {
            mockGameProcess();
            stubForAdminUser(wireMockServer);

            String adminToken = testTokenGenerator.generateToken(USER_ID_ADMIN);
            HttpHeaders httpHeaders = newAuthHeader(adminToken);

            // Process needs to be up for commands the get processed - only admin can start
            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            stubForNormalUser(wireMockServer);
            String token = testTokenGenerator.generateToken(USER_ID_USER);
            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), sessionHandler)
                    .get(1000, TimeUnit.MILLISECONDS);
            session.subscribe("/user/queue/reply", sessionHandler);

            StompSession.Receiptable receipt = session.send(
                    "/app/game",
                    GameOutputMessage.builder().data("haloo").build());
            assertThat(receipt).isNotNull();

            TestStompSessionHandlerAdapter.Frame userReplyFrame =
                    userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(userReplyFrame).isNotNull();
            GameUserReplyMessage replyMessage = userReplyFrame.getPayloadAs(GameUserReplyMessage.class);
            assertThat(replyMessage).isNotNull();
            assertThat(replyMessage.getTyp()).isEqualTo(GameUserReplyMessage.Type.ERROR);
            assertThat(replyMessage.getData()).isEqualTo("Access denied to command='haloo'");

            assertThat(session.isConnected()).isTrue();

            stop(session);
        }

        @SneakyThrows
        @Test
        void givenUserIsConnected_whenTokenExpires_andUserSendsMessageToGame_thenDisconnected(@Autowired JwtService jwtService,
                                                                                              CapturedOutput capturedOutput) {
            stubForAdminUser(wireMockServer);

            String token = testTokenGenerator.generateTokenExpiringIn(1, Duration.ofSeconds(1));

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), sessionHandler)
                    .get(1000, TimeUnit.MILLISECONDS);
            session.subscribe("/user/queue/reply", sessionHandler);

            await().atMost(Duration.ofSeconds(1)).until(() -> jwtService.isExpired(token));

            StompSession.Receiptable receipt = session.send(
                    "/app/game",
                    GameOutputMessage.builder().data("haloo").build());
            assertThat(receipt).isNotNull();

            TestStompSessionHandlerAdapter.Frame userReplyFrame =
                    userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(userReplyFrame).isNull();
            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);
            assertThat(session.isConnected()).isFalse();
            assertThat(capturedOutput.getAll()).contains("Access revoked from STOMP session with an expired JWT, principal=1");
        }

        @SneakyThrows
        @Test
        void givenUserIsConnected_whenTokenExpires_andExpiredSessionIsEvicted_thenDisconnected(@Autowired SessionStore sessionStore,
                                                                                               @Autowired JwtService jwtService,
                                                                                               CapturedOutput capturedOutput) {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateTokenExpiringIn(1, Duration.ofSeconds(1));

            HttpHeaders httpHeaders = newAuthHeader(token);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);
            session.subscribe("/topic/game/lifecycle", sessionHandler);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            await().atMost(Duration.ofSeconds(1)).until(() -> jwtService.isExpired(token));

            sessionStore.evictExpiredSessions();

            await().atMost(Duration.ofSeconds(1)); // await for disconnect

            outputAsServerProcess(SERVER_START_LOG);

            assertThat(session.isConnected()).isFalse();
            assertThat(capturedOutput.getAll()).contains("Closed session with expired JWT");

            // Eviction happened after the start command was invoked -> there should be a "starting" lifecycle message
            TestStompSessionHandlerAdapter.Frame lifecycleFrame =
                    lifecycleMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(lifecycleFrame).isNotNull();

            // Eviction happened before the executable logged that it's ready for connection -> no more lifecycle messages
            lifecycleFrame = lifecycleMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(lifecycleFrame).isNull();

            assertThat(sessionStore.sessionCount()).isZero();

            stop(session);
        }

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("subscriptionToGameAllowedUserArgs")
        void givenSubscribeToGame_withAllowedRole_thenSubscriptionOk(Integer user) {
            stubForAdminUser(wireMockServer);
            stubForNormalUser(wireMockServer);
            stubForViewerUser(wireMockServer);

            String token = testTokenGenerator.generateToken(user);

            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                    .get(1000, TimeUnit.MILLISECONDS);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();

            StompSession.Subscription subscriptionToLogs = session.subscribe("/topic/game/logs", sessionHandler);
            assertThat(subscriptionToLogs.getSubscriptionId()).isNotNull();

            StompSession.Subscription subscriptionToLifecycle = session.subscribe("/topic/game/lifecycle", sessionHandler);
            assertThat(subscriptionToLifecycle.getSubscriptionId()).isNotNull();

            session.disconnect();
            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);
        }

        static Stream<Arguments> sendMessageToGameAllowedUserArgs() {
            return Stream.of(
                    Arguments.of(Named.of("ADMIN", USER_ID_ADMIN)),
                    Arguments.of(Named.of("USER", USER_ID_USER))
            );
        }

        static Stream<Arguments> subscriptionToGameAllowedUserArgs() {
            return Stream.of(
                    Arguments.of(Named.of("ADMIN", USER_ID_ADMIN)),
                    Arguments.of(Named.of("USER", USER_ID_USER)),
                    Arguments.of(Named.of("VIEWER", USER_ID_VIEWER))
            );
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @SneakyThrows
        @Test
        void testRawWebSocketSessionIsStoredOnConnectAndRemovedOnDisconnect(CapturedOutput capturedOutput) {
            assertThat(sessionStore.sessionCount()).isZero();

            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateToken(1);
            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);

            assertThat(sessionStore.sessionCount()).isEqualTo(1);

            session.disconnect();

            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);

            assertThat(capturedOutput.getAll()).contains("Connection closed in WS session");
            assertThat(capturedOutput.getAll()).contains("Session removed with id");
        }

        @SneakyThrows
        @Test
        void testConnectWithHttpProtocol_thenExceptionIsThrown() {
            // SockJS is disabled so ws protocol must be used
            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateToken(1);
            String httpWebSocketUrl = webSocketUrl.replace("ws:", "http:");
            WebSocketStompClient client = newStompClient();
            String authParam = newAuthQueryParam(token);
            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> client.connectAsync(httpWebSocketUrl + authParam, sessionHandler))
                    .withMessage("Invalid scheme: http");
        }

        @SneakyThrows
        @Test
        void testSubscribeToGameBeforeGameIsUp() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateToken(1);

            HttpHeaders httpHeaders = newAuthHeader(token);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);
            session.subscribe("/topic/game/lifecycle", sessionHandler);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            // lifecycle
            TestStompSessionHandlerAdapter.Frame lifecycleFrame = lifecycleMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(lifecycleFrame).isNotNull();
            assertThat(lifecycleFrame.getPayloadAs(GameLifecycleMessage.class).getStatus()).isEqualTo("starting");

            lifecycleFrame = lifecycleMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(lifecycleFrame).isNotNull();
            assertThat(lifecycleFrame.getPayloadAs(GameLifecycleMessage.class).getStatus()).isEqualTo("up");

            // logs
            TestStompSessionHandlerAdapter.Frame logFrame = logMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(logFrame).isNotNull();
            assertThat(logFrame.getPayloadAs(GameLogMessage.class).getData()).contains("Done (13.324s)!");

            stop(session);
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void testSubscribeToGameAfterGameIsUp_andSendMessage_thenHistoryReceivedFirstInCorrectOrder_andSentMessageLast() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateToken(1);
            HttpHeaders httpHeaders = newAuthHeader(token);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG_QUIET);
            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);

            TestStompSessionHandlerAdapter.Frame userReplyFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS); // Poll the history message
            assertThat(userReplyFrame).isNotNull();

            GameUserReplyMessage userReplyMessage = userReplyFrame.getPayloadAs(GameUserReplyMessage.class);
            assertThat(userReplyFrame.getPayloadAs(GameUserReplyMessage.class).getTyp()).isEqualTo(GameUserReplyMessage.Type.HISTORY);

            String[] inputHistory = userReplyMessage.getData().split("\n");
            // Due to other tests there may be other history in the queue so check the order but ignore which index the history starts from
            int i = Arrays.asList(inputHistory).indexOf(SERVER_START_LOG_QUIET);
            assertThat(i).isNotNegative();
            assertThat(inputHistory[i + 1]).isEqualTo(SERVER_START_LOG);

            TestStompSessionHandlerAdapter.Frame lifecycleFrame = lifecycleMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(lifecycleFrame).isNull(); // no lifecycle events since subscribed after start and no changes

            GameOutputMessage msg = GameOutputMessage.builder().data("/weather clear").build();
            session.send("/app/game", msg);

            TestStompSessionHandlerAdapter.Frame logFrame = logMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(logFrame).isNotNull();
            GameLogMessage logMsg = logFrame.getPayloadAs(GameLogMessage.class);
            assertThat(logMsg).isNotNull();
            assertThat(logMsg.getData()).isEqualTo("/weather clear");

            stop(session);
        }

        @SneakyThrows
        @Test
        void testSubscribeToGameBeforeGameIsUp_andIsNotFirstStart_thenOldHistoryReceived() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateToken(1);
            HttpHeaders httpHeaders = newAuthHeader(token);

            // Start
            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            // Stop
            gameProcessService.stopForcibly();
            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            mockGameProcess();

            // Start again
            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
            assertThat(gameProcessService.getGameProcessStatus()).isEqualTo("up");

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);

            TestStompSessionHandlerAdapter.Frame userReplyFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(userReplyFrame).isNotNull();
            GameUserReplyMessage receivedMsg = userReplyFrame.getPayloadAs(GameUserReplyMessage.class);
            assertThat(receivedMsg.getTyp()).isEqualTo(GameUserReplyMessage.Type.HISTORY);
            String[] history = receivedMsg.getData().split("\n");
            assertThat(history[0]).isEqualTo(SERVER_START_LOG);
            assertThat(history[1]).isEqualTo("stop");

            stop(session);
        }

        @SneakyThrows
        @Test
        void testSendMessageToGame_whenMessageInvalid_thenErrorMessageReceived() {
            stubForAdminUser(wireMockServer);
            String token = testTokenGenerator.generateToken(1);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);

            Runnable assertReply = () -> {
                TestStompSessionHandlerAdapter.Frame userReplyFrame;
                try {
                    userReplyFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                assertThat(userReplyFrame).isNotNull();
                assertThat(userReplyFrame.getPayloadAs(GameUserReplyMessage.class).getData()).isEqualTo("Invalid message");
            };

            GameOutputMessage msg = GameOutputMessage.builder().data("").build();
            session.send("/app/game", msg);
            assertReply.run();

            msg = GameOutputMessage.builder().data(" ").build();
            session.send("/app/game", msg);
            assertReply.run();

            msg = GameOutputMessage.builder().data(" /weather").build();
            session.send("/app/game", msg);
            assertReply.run();

            msg = GameOutputMessage.builder().data("/ weather").build();
            session.send("/app/game", msg);
            assertReply.run();

            msg = GameOutputMessage.builder().data("@weather").build();
            session.send("/app/game", msg);
            assertReply.run();

            session.disconnect();
            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);
        }
    }

    @SneakyThrows
    StompSession newSession(@NonNull String token, @NonNull StompSessionHandlerAdapter sessionHandler) {
        return newStompClient()
                .connectAsync(webSocketUrl + newAuthQueryParam(token), sessionHandler)
                .get(1000, TimeUnit.MILLISECONDS);
    }

    HttpHeaders newAuthHeader(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        return httpHeaders;
    }

    String newAuthQueryParam(@NonNull String token) {
        return "?token=" + token;
    }

    WebSocketStompClient newStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient()); // new SockJsClient(transportList)
        List<MessageConverter> converters = new ArrayList<>();
        converters.add(new MappingJackson2MessageConverter(objectMapper));
        converters.add(new StringMessageConverter());
        client.setMessageConverter(new CompositeMessageConverter(converters));
        return client;
    }

    /**
     * SockJS support is disabled but this is here for reference. Connections with SockJSClient
     * will fail on missing /ws/info endpoint.
     * @return {@link WebSocketStompClient}
     */
    @SuppressWarnings("unused")
    WebSocketStompClient newStompWithSockJsClient() {
        List<Transport> transportList = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient client = new WebSocketStompClient(new SockJsClient(transportList));
        List<MessageConverter> converters = new ArrayList<>();
        converters.add(new MappingJackson2MessageConverter(objectMapper));
        converters.add(new StringMessageConverter());
        client.setMessageConverter(new CompositeMessageConverter(converters));
        return client;
    }

    StompSessionHandlerAdapter newStompSessionHandler() {
        return new TestStompSessionHandlerAdapter(userReplyQueue, logMessageQueue, lifecycleMessageQueue);
    }
}
