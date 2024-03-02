package io.github.nextentity.redis.lock.cache;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.*;

public class MapViewTest {

    private Map<String, Integer> map;

    @BeforeEach
    public void setUp() {
        map = new ReferenceValueMap<>(ReferenceType.WEAK);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
    }

    @Test
    public void testKeySet() {
        Set<String> keySet = map.keySet();
        
        // Check initial size
        assertEquals(3, keySet.size());
        
        // Check contents
        assertTrue(keySet.contains("one"));
        assertTrue(keySet.contains("two"));
        assertTrue(keySet.contains("three"));

        // Remove an element
        keySet.remove("one");
        assertFalse(map.containsKey("one"));
        assertEquals(2, map.size());

        // Add an element
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("four"));

        // Test iterator
        for (String key : keySet) {
            assertNotNull(map.get(key));
        }
    }

    @Test
    public void testValues() {
        Collection<Integer> values = map.values();
        
        // Check initial size
        assertEquals(3, values.size());
        
        // Check contents
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));

        // Remove an element
        values.remove(1);
        assertFalse(map.containsValue(1));
        assertEquals(2, map.size());

        // Add an element
        assertThrows(UnsupportedOperationException.class, () -> values.add(4));

        // Test iterator
        for (Integer value : values) {
            assertNotNull(value);
        }
    }

    @Test
    public void testEntrySet() {
        Set<Entry<String, Integer>> entrySet = map.entrySet();
        
        // Check initial size
        assertEquals(3, entrySet.size());
        
        // Check contents
        for (Entry<String, Integer> entry : entrySet) {
            assertTrue(map.containsKey(entry.getKey()));
            assertTrue(map.containsValue(entry.getValue()));
        }

        // Remove an element
        Entry<String, Integer> entryToRemove = null;
        for (Entry<String, Integer> entry : entrySet) {
            if ("one".equals(entry.getKey())) {
                entryToRemove = entry;
                break;
            }
        }
        entrySet.remove(entryToRemove);
        assertFalse(map.containsKey("one"));
        assertEquals(2, map.size());


        // Add an element
        entrySet.add(new AbstractMap.SimpleEntry<>("fore", 4));
        assertTrue(map.containsValue(4));
        assertEquals(map.get("fore"), 4); // Adding a value through values collection puts a null key in the map

        // Test iterator
        for (Entry<String, Integer> entry : entrySet) {
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
        }
    }
}
