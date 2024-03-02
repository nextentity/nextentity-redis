package io.github.nextentity.redis.lock.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceValueMapTest {


    @Test
    void clear() throws InterruptedException {
        ReferenceValueMap<Object, Object> map = new ReferenceValueMap<>(ReferenceType.WEAK);
        Object value = new Object();
        map.put("one", value);
        map.put("two", new Object());

        assertEquals(2, map.size());
        System.gc();
        Thread.sleep(500);

        assertEquals(1, map.size());
    }

}