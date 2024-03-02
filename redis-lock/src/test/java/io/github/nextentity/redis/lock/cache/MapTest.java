package io.github.nextentity.redis.lock.cache;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings({"ConstantValue", "RedundantCollectionOperation"})
public class MapTest {

    private Map<String, Integer> map;

    @BeforeEach
    public void setUp() {
        map = new ReferenceValueMap<>(ReferenceType.WEAK);
    }

    @Test
    public void testPutAndGet() {
        map.put("one", 1);
        map.put("two", 2);

        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testSize() {
        map.put("one", 1);
        map.put("two", 2);

        assertEquals(2, map.size());
    }

    @Test
    public void testContainsKey() {
        map.put("one", 1);

        assertTrue(map.containsKey("one"));
        assertFalse(map.containsKey("two"));
    }

    @Test
    public void testRemove() {
        map.put("one", 1);
        map.remove("one");

        assertFalse(map.containsKey("one"));
        assertNull(map.get("one"));
    }

    @Test
    public void testClear() {
        map.put("one", 1);
        map.put("two", 2);
        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testReplace() {
        map.put("one", 1);
        map.replace("one", 10);

        assertEquals(Integer.valueOf(10), map.get("one"));
    }

    @Test
    public void testPutIfAbsent() {
        map.put("one", 1);
        map.putIfAbsent("one", 10);

        assertEquals(Integer.valueOf(1), map.get("one"));

        map.putIfAbsent("two", 2);
        assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testKeySet() {
        map.put("one", 1);
        map.put("two", 2);

        assertTrue(map.keySet().contains("one"));
        assertTrue(map.keySet().contains("two"));
    }

    @Test
    public void testValues() {
        map.put("one", 1);
        map.put("two", 2);

        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
    }

    @Test
    public void testEntrySet() {
        map.put("one", 1);
        map.put("two", 2);

        assertEquals(2, map.entrySet().size());
    }
}
