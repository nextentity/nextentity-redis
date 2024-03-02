package io.github.nextentity.redis.lock;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Manages the synchronization logic for acquiring and releasing distributed locks.
 */
public class LockSynchronizer {
    private static final Logger logger = LoggerFactory.getLogger(LockSynchronizer.class);
    private static final long NOT_LOCKED_MARK = Long.MIN_VALUE;

    private final SynchronizeSupport support;
    @Getter
    private final String key;
    private final String clientId;
    private final long timeToLiveMillis;
    private final long maxWaitTimeMillis;
    private final LockKeyManager lockKeyManager;

    private final AtomicLong lockedTime = new AtomicLong(NOT_LOCKED_MARK);

    /**
     * Constructs a LockSynchronizer with the specified parameters.
     *
     * @param support        SynchronizeSupport for managing Redis operations
     * @param lockKeyManager LockKeyManager for managing lock keys
     * @param key            The lock key
     * @param option         Configuration options
     */
    public LockSynchronizer(SynchronizeSupport support, LockKeyManager lockKeyManager, String key, Option option) {
        this.support = support;
        this.key = key;
        this.clientId = option.getClientId();
        this.timeToLiveMillis = option.getKeyTimeToLive();
        this.maxWaitTimeMillis = option.getWaitLimit();
        this.lockKeyManager = lockKeyManager;
    }

    /**
     * Acquires the lock, blocking until it is available.
     */
    public void acquireLock() {
        while (true) {
            if (tryAcquireLockUntil(Long.MAX_VALUE)) {
                return;
            }
        }
    }

    /**
     * Attempts to acquire the lock immediately.
     *
     * @return true if the lock was acquired, false otherwise
     */
    public boolean tryAcquireLock() {
        return tryAcquireLockUntil(0);
    }

    /**
     * Attempts to acquire the lock until the specified deadline.
     *
     * @param deadline The deadline in milliseconds
     * @return true if the lock was acquired, false otherwise
     */
    public boolean tryAcquireLockUntil(long deadline) {
        Thread currentThread = Thread.currentThread();
        SynchronizeSupport.Cancelable subscription = support.subscribeToKey(key, () -> {
            logger.debug("Waiting for release {}[{}] successful", key, currentThread.getName());
            LockSupport.unpark(currentThread);
        });

        long waitTime = 0;
        try {
            do {
                awaitReleaseSignal(waitTime);
                Long remainingTTL = support.setIfAbsentOrGetRemainingTTL(key, clientId, timeToLiveMillis);
                if (remainingTTL == null) {
                    markAsLocked();
                    return true;
                } else if (System.currentTimeMillis() >= deadline) {
                    return false;
                }
                waitTime = calculateWaitTime(deadline, System.currentTimeMillis() + remainingTTL);
            } while (true);
        } finally {
            subscription.cancel();
        }
    }

    private long calculateWaitTime(long deadline, long expirationTime) {
        long remainingDeadline = deadline - System.currentTimeMillis();
        long remainingTTL = expirationTime - System.currentTimeMillis();
        long waitTime = remainingTTL > 0 && remainingTTL < remainingDeadline ? remainingTTL : remainingDeadline;
        return Math.min(waitTime, maxWaitTimeMillis);
    }

    private void awaitReleaseSignal(long waitTime) {
        if (waitTime <= 0) {
            return;
        }
        logger.debug("Expected wait time: {} ms", waitTime);
        long startTime = System.currentTimeMillis();
        try {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTime));
        } catch (Exception e) {
            logger.warn("Error while waiting for release of {}: ", key, e);
        }
        long actualWaitTime = System.currentTimeMillis() - startTime;
        logger.debug("Actual wait time: {} ms", actualWaitTime);
    }

    /**
     * Releases the lock.
     */
    public void releaseLock() {
        if (lockedTime.getAndSet(NOT_LOCKED_MARK) == NOT_LOCKED_MARK) {
            throw new IllegalStateException(key + ":" + clientId + " is not locked");
        }
        logger.debug("Unlocked {}", this);
        lockKeyManager.removeKey(key);
        if (support.deleteIfValueEquals(key, clientId)) {
            support.publishKey(key);
        } else {
            logger.warn("{}:{} failed to delete key upon unlocking", key, clientId);
            throw new IllegalMonitorStateException(key + ":" + clientId + " is not locked");
        }
    }

    private void markAsLocked() {
        if (!lockedTime.compareAndSet(NOT_LOCKED_MARK, System.currentTimeMillis())) {
            throw new IllegalMonitorStateException(key + ":" + clientId + " is already locked");
        }
        logger.debug("Locked {}", this);
        lockKeyManager.addKey(key);
    }

    public boolean isLocked() {
        return lockedTime.get() != NOT_LOCKED_MARK;
    }

    public Long getLockedTime() {
        long time = lockedTime.get();
        return time == NOT_LOCKED_MARK ? null : time;
    }

}
