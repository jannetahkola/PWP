package fi.jannetahkola.palikka.game.websocket;

import fi.jannetahkola.palikka.game.config.properties.GameProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Periodically calls {@link SessionStore#evictExpiredSessions()}.
 */
@Slf4j
@RequiredArgsConstructor
public class SessionCleanUpScheduler {
    private final AtomicInteger invocations = new AtomicInteger(0);

    private final GameProperties gameProperties;
    private final SessionStore sessionStore;

    @Scheduled(fixedDelayString = "${palikka.game.session.auto-clean.fixed-delay}")
    public void cleanUp() {
        sessionStore.evictExpiredSessions();
        invocations.incrementAndGet();
    }

    public int getInvocationCount() {
        return invocations.get();
    }

    @PostConstruct
    void postConstruct() {
        log.info("----- Session clean up scheduler ENABLED -----");
        log.info("Session clean up properties={}", gameProperties.getSession().getAutoClean());
    }
}
