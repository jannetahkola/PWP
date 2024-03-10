package fi.jannetahkola.palikka.game.testutils;

import fi.jannetahkola.palikka.game.api.game.model.GameMessage;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
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
    private final BlockingQueue<Frame> responseQueue;

    public TestStompSessionHandlerAdapter(BlockingQueue<Frame> responseQueue) {
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
        return GameMessage.class;
    }

    @Override
    public void handleFrame(@Nonnull StompHeaders headers, Object payload) {
        if (payload != null) {
            try {
                Frame frame = new Frame(headers, payload);
                log.debug("Received frame, headers={}, payload={}", headers, payload);
                if (!responseQueue.offer(frame, 1000, TimeUnit.MILLISECONDS)) {
                    log.error("Failed to add response payload to response queue");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class Frame {
        StompHeaders headers;
        Object payload;

        public <T> T getPayloadAs(Class<T> clazz) {
            return clazz.cast(payload);
        }
    }
}
