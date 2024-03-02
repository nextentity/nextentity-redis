package io.github.nextentity.redis.lock.cache;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.*;
import java.util.Map;
import java.util.Objects;

/**
 * the specific public implementation type of {@link Reference}
 */
public enum ReferenceType {

    /**
     * type of {@link WeakReference}
     */
    WEAK {
        @Override
        <K, V> ReferenceValue<V> createValue(Map<K, ReferenceValue<V>> map, K k, V v) {
            return new Weak<>(map, k, v);
        }
    },

    /**
     * type of {@link SoftReference}
     */
    SOFT {
        @Override
        <K, V> ReferenceValue<V> createValue(Map<K, ReferenceValue<V>> map, K k, V v) {
            return new Soft<>(map, k, v);
        }
    },

    /**
     * type of {@link PhantomReference}
     */
    PHANTOM {
        @Override
        <K, V> ReferenceValue<V> createValue(Map<K, ReferenceValue<V>> map, K k, V v) {
            return new Phantom<>(map, k, v);
        }
    };

    abstract <K, V> ReferenceValue<V> createValue(Map<K, ReferenceValue<V>> map, K k, V v);

    static class Weak<T> extends WeakReference<T> implements CleanableReferenceValue<T> {
        private final Map<?, ReferenceValue<T>> map;
        private final Object key;

        public Weak(Map<?, ReferenceValue<T>> map, Object key, T value) {
            super(value, REFERENCE_QUEUE);
            this.map = map;
            this.key = key;
        }

        @Override
        public Map<?, ReferenceValue<T>> map() {
            return this.map;
        }

        @Override
        public Object key() {
            return this.key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReferenceValue<?> that)) return false;
            return Objects.equals(get(), that.get());
        }

        @Override
        public int hashCode() {
            return Objects.hash(get());
        }
    }

    static class Soft<T> extends SoftReference<T> implements CleanableReferenceValue<T> {
        private final Map<?, ReferenceValue<T>> container;
        private final Object key;

        public Soft(Map<?, ReferenceValue<T>> map, Object key, T value) {
            super(value, REFERENCE_QUEUE);
            this.container = map;
            this.key = key;
        }

        @Override
        public Map<?, ReferenceValue<T>> map() {
            return this.container;
        }

        @Override
        public Object key() {
            return this.key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReferenceValue<?> that)) return false;
            return Objects.equals(get(), that.get());
        }

        @Override
        public int hashCode() {
            return Objects.hash(get());
        }
    }

    static class Phantom<T> extends PhantomReference<T> implements CleanableReferenceValue<T> {
        private final Map<?, ReferenceValue<T>> container;
        private final Object key;

        public Phantom(Map<?, ReferenceValue<T>> map, Object key, T value) {
            super(value, REFERENCE_QUEUE);
            this.container = map;
            this.key = key;
        }

        @Override
        public Map<?, ReferenceValue<T>> map() {
            return this.container;
        }

        @Override
        public Object key() {
            return this.key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReferenceValue<?> that)) return false;
            return Objects.equals(get(), that.get());
        }

        @Override
        public int hashCode() {
            return Objects.hash(get());
        }
    }

    static class ValueWrapper<T> implements ReferenceValue<T> {

        private final T value;

        ValueWrapper(T value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReferenceValue<?> that)) return false;
            return Objects.equals(value, that.get());
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public T get() {
            return value;
        }
    }

    interface ReferenceValue<T> {
        static <V> ReferenceValue<V> of(V value) {
            return new ValueWrapper<>(value);
        }

        T get();
    }

    private interface CleanableReferenceValue<T> extends ReferenceValue<T> {
        ReferenceQueue<Object> REFERENCE_QUEUE = runnableReferenceQueue();

        private static @NotNull ReferenceQueue<Object> runnableReferenceQueue() {
            ReferenceQueue<Object> queue = new ReferenceQueue<>();
            Thread thread = new Thread(() -> {
                Logger log = LoggerFactory.getLogger(ReferenceValue.class);
                while (true) {
                    try {
                        Reference<?> reference = queue.remove();
                        if (reference instanceof CleanableReferenceValue<?> referenceValue) {
                            referenceValue.clean();
                            log.debug("{} has been executed", reference);
                        } else {
                            log.error("{} is not Runnable", reference.getClass().getName());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
            return queue;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        default void clean() {
            Map map = map();
            map.compute(key(), (k, v) -> v == this ? null : v);
        }

        Map<?, ReferenceValue<T>> map();

        Object key();
    }
}
