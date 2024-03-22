package fi.jannetahkola.palikka.game.api.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.game.api.game.model.GameLifecycleMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameLogMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameUserReplyMessage;
import fi.jannetahkola.palikka.game.testutils.TestStompSessionHandlerAdapter;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockGameProcessTest;
import io.restassured.RestAssured;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "logging.level.fi.jannetahkola.palikka.game=debug" })
@ExtendWith(OutputCaptureExtension.class)
// New context for each test so less hassle resetting everything
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class GameControllerIT extends WireMockGameProcessTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TestTokenUtils tokens;

    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> userReplyQueue = new LinkedBlockingQueue<>();
    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> logMessageQueue = new LinkedBlockingDeque<>();
    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> lifecycleMessageQueue = new LinkedBlockingDeque<>();

    static String webSocketUrl;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        logMessageQueue.clear();
        lifecycleMessageQueue.clear();;

        webSocketUrl = "ws://localhost:" + localServerPort + "/ws";
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
        void givenConnectRequest_withUnknownUser_thenForbiddenResponse(CapturedOutput capturedOutput) {
            stubForUserNotFound(wireMockServer, 1);

            try {
                String token = tokens.generateToken(1);
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
        void givenSendMessageToGame_withoutRoles_thenDisconnected(CapturedOutput capturedOutput) {
            stubForNormalUser(wireMockServer);

            String token = tokens.generateToken(2);

            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                    .get(1000, TimeUnit.MILLISECONDS);
            StompSession.Receiptable receipt = session.send("/app/game", GameLogMessage.builder().data("haloo").build());
            assertThat(receipt.getReceiptId()).isNull();

            TestStompSessionHandlerAdapter.Frame frame = logMessageQueue.poll(1000, TimeUnit.MILLISECONDS);
            assertThat(frame).isNull();

            assertThat(capturedOutput.getAll()).contains("Failed to authorize message with authorization manager");
            assertThat(session.isConnected()).isFalse(); // should disconnect session
        }

        @SneakyThrows
        @Test
        void givenSubscribeToGame_whenNotAdmin_thenSubscriptionOk() {
            stubForNormalUser(wireMockServer);

            String token = tokens.generateToken(2);

            StompSession session = newStompClient()
                    .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                    .get(1000, TimeUnit.MILLISECONDS);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();

            StompSession.Subscription subscriptionToLogs = session.subscribe("/topic/game/logs", sessionHandler);
            assertThat(subscriptionToLogs.getSubscriptionId()).isNotNull();

            StompSession.Subscription subscriptionToLifecycle = session.subscribe("/topic/game/lifecycle", sessionHandler);
            assertThat(subscriptionToLifecycle.getSubscriptionId()).isNotNull();

            session.disconnect();
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @SneakyThrows
        @Test
        void testConnectWithHttpProtocol_thenExceptionIsThrown() {
            // SockJS is disabled so ws protocol must be used
            stubForAdminUser(wireMockServer);
            String token = tokens.generateToken(1);
            String httpWebSocketUrl = webSocketUrl.replace("ws:", "http:");
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> newStompClient()
                            .connectAsync(httpWebSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                            .get(1000, TimeUnit.MILLISECONDS))
                    .withMessage("Invalid scheme: http");
        }

        @SneakyThrows
        @Test
        void testSubscribeToGameBeforeGameIsUp() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = tokens.generateToken(1);

            HttpHeaders httpHeaders = newAuthHeader(token);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);
            session.subscribe("/topic/game/lifecycle", sessionHandler);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
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
        void testSubscribeToGameAfterGameIsUp_thenLogHistoryIsReceivedInCorrectOrder() {
            mockGameProcess();
            stubForAdminUser(wireMockServer);

            String token = tokens.generateToken(1);
            HttpHeaders httpHeaders = newAuthHeader(token);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG_QUIET);
            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);
            session.subscribe("/topic/game/lifecycle", sessionHandler);

            TestStompSessionHandlerAdapter.Frame userReplyFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(userReplyFrame).isNotNull();

            GameUserReplyMessage userReplyMessage = userReplyFrame.getPayloadAs(GameUserReplyMessage.class);
            assertThat(userReplyMessage.getTyp()).isEqualTo(GameUserReplyMessage.Type.HISTORY);

            String[] inputHistory = userReplyMessage.getData().split("\n");
            assertThat(inputHistory[0]).isEqualTo(SERVER_START_LOG_QUIET);
            assertThat(inputHistory[1]).isEqualTo(SERVER_START_LOG);

            TestStompSessionHandlerAdapter.Frame lifecycleFrame = lifecycleMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(lifecycleFrame).isNull(); // no lifecycle events since subscribed after start and no changes

            stop(session);
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void testSubscribeToGameAfterGameIsUp_andSendMessage_thenHistoryReceivedFirstAndSentMessageLast() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = tokens.generateToken(1);
            HttpHeaders httpHeaders = newAuthHeader(token);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game/logs", sessionHandler);

            TestStompSessionHandlerAdapter.Frame userReplyFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS); // Poll the history message
            assertThat(userReplyFrame).isNotNull();
            assertThat(userReplyFrame.getPayloadAs(GameUserReplyMessage.class).getTyp()).isEqualTo(GameUserReplyMessage.Type.HISTORY);

            GameLogMessage msg = GameLogMessage.builder().data("/weather clear").build();
            session.send("/app/game", msg);

            TestStompSessionHandlerAdapter.Frame logFrame = logMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(logFrame).isNotNull();
            GameLogMessage logMsg = logFrame.getPayloadAs(GameLogMessage.class);
            assertThat(logMsg).isNotNull();
            assertThat(logMsg.getData()).isEqualTo("/weather clear");

            stop(session);
        }

        @SneakyThrows
        @Test // If ran more than once we will receive the old history which is expected behaviour
        void testSubscribeToGameBeforeGameIsUp_andSendMessage_thenEmptyHistoryReceivedFirst() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);

            String token = tokens.generateToken(1);

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
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            // Poll history event
            TestStompSessionHandlerAdapter.Frame receivedFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            GameUserReplyMessage receivedMsg = receivedFrame.getPayloadAs(GameUserReplyMessage.class);
            assertThat(receivedMsg.getTyp()).isEqualTo(GameUserReplyMessage.Type.HISTORY);
            assertThat(receivedMsg.getData()).isEmpty();

            GameLogMessage msg = GameLogMessage.builder().data("/weather clear").build();
            session.send("/app/game", msg);

            // Poll input events
            TestStompSessionHandlerAdapter.Frame logFrame = logMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(logFrame).isNotNull();
            GameLogMessage logMsg = logFrame.getPayloadAs(GameLogMessage.class);
            assertThat(logMsg).isNotNull();
            assertThat(logMsg.getData()).isEqualTo(SERVER_START_LOG);

            logFrame = logMessageQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(logFrame).isNotNull();
            logMsg = logFrame.getPayloadAs(GameLogMessage.class);
            assertThat(logMsg).isNotNull();
            assertThat(logMsg.getData()).isEqualTo("/weather clear");

            stop(session);
        }

        @SneakyThrows
        @Test
        void testSubscribeToGameBeforeGameIsUp_andIsNotFirstStart_thenOldHistoryReceived() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            String token = tokens.generateToken(1);
            HttpHeaders httpHeaders = newAuthHeader(token);

            // Start
            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            // Stop
            gameProcessService.stopForcibly();
            assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            // Start again
            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

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
        void testSendInvalidMessage() {
            stubForAdminUser(wireMockServer);
            String token = tokens.generateToken(1);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(token, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);

            GameOutputMessage msg = GameOutputMessage.builder().data(" ").build();
            session.send("/app/game", msg);

            TestStompSessionHandlerAdapter.Frame userReplyFrame = userReplyQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(userReplyFrame).isNotNull();
            assertThat(userReplyFrame.getPayloadAs(GameUserReplyMessage.class).getData()).isEqualTo("Invalid message");
        }
    }

    @SneakyThrows
    void stop(StompSession session) {
        session.disconnect();
        gameProcessService.stopForcibly();
        assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
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
