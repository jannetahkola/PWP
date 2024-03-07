package fi.jannetahkola.palikka.game.process;

import fi.jannetahkola.palikka.game.process.exception.GameProcessAlreadyActiveException;
import fi.jannetahkola.palikka.game.process.task.AsyncTaskSubmitter;
import fi.jannetahkola.palikka.game.process.task.InputListenerTask;
import fi.jannetahkola.palikka.game.process.task.OutputWriterTask;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class GameProcess {
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(3);
    private static final BlockingQueue<String> OUTPUT_QUEUE = new LinkedBlockingQueue<>();

    private final GameProcessExecutable executable;
    private final GameProcessHooks hooks;
    private final AtomicReference<Status> status = new AtomicReference<>(Status.INACTIVE);

    private Process process;

    public GameProcess(@NonNull GameProcessExecutable executable,
                       @NonNull GameProcessHooks hooks) {
        this.executable = executable;
        this.hooks = hooks;
    }

    public void start() throws InterruptedException {
        if (isActive()) {
            throw new GameProcessAlreadyActiveException("Failed to start - game process already active");
        }

        status.set(Status.ACTIVE);

        Thread processThread = new Thread(() -> {
            log.info("Game process start (1)");
            process = executable.execute();

            List<? extends Future<?>> ioTasks = startIOTasks();

            process.onExit().thenAccept(exitedProcess -> {
                log.info("Game process exit (1)");

                // Not necessarily terminated immediately
                ioTasks.forEach(task -> task.cancel(true));
                log.info("{}/{} I/O task(s) cancelled",
                        ioTasks.stream().filter(Future::isDone).count(),
                        ioTasks.size());

                try {
                    int exitValue = process.waitFor();
                    log.info("Game process exited with value {}", exitValue);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                status.set(Status.INACTIVE);
                hooks.getOnProcessExited().ifPresent(Runnable::run);
                log.info("Game process exit (2)");
            });

            hooks.getOnProcessStarted().ifPresent(Runnable::run);
            log.info("Game process start (2)");
        });

        processThread.start();
        processThread.join();
    }

    public boolean stop(long timeoutInMillis) throws InterruptedException {
        log.info("Stopping game process gracefully with timeout of {} ms", timeoutInMillis);
        if (isActive()) {
            OUTPUT_QUEUE.add("stop");
            return process.waitFor(timeoutInMillis, TimeUnit.MILLISECONDS);
        }
        log.info("Game process already stopped");
        return true;
    }

    public boolean stopForcibly(long timeoutInMillis) throws InterruptedException {
        if (stop(timeoutInMillis)) {
            return true;
        }
        log.info("Graceful stop timed out, forcing now");
        return process.destroyForcibly().waitFor(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isActive() {
        return status.get().equals(Status.ACTIVE);
    }

    @Builder
    @Value
    public static class GameProcessHooks {
        /**
         * Called when the process starts.
         */
        Runnable onProcessStarted;

        /**
         * Called when the process ends.
         */
        Runnable onProcessExited;

        /**
         * Called when the process has started, and is ready to accept connections.
         */
        Runnable onGameStarted;

        /**
         * Called whe the process no longer accepts connections. Called before {@link GameProcessHooks#getOnProcessExited()}.
         */
        Runnable onGameExited;

        Consumer<String> onInput;

        public Optional<Runnable> getOnProcessStarted() {
            return Optional.ofNullable(onProcessStarted);
        }

        public Optional<Runnable> getOnProcessExited() {
            return Optional.ofNullable(onProcessExited);
        }

        public Optional<Runnable> getOnGameStarted() {
            return Optional.ofNullable(onGameStarted);
        }

        public Optional<Runnable> getOnGameExited() {
            return Optional.ofNullable(onGameExited);
        }

        public Optional<Consumer<String>> getOnInput() {
            return Optional.ofNullable(onInput);
        }
    }

    private List<? extends Future<?>> startIOTasks() {
        return AsyncTaskSubmitter.submitAll(IO_EXECUTOR, List.of(
                new InputListenerTask(process.getInputStream(), input -> {
                    hooks.getOnInput().ifPresent(onInputHook -> onInputHook.accept(input));
                    // TODO These could be moved to another input listener task for performance
//                    if (!gameProcessStartDetected && input.matches(GameProcessLogPatterns.SERVER_START_PATTERN.pattern())) {
//                        hooks.getOnGameStarted().ifPresent(Runnable::run);
//                        gameProcessStartDetected = true;
//                    }
//                    if (!gameProcessExitDetected && input.matches(GameProcessLogPatterns.SERVER_STOP_PATTERN.pattern())) {
//                        hooks.getOnGameExited().ifPresent(Runnable::run);
//                        gameProcessExitDetected = true;
//                    }
                }),
                new OutputWriterTask(process.getOutputStream(), OUTPUT_QUEUE, input -> {
                    hooks.getOnInput().ifPresent(onInputHook -> onInputHook.accept(input));
                })
        ));
    }

    private enum Status {
        ACTIVE, INACTIVE
    }
}