package fi.jannetahkola.palikka.game.util;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SynchronizedLimitedQueueTests {
    @Test
    void testOldestEntryIsEvictedWhenFilled() {
        SynchronizedLimitedQueue q = new SynchronizedLimitedQueue(1);
        q.add("test");
        q.add("test2");
        q.add("test3");
        assertThat(q.size()).isEqualTo(1);
        assertThat(q.copy()).contains("test3");
    }

    @SneakyThrows
    @Test
    void testAsynchronousAdd() {
        SynchronizedLimitedQueue q = new SynchronizedLimitedQueue(1);
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> q.add("test")),
                CompletableFuture.runAsync(() -> q.add("test2")),
                CompletableFuture.runAsync(() -> q.add("test3"))
        ).get(1000, TimeUnit.SECONDS);
        assertThat(q.size()).isEqualTo(1);
    }
}
