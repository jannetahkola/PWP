package fi.jannetahkola.palikka.game.api.game;

import fi.jannetahkola.palikka.core.auth.jwt.JwtService;
import fi.jannetahkola.palikka.game.api.game.model.GameOutputMessage;
import fi.jannetahkola.palikka.game.api.game.model.GameUserReplyMessage;
import fi.jannetahkola.palikka.game.testutils.GameProcessIntegrationTest;
import fi.jannetahkola.palikka.game.testutils.TestStompSessionHandlerAdapter;
import fi.jannetahkola.palikka.game.websocket.GameMessageValidator;
import fi.jannetahkola.palikka.game.websocket.SessionStore;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static fi.jannetahkola.palikka.game.testutils.Stubs.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "logging.level.fi.jannetahkola.palikka.game=debug" })
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
class GameControllerSecurityIT extends GameProcessIntegrationTest {
    @SpyBean
    GameMessageValidator gameMessageValidator;

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

        int sessionCountAfterConnection = sessionStore.sessionCount();

        GameOutputMessage message = GameOutputMessage.builder().data("haloo").build();
        session.send("/app/game", message);

        assertThat(session.isConnected()).isTrue();
        assertThat(capturedOutput.getAll()).doesNotContain("Failed to authorize message with authorization manager");

        session.disconnect();
        await().atMost(getTestTimeout()).until(() ->
                sessionStore.sessionCount() < sessionCountAfterConnection);
    }

    @SneakyThrows
    @Test
    void givenSendMessageToGame_whenNoRoleToSendCommands_thenDisconnected(CapturedOutput capturedOutput) {
        stubForViewerUser(wireMockServer);

        String token = testTokenGenerator.generateToken(USER_ID_VIEWER);

        StompSession session = newStompClient()
                .connectAsync(webSocketUrl + newAuthQueryParam(token), newStompSessionHandler())
                .get(1000, TimeUnit.MILLISECONDS);
        int sessionCountAfterConnection = sessionStore.sessionCount();

        StompSession.Receiptable receipt = session.send(
                "/app/game",
                GameOutputMessage.builder().data("/weather clear").build());
        assertThat(receipt.getReceiptId()).isNull();

        // should disconnect session
        await().atMost(getTestTimeout()).until(() -> !session.isConnected());
        await().atMost(getTestTimeout()).until(() ->
                sessionStore.sessionCount() < sessionCountAfterConnection);

        assertThat(logMessageQueue.poll()).isNull();
        await().atMost(getTestTimeout()).until(() ->
                capturedOutput.getAll().contains("Failed to authorize message with authorization manager"));
    }

    @SneakyThrows
    @Test
    void givenSendMessageToGame_whenNoAuthorityForCommand_thenErrorReplyReceived() {
        mockGameProcess();
        stubForAdminUser(wireMockServer);

        // Process needs to be up for commands the get processed - mock it for test performance
        doNothing().when(gameMessageValidator).validateGameProcessIsUp();

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

        session.disconnect();
        await().atMost(getTestTimeout()).until(() -> !session.isConnected());
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

        int sessionCountAfterConnection = sessionStore.sessionCount();

        await().atMost(Duration.ofSeconds(1)).until(() -> jwtService.isExpired(token));

        StompSession.Receiptable receipt = session.send(
                "/app/game",
                GameOutputMessage.builder().data("haloo").build());
        assertThat(receipt).isNotNull();

        assertThat(userReplyQueue.poll()).isNull();

        await().atMost(getTestTimeout()).until(() -> !session.isConnected());
        await().atMost(getTestTimeout()).until(() -> sessionStore.sessionCount() < sessionCountAfterConnection);
        assertThat(capturedOutput.getAll()).contains("Access revoked from STOMP session with an expired JWT, principal=1");
    }

    @SneakyThrows
    @Test
    void givenUserIsConnected_whenTokenExpires_andExpiredSessionIsEvicted_thenDisconnected(@Autowired SessionStore sessionStore,
                                                                                           @Autowired JwtService jwtService,
                                                                                           CapturedOutput capturedOutput) {
        stubForAdminUser(wireMockServer);
        String token = testTokenGenerator.generateTokenExpiringIn(1, Duration.ofSeconds(1));
        StompSession session = newSession(token, newStompSessionHandler());

        await().atMost(getTestTimeout()).until(() -> jwtService.isExpired(token));

        // Evict manually for test performance
        sessionStore.evictExpiredSessions();
        await().atMost(getTestTimeout()).until(() -> !session.isConnected()); // await for disconnect
        await().atMost(getTestTimeout()).until(() ->
                capturedOutput.getAll().contains("Closed session with expired JWT"));
        assertThat(sessionStore.sessionCount()).isZero();
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
        await().atMost(getTestTimeout()).until(() -> sessionStore.sessionCount() == 0);
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
