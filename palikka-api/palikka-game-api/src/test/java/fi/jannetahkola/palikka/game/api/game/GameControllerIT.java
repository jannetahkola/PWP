package fi.jannetahkola.palikka.game.api.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.game.testutils.TestStompSessionHandlerAdapter;
import fi.jannetahkola.palikka.game.testutils.TestTokenUtils;
import fi.jannetahkola.palikka.game.testutils.WireMockTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
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

import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForAdminUser;
import static fi.jannetahkola.palikka.game.testutils.Stubs.stubForUserNotFound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GameControllerIT extends WireMockTest {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TestTokenUtils tokens;

    final BlockingQueue<Object> responseQueue = new LinkedBlockingDeque<>();

    static String webSocketUri;

    @BeforeEach
    void beforeEach(@LocalServerPort int localServerPort) {
        webSocketUri = "http://localhost:" + localServerPort + "/ws";
    }

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("palikka.integration.users-api.base-uri", () -> wireMockServer.baseUrl());
    }

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
    void givenConnectRequest_withUnknownUser_thenForbiddenResponse() {
        stubForUserNotFound(wireMockServer, 1);

        try {
            WebSocketHttpHeaders headers = newAuthHeaders(1);
            newStompClient().connectAsync(webSocketUri, headers, newStompSessionHandler()).get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            assertTrue(e.getMessage().contains("403"));
            return;
        }
        fail("Expected exception but none was thrown");
    }

    @SneakyThrows
    @Test
    void testEcho2() {
        stubForAdminUser(wireMockServer);

        StompSessionHandlerAdapter sessionHandler = newStompSessionHandler();
        StompSession session = newSession(1, sessionHandler);
        assertThat(session).isNotNull();

        session.subscribe("/user/queue/reply", sessionHandler);
        session.send("/app/echo", "test");

        assertThat((String) responseQueue.poll(1000, TimeUnit.MILLISECONDS)).isEqualTo("test");

        session.disconnect();
    }

    @SneakyThrows
    StompSession newSession(int userId, StompSessionHandlerAdapter sessionHandler) {
        return newStompClient().connectAsync(webSocketUri, newAuthHeaders(userId), sessionHandler).get(1000, TimeUnit.MILLISECONDS);
    }

    WebSocketHttpHeaders newAuthHeaders(int userId) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.generateToken(userId));
        return new WebSocketHttpHeaders(httpHeaders);
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
