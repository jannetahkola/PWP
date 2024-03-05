package fi.jannetahkola.palikka.game.process.task;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@UtilityClass
public class AsyncTaskSubmitter {
    /**
     * {@link ExecutorService#invokeAll(Collection)} will block until the tasks are done, so this is a
     * utility method that returns the futures immediately after they've been submitted.
     */
    public List<? extends Future<?>> submitAll(ExecutorService executorService, List<Runnable> tasks) {
        return tasks.stream()
                .map(executorService::submit)
                .toList();
    }
}
