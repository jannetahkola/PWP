package fi.jannetahkola.palikka.game.testutils;

import fi.jannetahkola.palikka.game.process.GameProcess;
import fi.jannetahkola.palikka.game.service.GameProcessService;
import fi.jannetahkola.palikka.game.service.factory.ProcessFactory;
import fi.jannetahkola.palikka.game.service.validator.PathValidator;
import lombok.SneakyThrows;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.stomp.StompSession;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Autowired
    protected GameProcessService gameProcessService;

    @MockBean
    protected ProcessFactory processFactory;

    @MockBean
    protected PathValidator pathValidator;

    @SneakyThrows
    protected void stop(StompSession session) {
        try {
            session.disconnect();
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
            throw new IllegalAccessException("Game process is not DOWN");
        }

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
}
