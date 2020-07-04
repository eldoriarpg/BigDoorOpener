package de.eldoria.bigdoorsopener.util;

import com.google.common.base.Objects;
import lombok.Getter;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;

public class HeatCache<K, V> {
    private final Queue<HeatedObject<K, V>> tempQueue = new LinkedList<>();
    private final PriorityQueue<HeatedObject<K, V>> priorityQueue;
    private final int size;

    /**
     * Creates a new heat cache with a fixed size.
     * Will sort entries with highest access rate first.
     * Works good only if some values are accessed more often than others.
     *
     * @param size size of the cache
     * @throws IllegalArgumentException if the size is less than 1;
     */
    public HeatCache(int size) throws IllegalArgumentException {
        if (size < 1) {
            throw new IllegalArgumentException("Heat Cache size can not be less than 1");
        }
        this.size = size;
        priorityQueue = new PriorityQueue<>((o1, o2) -> Long.compare(o2.getHeat(), o1.getHeat()));
    }

    /**
     * Get a value from the cache.
     *
     * @param key key to search for
     * @return value or null if key is not found.
     */
    public V get(K key) {
        if (!priorityQueue.contains(new HeatedObject<>(key, null))) {
            return null;
        }

        V value = null;
        while (!priorityQueue.isEmpty()) {
            HeatedObject<K, V> poll = priorityQueue.poll();
            if (poll.is(key)) {
                value = poll.getValue();
                priorityQueue.add(poll);
                priorityQueue.addAll(tempQueue);
                tempQueue.clear();
            } else {
                tempQueue.add(poll);
            }
        }

        return value;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        java.util.Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                add(new HeatedObject<>(key, newValue));
                return newValue;
            }
        }

        return v;
    }

    public void add(K key, V value) {
        HeatedObject<K, V> heatedObject = new HeatedObject<>(key, value);
        if (!priorityQueue.contains(heatedObject)) {
            if (priorityQueue.size() == size) {
                HeatedObject<K, V> last = null;
                for (HeatedObject<K, V> kvHeatedObject : priorityQueue) {
                    last = kvHeatedObject;
                }
                priorityQueue.remove(last);
            }
            priorityQueue.add(heatedObject);
        }
    }

    public void add(HeatedObject<K, V> kvHeatedObject) {
        if (!priorityQueue.contains(kvHeatedObject)) {
            priorityQueue.add(kvHeatedObject);
        }
    }

    @Getter
    public static final class HeatedObject<K, V> {
        private final K key;
        private final V value;
        @Getter
        private long heat;

        private HeatedObject(K key, V value) {
            this.key = key;
            this.value = value;
            heat = 0;
        }

        public V getValue() {
            heat++;
            return value;
        }

        public boolean is(K key) {
            return key.equals(this.key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HeatedObject<?, ?> that = (HeatedObject<?, ?>) o;
            return Objects.equal(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key);
        }
    }
}

