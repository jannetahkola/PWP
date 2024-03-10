package fi.jannetahkola.palikka.game.api.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.game.api.game.model.GameMessage;
import fi.jannetahkola.palikka.game.testutils.TestStompSessionHandlerAdapter;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockGameProcessTest;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
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
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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

    final BlockingQueue<TestStompSessionHandlerAdapter.Frame> responseQueue = new LinkedBlockingDeque<>();

    static String webSocketUri;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        webSocketUri = "http://localhost:" + localServerPort + "/ws";
        RestAssured.port = localServerPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        responseQueue.clear();
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
                newStompClient().connectAsync(webSocketUri, newStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
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
                WebSocketHttpHeaders headers = newWebSocketAuthHeaders(1);
                newStompClient().connectAsync(webSocketUri, headers, newStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
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

            WebSocketHttpHeaders headers = newWebSocketAuthHeaders(2);
            StompSession session = newStompClient().connectAsync(webSocketUri, headers, newStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
            StompSession.Receiptable receipt = session.send("/app/game", GameMessage.builder().data("haloo").build());
            assertThat(receipt.getReceiptId()).isNull();

            TestStompSessionHandlerAdapter.Frame frame = responseQueue.poll(1000, TimeUnit.MILLISECONDS);
            assertThat(frame).isNull();

            assertThat(capturedOutput.getAll()).contains("Failed to authorize message with authorization manager");
            assertThat(session.isConnected()).isFalse(); // should disconnect session
        }

        @SneakyThrows
        @Test
        void givenSubscribeToGame_whenNotAdmin_thenSubscriptionOk() {
            stubForNormalUser(wireMockServer);

            WebSocketHttpHeaders headers = newWebSocketAuthHeaders(2);
            StompSession session = newStompClient().connectAsync(webSocketUri, headers, newStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);

            StompSession.Subscription subscription = session.subscribe("/topic/game", newStompSessionHandler());
            assertThat(subscription.getSubscriptionId()).isNotNull();

            session.disconnect();
        }
    }

    @Nested
    class ResourceFunctionalityIT {
        @SneakyThrows
        @Test
        void testSubscribeToGameBeforeGameIsUp() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            HttpHeaders httpHeaders = newAuthHeader(1);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(newWebSocketAuthHeaders(httpHeaders), sessionHandler);
            session.subscribe("/topic/game", sessionHandler);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            TestStompSessionHandlerAdapter.Frame receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            assertThat(receivedFrame.getHeaders().getDestination()).isEqualTo("/topic/game");

            GameMessage receivedMessage = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMessage).isNotNull();
            assertThat(receivedMessage.getSrc()).isEqualTo(GameMessage.Source.GAME);
            assertThat(receivedMessage.getData()).contains("Done (13.324s)!");

            stop(session);
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void testSubscribeToGameAfterGameIsUp_thenLogHistoryIsReceivedInCorrectOrder() {
            mockGameProcess();
            stubForAdminUser(wireMockServer);
            HttpHeaders httpHeaders = newAuthHeader(1);

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
            StompSession session = newSession(newWebSocketAuthHeaders(httpHeaders), sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game", sessionHandler);

            TestStompSessionHandlerAdapter.Frame receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            GameMessage receivedMsg = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMsg.getSrc()).isEqualTo(GameMessage.Source.SERVER);
            assertThat(receivedMsg.getTyp()).isEqualTo(GameMessage.Type.HISTORY);

            String[] inputHistory = receivedMsg.getData().split("\n");
            assertThat(inputHistory[0]).isEqualTo(SERVER_START_LOG_QUIET);
            assertThat(inputHistory[1]).isEqualTo(SERVER_START_LOG);

            stop(session);
        }

        @SneakyThrows
        @RepeatedTest(value = 3, failureThreshold = 1)
        void testSubscribeToGameAfterGameIsUp_andSendMessage_thenHistoryReceivedFirstAndSentMessageLast() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            HttpHeaders httpHeaders = newAuthHeader(1);
            WebSocketHttpHeaders webSocketHttpHeaders = newWebSocketAuthHeaders(httpHeaders);

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
            StompSession session = newSession(webSocketHttpHeaders, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game", sessionHandler);

            TestStompSessionHandlerAdapter.Frame receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS); // Poll the history message
            assertThat(receivedFrame).isNotNull();
            assertThat(receivedFrame.getPayloadAs(GameMessage.class).getTyp()).isEqualTo(GameMessage.Type.HISTORY);

            GameMessage msg = GameMessage.builder().data("/weather clear").build();
            session.send("/app/game", msg);

            receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            GameMessage receivedMsg = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMsg).isNotNull();
            assertThat(receivedMsg.getSrc()).isEqualTo(GameMessage.Source.GAME);
            assertThat(receivedMsg.getData()).isEqualTo("/weather clear");

            stop(session);
        }

        @SneakyThrows
        @Test // If ran more than once we will receive the old history which is expected behaviour
        void testSubscribeToGameBeforeGameIsUp_andSendMessage_thenEmptyHistoryReceivedFirst() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            HttpHeaders httpHeaders = newAuthHeader(1);
            WebSocketHttpHeaders webSocketHttpHeaders = newWebSocketAuthHeaders(httpHeaders);

            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(webSocketHttpHeaders, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game", sessionHandler);

            given()
                    .headers(httpHeaders)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new JSONObject().put("action", "start").toString())
                    .post("/game-api/game/process")
                    .then().assertThat()
                    .statusCode(200);

            outputAsServerProcess(SERVER_START_LOG);
            assertThat(gameStartLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();

            TestStompSessionHandlerAdapter.Frame receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS); // Poll the history message
            assertThat(receivedFrame).isNotNull();
            GameMessage receivedMsg = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMsg.getTyp()).isEqualTo(GameMessage.Type.HISTORY);
            assertThat(receivedMsg.getData()).isEmpty();

            GameMessage msg = GameMessage.builder().data("/weather clear").build();
            session.send("/app/game", msg);

            receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            receivedMsg = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMsg).isNotNull();
            assertThat(receivedMsg.getSrc()).isEqualTo(GameMessage.Source.GAME);
            assertThat(receivedMsg.getData()).isEqualTo(SERVER_START_LOG);

            receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            receivedMsg = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMsg).isNotNull();
            assertThat(receivedMsg.getSrc()).isEqualTo(GameMessage.Source.GAME);
            assertThat(receivedMsg.getData()).isEqualTo("/weather clear");

            stop(session);
        }

        @SneakyThrows
        @Test
        void testSubscribeToGameBeforeGameIsUp_andIsNotFirstStart_thenOldHistoryReceived() {
            mockGameProcess();

            stubForAdminUser(wireMockServer);
            HttpHeaders httpHeaders = newAuthHeader(1);

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

            WebSocketHttpHeaders webSocketHttpHeaders = newWebSocketAuthHeaders(httpHeaders);
            StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
            StompSession session = newSession(webSocketHttpHeaders, sessionHandler);
            session.subscribe("/user/queue/reply", sessionHandler);
            session.subscribe("/topic/game", sessionHandler);

            TestStompSessionHandlerAdapter.Frame receivedFrame = responseQueue.poll(testTimeoutMillis, TimeUnit.MILLISECONDS);
            assertThat(receivedFrame).isNotNull();
            GameMessage receivedMsg = receivedFrame.getPayloadAs(GameMessage.class);
            assertThat(receivedMsg.getTyp()).isEqualTo(GameMessage.Type.HISTORY);
            String[] history = receivedMsg.getData().split("\n");
            assertThat(history[0]).isEqualTo(SERVER_START_LOG);
            assertThat(history[1]).isEqualTo("stop");

            stop(session);
        }
    }

    @SneakyThrows
    void stop(StompSession session) {
        session.disconnect();
        gameProcessService.stopForcibly();
        assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
    }

    @SneakyThrows
    StompSession newSession(WebSocketHttpHeaders headers, StompSessionHandlerAdapter sessionHandler) {
        return newStompClient().connectAsync(webSocketUri, headers, sessionHandler).get(1000, TimeUnit.MILLISECONDS);
    }

    WebSocketHttpHeaders newWebSocketAuthHeaders(int userId) {
        return new WebSocketHttpHeaders(newAuthHeader(userId));
    }

    WebSocketHttpHeaders newWebSocketAuthHeaders(HttpHeaders headers) {
        return new WebSocketHttpHeaders(headers);
    }

    HttpHeaders newAuthHeader(int userId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(tokens.generateToken(userId));
        return httpHeaders;
    }

    WebSocketStompClient newStompClient() {
        List<Transport> transportList = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient client = new WebSocketStompClient(new SockJsClient(transportList));
        List<MessageConverter> converters = new ArrayList<>();
        converters.add(new MappingJackson2MessageConverter(objectMapper));
        converters.add(new StringMessageConverter());
        client.setMessageConverter(new CompositeMessageConverter(converters));
        return client;
    }

    StompSessionHandlerAdapter newStompSessionHandler() {
        return new TestStompSessionHandlerAdapter(responseQueue);
    }
}
