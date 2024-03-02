package io.github.nextentity.redis.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

class RedisLockTest {
    public static final Logger logger = LoggerFactory.getLogger(RedisLockTest.class);

    private static final int THREAD_COUNT_PRE_CLIENT = 4;
    private static final int CLIENT_COUNT = 8;
    private static final int ITERATIONS = 6250;
    private static volatile int counter = 0;
    private static final AtomicInteger expected = new AtomicInteger();
    private static final String key = UUID.randomUUID().toString();
    private static final AtomicInteger id = new AtomicInteger();

    private final LockFactory lockFactory = LockFactory.of(RedisConfig.getJedisPooled());
    private final Lock lock = lockFactory.get(key);
    // private static final Lock lock = getRlock();

    @Test
    void testLock() {
        long start = System.currentTimeMillis();
        List<Thread> threads = IntStream.range(0, CLIENT_COUNT)
                .mapToObj(i -> {
                    try {
                        Thread thread = new Thread(new RedisLockTest()::testNewClient);
                        thread.setName("test-" + i);
                        return thread;
                    } catch (Exception e) {
                        logger.error("", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .peek(Thread::start)
                .toList();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
        int expectedCounter = THREAD_COUNT_PRE_CLIENT * CLIENT_COUNT * ITERATIONS;
        Assertions.assertEquals(expected.get(), counter);
        Assertions.assertEquals(expectedCounter, counter);
        long cost = System.currentTimeMillis() - start;
        Assertions.assertTrue(cost < TimeUnit.SECONDS.toMillis(10));
        logger.info("complete count {} in {}ms", counter, cost);
    }

    private void testNewClient() {
        Thread[] threads = new Thread[THREAD_COUNT_PRE_CLIENT];

        for (int i = 0; i < THREAD_COUNT_PRE_CLIENT; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    acquireLock();
                    try {
                        incrementCounter();
                    } finally {
                        releaseLock();
                    }
                }
                logger.info("DONE");
            });
        }

        for (Thread thread : threads) {
            thread.setName("worker-" + id.incrementAndGet());
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
        logger.info("ALL DONE");
        lockFactory.close();
    }

    private void acquireLock() {
        lock.lock();
    }

    private void releaseLock() {
        lock.unlock();
    }

    private void incrementCounter() {
        //noinspection NonAtomicOperationOnVolatileField
        counter++;
        expected.incrementAndGet();
    }

}