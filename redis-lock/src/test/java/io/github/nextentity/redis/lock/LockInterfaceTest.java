package io.github.nextentity.redis.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SimplifiableAssertion")
public class LockInterfaceTest {

    private Lock lock;

    @BeforeEach
    public void setUp() {
        LockFactory factory = LockFactory.of(RedisConfig.getJedisPooled());
        lock = factory.get(UUID.randomUUID().toString());
    }

    @Test
    public void testLockAndUnlock() {
        lock.lock();
        try {
            assertTrue(((RedisLock) lock).isHeldByCurrentThread());
        } finally {
            lock.unlock();
        }
        assertFalse(((RedisLock) lock).isHeldByCurrentThread());
    }

    @Test
    public void testTryLock() {
        assertTrue(lock.tryLock());
        try {
            assertTrue(((RedisLock) lock).isHeldByCurrentThread());
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void testTryLockWithTimeout() throws InterruptedException {
        assertTrue(lock.tryLock(1, TimeUnit.SECONDS));
        try {
            assertTrue(((RedisLock) lock).isHeldByCurrentThread());
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void testLockInterruptibly() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            assertTrue(((RedisLock) lock).isHeldByCurrentThread());
        } finally {
            lock.unlock();
        }

        Thread interruptingThread = new Thread(() -> {
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        interruptingThread.start();
        interruptingThread.interrupt();
        interruptingThread.join();
    }

    @Test
    public void testUnlockWithoutLock() {
        assertThrows(IllegalMonitorStateException.class, () -> lock.unlock());
    }


    @Test
    public void testMultipleLocks() {
        lock.lock();
        lock.lock();
        try {
            assertTrue(((RedisLock) lock).getHoldCount() == 2);
        } finally {
            lock.unlock();
            lock.unlock();
        }
        assertTrue(((RedisLock) lock).getHoldCount() == 0);
    }
}
