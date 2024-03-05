package fi.jannetahkola.palikka.game.process.task;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Scanner;
import java.util.function.Consumer;

@Slf4j
public record InputListenerTask(@NonNull InputStream in, @NonNull Consumer<String> onInput) implements Runnable {
    @Override
    public void run() {
        log.info("Start");
        Scanner scanner = new Scanner(in);
        try {
            while (!Thread.currentThread().isInterrupted() && scanner.hasNextLine()) {
                String line = scanner.nextLine();
                onInput.accept(line);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        log.info("Stop");
    }
}
