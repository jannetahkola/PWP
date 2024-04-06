package fi.jannetahkola.palikka.game.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class SynchronizedLimitedQueue {
    private final List<String> list = new LinkedList<>();
    private final int limit;

    public SynchronizedLimitedQueue(int limit) {
        this.limit = limit;
    }

    public void add(String item) {
        synchronized (list) {
            list.add(item);
            if (list.size() > limit) {
                list.removeFirst();
            }
        }
    }

    public List<String> copy() {
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public int size() {
        synchronized (list) {
            return list.size();
        }
    }
}
