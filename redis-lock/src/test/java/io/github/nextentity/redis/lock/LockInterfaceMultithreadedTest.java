package io.github.nextentity.redis.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class LockInterfaceMultithreadedTest {

    private Lock lock;

    @BeforeEach
    public void setUp() {
        LockFactory factory = LockFactory.of(RedisConfig.getJedisPooled());
        lock = factory.get("test");
    }

    @Test
    public void testConcurrentLocking() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            lock.lock();
            try {
                // Simulate some work with the lock held
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                // Attempt to acquire the lock with a timeout
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        // Simulate some work with the lock held
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        lock.unlock();
                    }
                } else {
                    fail("Thread 2 could not acquire the lock");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread thread3 = new Thread(() -> {
            try {
                if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    lock.unlock();
                    throw new IllegalStateException("Thread 3 could not acquire the lock");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread thread4 = new Thread(() -> {
            if (lock.tryLock()) {
                lock.unlock();
                throw new IllegalStateException("Thread 4 could not acquire the lock");
            }
        });

        thread1.start();
        Thread.sleep(100); // Ensure thread1 acquires the lock first
        thread2.start();
        thread3.start();
        thread4.start();

        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
    }

    @Test
    public void testLockInterruptiblyWithContention() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            lock.lock();
            try {
                // Simulate some work with the lock held
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                lock.lockInterruptibly();
                try {
                    // Simulate some work with the lock held
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        thread1.start();
        Thread.sleep(100); // Ensure thread1 acquires the lock first
        thread2.start();
        thread2.interrupt(); // Interrupt thread2 while it's waiting for the lock

        thread1.join();
        thread2.join();

        assertTrue(thread2.isInterrupted());
    }

}
