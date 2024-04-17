package fi.jannetahkola.palikka.game.api.game;

import fi.jannetahkola.palikka.game.api.game.model.GameLifecycleMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameLogMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameUserReplyMessage;
import fi.jannetahkola.palikka.game.testutils.TestStompSessionHandlerAdapter;
import fi.jannetahkola.palikka.game.testutils.GameProcessIntegrationTest;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "logging.level.fi.jannetahkola.palikka.game=debug" })
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
class GameControllerIT extends GameProcessIntegrationTest {
    @SneakyThrows
    @Test
    void testRawWebSocketSessionIsStoredOnConnectAndRemovedOnDisconnect(CapturedOutput capturedOutput) {
        stubForAdminUser(wireMockServer);
        String token = testTokenGenerator.generateToken(1);
        StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
        StompSession session = newSession(token, sessionHandler);

        int sessionCountAfterConnection = sessionStore.sessionCount();
        assertThat(sessionCountAfterConnection).isNotZero();

        session.disconnect();
        await().atMost(getTestTimeout()).until(() ->
                sessionStore.sessionCount() < sessionCountAfterConnection);
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

        assertThat(lifecycleMessageQueue.poll()).isNull(); // no lifecycle events since subscribed after start and no changes

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
    @Disabled("duplicate and does not test the case correctly")
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

        int sessionCountAfterConnection = sessionStore.sessionCount();

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
        await().atMost(Duration.ofSeconds(1)).until(() ->
                sessionStore.sessionCount() < sessionCountAfterConnection);
    }
}
