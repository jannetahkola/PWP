package fi.jannetahkola.palikka.game.process.task;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

@Slf4j
public record OutputWriterTask(@NonNull OutputStream out,
                              @NonNull BlockingQueue<String> outputQueue,
                              Consumer<String> onOutput) implements Runnable {
    @Override
    public void run() {
        log.info("Start");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        while (!Thread.currentThread().isInterrupted()) {
            String output = null;
            try {
                output = outputQueue.take();
                log.debug("Writing output \"{}\"", output);
                writer.write(output);
                writer.newLine();
                writer.flush();
                if (onOutput != null) {
                    // Do this last in case it throws - we still want to
                    // send the output
                    onOutput.accept(output);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error writing output \"{}\" into the stream: ", output, e);
            }
        }
        log.info("Stop");
    }
}
