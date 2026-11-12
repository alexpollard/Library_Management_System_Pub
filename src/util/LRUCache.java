package util;

import java.util.*;

/**
 *
 * @author s2342835
 * @param <K>
 * @param <V>
 */


public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int CAPACITY;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.CAPACITY = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > CAPACITY;
    }
}
