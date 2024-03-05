package fi.jannetahkola.palikka.game.testutils;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TestStompSessionHandlerAdapter extends StompSessionHandlerAdapter {
    private final BlockingQueue<Object> responseQueue;

    public TestStompSessionHandlerAdapter(BlockingQueue<Object> responseQueue) {
        this.responseQueue = responseQueue;
    }

    @Override
    public void afterConnected(@Nonnull StompSession session,
                               @Nonnull StompHeaders connectedHeaders) {
        log.info("STOMP session connected");
    }

    @Override
    public void handleException(@Nonnull StompSession session,
                                StompCommand command,
                                @Nonnull StompHeaders headers,
                                @Nonnull byte[] payload,
                                @Nonnull Throwable exception) {
        log.error("STOMP session error: ", exception);
    }

    @Override
    public void handleTransportError(@Nonnull StompSession session,
                                     @Nonnull Throwable exception) {
        super.handleTransportError(session, exception);
        log.error("STOMP session transport error: ", exception);
    }

    @Override
    @Nonnull
    public Type getPayloadType(@Nonnull StompHeaders headers) {
        return String.class;
    }

    @Override
    public void handleFrame(@Nonnull StompHeaders headers, Object payload) {
        if (payload != null) {
            try {
                log.debug("Received frame, headers={}, payload={}", headers, payload);
                if (!responseQueue.offer(payload, 500, TimeUnit.MILLISECONDS)) {
                    log.error("Failed to add response payload to response queue");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
