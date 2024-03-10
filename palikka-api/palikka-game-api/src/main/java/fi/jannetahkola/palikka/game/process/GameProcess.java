package fi.jannetahkola.palikka.game.process;

import fi.jannetahkola.palikka.game.process.exception.GameProcessAlreadyActiveException;
import fi.jannetahkola.palikka.game.process.task.AsyncTaskSubmitter;
import fi.jannetahkola.palikka.game.process.task.InputListenerTask;
import fi.jannetahkola.palikka.game.process.task.OutputWriterTask;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class GameProcess {
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(3);

    private final GameProcessExecutable executable;
    private final GameProcessHooks hooks;
    private final BlockingQueue<String> outputQueue;
    private final AtomicReference<Status> status = new AtomicReference<>(Status.INACTIVE);

    private Process process;

    /**
     * Set after a console output has been detected indicating that the server is ready to accept connections.
     * Used as an additional check to remove the need to match the start regex after the server is up.
     */
    private boolean processStarted = false;

    public GameProcess(@NonNull GameProcessExecutable executable,
                       @NonNull GameProcessHooks hooks,
                       @NonNull BlockingQueue<String> outputQueue) {
        this.executable = executable;
        this.hooks = hooks;
        this.outputQueue = outputQueue;
    }

    public void start() throws InterruptedException {
        if (isActive()) {
            throw new GameProcessAlreadyActiveException("Failed to start - game process already active");
        }

        setStatusAndLog(Status.ACTIVE);

        Thread processThread = new Thread(() -> {
            log.info("Game process start (1)");
            process = executable.execute();

            List<? extends Future<?>> ioTasks = startIOTasks();
            log.info("{} I/O task(s) started", ioTasks.size());

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

                setStatusAndLog(Status.INACTIVE);
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
            outputQueue.add("stop");
            return process.waitFor(timeoutInMillis, TimeUnit.MILLISECONDS);
        }
        log.info("Game process already stopped");
        return true;
    }

    /**
     * Stops the process by first attempting graceful shutdown.
     * @param timeoutInMillis Timeout - at maximum double the value if graceful shutdown fails
     * @return True if the process was shutdown
     * @throws InterruptedException
     */
    public boolean stopForcibly(long timeoutInMillis) throws InterruptedException {
        if (stop(timeoutInMillis)) {
            return true;
        }
        log.info("Graceful stop timed out, forcing now");
        try {
            process.destroyForcibly().onExit().get(timeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Force stop timed out", e);
            return false;
        }
        return true;
    }

    public boolean isActive() {
        return status.get().equals(Status.ACTIVE);
    }

    @Builder(toBuilder = true)
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

    private void setStatusAndLog(Status newStatus) {
        status.set(newStatus);
        log.info("Set process status={}", status.get());
    }

    private List<? extends Future<?>> startIOTasks() {
        return AsyncTaskSubmitter.submitAll(IO_EXECUTOR, List.of(
                new InputListenerTask(process.getInputStream(), input -> {
                    hooks.getOnInput().ifPresent(onInputHook -> onInputHook.accept(input));
                    // TODO These could be moved to another input listener task for performance
                    if (!processStarted && GameProcessPatterns.SERVER_START_PATTERN.matcher(input).matches()) {
                        log.info("Start detected, ready for connections");
                        processStarted = true;
                        hooks.getOnGameStarted().ifPresent(Runnable::run);
                    }

//                    if (!gameProcessStartDetected && input.matches(GameProcessLogPatterns.SERVER_START_PATTERN.pattern())) {
//                        hooks.getOnGameStarted().ifPresent(Runnable::run);
//                        gameProcessStartDetected = true;
//                    }
//                    if (!gameProcessExitDetected && input.matches(GameProcessLogPatterns.SERVER_STOP_PATTERN.pattern())) {
//                        hooks.getOnGameExited().ifPresent(Runnable::run);
//                        gameProcessExitDetected = true;
//                    }
                }),
                new OutputWriterTask(process.getOutputStream(), outputQueue,
                        input -> hooks.getOnInput().ifPresent(onInputHook -> onInputHook.accept(input)))
        ));
    }

    private enum Status {
        ACTIVE, INACTIVE
    }
}
