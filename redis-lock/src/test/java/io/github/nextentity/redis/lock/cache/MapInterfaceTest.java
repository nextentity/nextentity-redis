package io.github.nextentity.redis.lock.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantValue")
public class MapInterfaceTest {

    private Map<String, Integer> map;

    @BeforeEach
    public void setUp() {
        map = new ReferenceValueMap<>(ReferenceType.WEAK);
    }

    @Test
    public void testPut() {
        assertNull(map.put("one", 1));
        assertEquals(Integer.valueOf(1), map.put("one", 2));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> anotherMap = new HashMap<>();
        anotherMap.put("one", 1);
        anotherMap.put("two", 2);
        map.putAll(anotherMap);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testGet() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertNull(map.get("two"));
    }

    @Test
    public void testRemove() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), map.remove("one"));
        assertNull(map.remove("one"));
    }

    @Test
    public void testClear() {
        map.put("one", 1);
        map.put("two", 2);
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsKey() {
        map.put("one", 1);
        assertTrue(map.containsKey("one"));
        assertFalse(map.containsKey("two"));
    }

    @Test
    public void testContainsValue() {
        map.put("one", 1);
        assertTrue(map.containsValue(1));
        assertFalse(map.containsValue(2));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(map.isEmpty());
        map.put("one", 1);
        assertFalse(map.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, map.size());
        map.put("one", 1);
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySet() {
        map.put("one", 1);
        map.put("two", 2);
        Set<String> keySet = map.keySet();
        assertTrue(keySet.contains("one"));
        assertTrue(keySet.contains("two"));
        assertEquals(2, keySet.size());
    }

    @Test
    public void testValues() {
        map.put("one", 1);
        map.put("two", 2);
        Collection<Integer> values = map.values();
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySet() {
        map.put("one", 1);
        map.put("two", 2);
        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        assertEquals(2, entrySet.size());
        for (Map.Entry<String, Integer> entry : entrySet) {
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
        }
    }

    @Test
    public void testPutIfAbsent() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), map.putIfAbsent("one", 2));
        assertNull(map.putIfAbsent("two", 2));
        assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testReplace() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), map.replace("one", 2));
        assertNull(map.replace("two", 2));
    }

    @Test
    public void testReplaceWithCondition() {
        map.put("one", 1);
        assertTrue(map.replace("one", 1, 2));
        assertFalse(map.replace("one", 1, 3));
    }

    @Test
    public void testComputeIfAbsent() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), map.computeIfAbsent("one", k -> 2));
        assertEquals(Integer.valueOf(2), map.computeIfAbsent("two", k -> 2));
    }

    @Test
    public void testComputeIfPresent() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(2), map.computeIfPresent("one", (k, v) -> v + 1));
        assertNull(map.computeIfPresent("two", (k, v) -> v + 1));
    }

    @Test
    public void testCompute() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(2), map.compute("one", (k, v) -> v == null ? 0 : v + 1));
        assertEquals(Integer.valueOf(0), map.compute("two", (k, v) -> v == null ? 0 : v + 1));
    }

    @Test
    public void testMerge() {
        map.put("one", 1);
        assertEquals(Integer.valueOf(3), map.merge("one", 2, Integer::sum));
        assertEquals(Integer.valueOf(2), map.merge("two", 2, Integer::sum));
    }
}
