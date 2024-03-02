package io.github.nextentity.redis.lock.cache;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@code ConcurrentHashMap} based implementation of the {@code Map} interface.
 * The value is maintained in the Reference type. Deletes the {@code key} when the value is reclaimed.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see ConcurrentHashMap
 */
public class ReferenceValueMap<K, V> implements Map<K, V> {

    private final ConcurrentHashMap<K, ReferenceType.ReferenceValue<V>> target = new ConcurrentHashMap<>();
    private final ReferenceType referenceType;

    public ReferenceValueMap(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public V computeIfAbsent(@NotNull K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        AtomicReference<V> result = new AtomicReference<>();
        target.compute(key, (k, ref) -> {
            if (ref != null) {
                result.set(ref.get());
            }
            if (result.get() == null) {
                V newValue = mappingFunction.apply(key);
                if (newValue != null) {
                    result.set(newValue);
                    ref = newReference(key, newValue);
                } else {
                    ref = null;
                }
            }
            return ref;
        });
        return result.get();
    }

    private ReferenceType.ReferenceValue<V> newReference(K key, V value) {
        return value == null ? null : referenceType.createValue(target, key, value);
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public boolean isEmpty() {
        return target.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return target.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        //noinspection unchecked
        return target.containsValue(ReferenceType.ReferenceValue.of((V) value));
    }

    @Override
    public V get(Object key) {
        return getValue(target.get(key));
    }

    private V getValue(ReferenceType.ReferenceValue<V> reference) {
        return reference == null ? null : reference.get();
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        ReferenceType.ReferenceValue<V> reference = newReference(key, value);
        ReferenceType.ReferenceValue<V> put = target.put(key, reference);
        return getValue(put);
    }

    @Override
    public V remove(Object key) {
        return getValue(target.remove(key));
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        target.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return target.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return new Values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        target.forEach((k, v) -> action.accept(k, getValue(v)));
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        target.replaceAll((k, ref) -> {
            V v = function.apply(k, getValue(ref));
            return newReference(k, v);
        });
    }

    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        ReferenceType.ReferenceValue<V> reference = target.putIfAbsent(key, newReference(key, value));
        return getValue(reference);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return target.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return target.replace(key, newReference(key, oldValue), newReference(key, newValue));
    }

    @Nullable
    @Override
    public V replace(K key, V value) {
        ReferenceType.ReferenceValue<V> replace = target.replace(key, newReference(key, value));
        return getValue(replace);
    }

    @Override
    public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        ReferenceType.ReferenceValue<V> reference = target.computeIfPresent(key, wrapRemapping(key, remappingFunction));
        return getValue(reference);
    }

    private @NotNull BiFunction<K, ReferenceType.ReferenceValue<V>, ReferenceType.ReferenceValue<V>>
    wrapRemapping(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return (k, ref) -> {
            V v = remappingFunction.apply(k, getValue(ref));
            return newReference(key, v);
        };
    }

    @Override
    public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        ReferenceType.ReferenceValue<V> reference = target.compute(key, wrapRemapping(key, remappingFunction));
        return getValue(reference);
    }

    @Override
    public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        ReferenceType.ReferenceValue<V> reference = target.merge(key, newReference(key, value), (a, b) -> {
            V v = remappingFunction.apply(getValue(a), getValue(b));
            return newReference(key, v);
        });
        return getValue(reference);
    }

    class Values implements Collection<V> {
        private final Collection<ReferenceType.ReferenceValue<V>> values = target.values();

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public boolean isEmpty() {
            return values.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return values.contains(ReferenceType.ReferenceValue.of(o));
        }

        @NotNull
        @Override
        public Iterator<V> iterator() {
            return new Iterator<>() {
                private final Iterator<ReferenceType.ReferenceValue<V>> iterator = values.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public V next() {
                    return getValue(iterator.next());
                }

                @Override
                public void remove() {
                    Iterator.super.remove();
                }

                @Override
                public void forEachRemaining(Consumer<? super V> action) {
                    iterator.forEachRemaining(ref -> action.accept(ref.get()));
                }
            };
        }

        @NotNull
        @Override
        public Object @NotNull [] toArray() {
            return values.stream().map(ReferenceType.ReferenceValue::get).toArray();
        }

        @NotNull
        @Override
        public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
            return values.stream().map(ReferenceType.ReferenceValue::get).toList().toArray(a);
        }

        @Override
        public boolean add(V v) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            return values.remove(ReferenceType.ReferenceValue.of(o));
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return values.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            return values.removeAll(c);
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            return values.retainAll(c);
        }

        @Override
        public void clear() {
            values.clear();
        }
    }


    class EntrySet implements Set<Map.Entry<K, V>> {
        Set<Entry<K, ReferenceType.ReferenceValue<V>>> set = target.entrySet();

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return set.contains(o);
        }

        @NotNull
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new Iterator<>() {
                private final Iterator<Entry<K, ReferenceType.ReferenceValue<V>>> iterator = set.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Entry<K, V> next() {
                    Entry<K, ReferenceType.ReferenceValue<V>> next = iterator.next();
                    return map(next);
                }
            };
        }

        private @NotNull Entry<K, V> map(Entry<K, ReferenceType.ReferenceValue<V>> next) {
            return new Entry<>() {
                @Override
                public K getKey() {
                    return next.getKey();
                }

                @Override
                public V getValue() {
                    return ReferenceValueMap.this.getValue(next.getValue());
                }

                @Override
                public V setValue(V value) {
                    ReferenceType.ReferenceValue<V> ref = next.setValue(newReference(next.getKey(), value));
                    return ReferenceValueMap.this.getValue(ref);
                }
            };
        }

        @NotNull
        @Override
        public Object @NotNull [] toArray() {
            return set.stream().map(this::map).toArray();
        }

        @NotNull
        @Override
        public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
            return set.stream().map(this::map).toList().toArray(a);
        }

        @Override
        public boolean add(Entry<K, V> kvEntry) {
            Entry<K, ReferenceType.ReferenceValue<V>> entry = getkRunnableReferenceEntry(kvEntry);
            return set.add(entry);
        }

        private @NotNull Entry<K, ReferenceType.ReferenceValue<V>> getkRunnableReferenceEntry(Entry<K, V> kvEntry) {
            K key = kvEntry.getKey();
            V value = kvEntry.getValue();
            ReferenceType.ReferenceValue<V> reference = newReference(key, value);
            return new AbstractMap.SimpleEntry<>(key, reference);
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Entry<?, ?> entry) {
                Object key = entry.getKey();
                //noinspection unchecked
                ReferenceType.ReferenceValue<V> reference = newReference((K) key, (V) entry.getValue());
                return set.remove(new AbstractMap.SimpleEntry<>(key, reference));
            } else {
                return false;
            }
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            return set.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends Entry<K, V>> c) {
            return set.addAll(
                    c.stream()
                            .map(this::getkRunnableReferenceEntry)
                            .toList()
            );
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            return set.retainAll(c);
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            return set.removeAll(c);
        }

        @Override
        public void clear() {
            set.clear();
        }
    }


}
