package fi.jannetahkola.palikka.game.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.jannetahkola.palikka.game.process.GameProcess;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import fi.jannetahkola.palikka.game.service.factory.ProcessFactory;
import fi.jannetahkola.palikka.game.service.validator.PathValidator;
import fi.jannetahkola.palikka.game.websocket.SessionStore;
import io.restassured.RestAssured;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public abstract class GameProcessIntegrationTest extends IntegrationTest {
    protected static final String SERVER_START_LOG_QUIET = "Starting net.minecraft.server.Main";
    protected static final String SERVER_START_LOG = "[19:56:37] [Server thread/INFO]: Done (13.324s)! For help, type \"help\"";
    protected CountDownLatch processStartLatch;
    protected CountDownLatch processExitLatch;
    protected CountDownLatch gameStartLatch;
    protected PipedOutputStream gameOut;

    @Value("#{T(java.lang.Long).parseLong('${palikka.test.timeout-in-millis}')}")
    protected Long testTimeoutMillis;

    protected Duration getTestTimeout() {
        return Duration.ofMillis(testTimeoutMillis);
    }

    @Autowired
    protected GameProcessService gameProcessService;

    @Autowired
    protected SessionStore sessionStore;

    @MockBean
    protected ProcessFactory processFactory;

    @MockBean
    protected PathValidator pathValidator;

    @Autowired
    protected ObjectMapper objectMapper;

    protected final BlockingQueue<TestStompSessionHandlerAdapter.Frame> userReplyQueue = new LinkedBlockingQueue<>();
    protected final BlockingQueue<TestStompSessionHandlerAdapter.Frame> logMessageQueue = new LinkedBlockingDeque<>();
    protected final BlockingQueue<TestStompSessionHandlerAdapter.Frame> lifecycleMessageQueue = new LinkedBlockingDeque<>();

    protected static String webSocketUrl;

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

    @SneakyThrows
    protected void stop(StompSession session) {
        try {
            session.disconnect();
            await().atMost(Duration.ofSeconds(1)).until(() -> sessionStore.sessionCount() == 0);
        } catch (Exception e) {
            // Ignore
        } finally {
            stop();
        }
    }

    @SneakyThrows
    protected void stop() {
        gameProcessService.stopForcibly();
        assertThat(processExitLatch.await(testTimeoutMillis, TimeUnit.MILLISECONDS)).isTrue();
    }

    /**
     * Output something as if the game process would log it.
     * @param output Output to write
     */
    @SneakyThrows
    protected void outputAsServerProcess(String output) {
        if (gameOut != null) {
            gameOut.write((output + "\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    @SneakyThrows
    protected void mockGameProcess() {
        if (!gameProcessService.isDown()) {
            throw new IllegalAccessException("Game process is not DOWN, status=" + gameProcessService.getGameProcessStatus());
        }

        Mockito.reset(processFactory, pathValidator);

        gameOut = new PipedOutputStream();
        var out = new PipedOutputStream(new PipedInputStream());

        Process mockProcess = Mockito.mock(Process.class);
        // Graceful shutdown set to always succeed as we react to the "stop" command in the hooks below
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
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

        when(processFactory.newGameProcess(any(), any(), any(), any())).thenAnswer(invocationOnMock -> {
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
            return new GameProcess(() -> mockProcess, newHooks, gameProcessService.getOutputQueue());
        });

        when(pathValidator.validatePathExistsAndIsAFile(any())).thenReturn(true);
    }

    @SneakyThrows
    protected StompSession newSession(@NonNull String token, @NonNull StompSessionHandlerAdapter sessionHandler) {
        return newStompClient()
                .connectAsync(webSocketUrl + newAuthQueryParam(token), sessionHandler)
                .get(testTimeoutMillis, TimeUnit.MILLISECONDS);
    }

    protected HttpHeaders newAuthHeader(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(token);
        return httpHeaders;
    }

    protected String newAuthQueryParam(@NonNull String token) {
        return "?token=" + token;
    }

    protected WebSocketStompClient newStompClient() {
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

    protected StompSessionHandlerAdapter newStompSessionHandler() {
        return new TestStompSessionHandlerAdapter(userReplyQueue, logMessageQueue, lifecycleMessageQueue);
    }
}
