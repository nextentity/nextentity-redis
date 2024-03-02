package io.github.nextentity.redis.lock;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A distributed lock implementation using Redis.
 */
public class RedisLock implements Lock {

    private final ReentrantLock localLock = new ReentrantLock();
    private final LockSynchronizer synchronizer;
    private final Executor asyncExecutor;
    private final long maxReleaseDelayMillis;

    /**
     * Constructs a RedisLock with the specified synchronizer, executor, and maximum release delay.
     *
     * @param synchronizer          The synchronizer for managing Redis operations
     * @param asyncExecutor         The executor for asynchronous operations
     * @param maxReleaseDelayMillis The maximum delay for releasing the lock
     */
    public RedisLock(LockSynchronizer synchronizer, Executor asyncExecutor, long maxReleaseDelayMillis) {
        this.synchronizer = synchronizer;
        this.asyncExecutor = asyncExecutor;
        this.maxReleaseDelayMillis = maxReleaseDelayMillis;
    }

    @Override
    public void lock() {
        localLock.lock();
        acquireLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        localLock.lockInterruptibly();
        acquireLock();
    }

    private void acquireLock() {
        try {
            if (!synchronizer.isLocked()) {
                synchronizer.acquireLock();
            }
        } catch (Throwable throwable) {
            localLock.unlock();
            throw throwable;
        }
    }

    @Override
    public boolean tryLock() {
        if (localLock.tryLock()) {
            try {
                return synchronizer.isLocked() || synchronizer.tryAcquireLock();
            } finally {
                if (!synchronizer.isLocked()) {
                    localLock.unlock();
                }
            }
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, @NotNull TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(time);
        if (localLock.tryLock(time, unit)) {
            try {
                if (synchronizer.isLocked()) {
                    return true;
                }
                return synchronizer.tryAcquireLockUntil(deadline);
            } finally {
                if (!synchronizer.isLocked()) {
                    localLock.unlock();
                }
            }
        }
        return false;
    }

    @Override
    public void unlock() {
        int holdCount = localLock.getHoldCount();
        if (holdCount <= 0 || !synchronizer.isLocked()) {
            throw new IllegalMonitorStateException();
        }
        if (holdCount > 1) {
            localLock.unlock();
        } else {
            handleUnlock();
        }
    }

    private void handleUnlock() {
        Long lockedTime = synchronizer.getLockedTime();
        Objects.requireNonNull(lockedTime, "Locked time cannot be null");
        if (localLock.getQueueLength() == 0 || System.currentTimeMillis() - lockedTime > maxReleaseDelayMillis) {
            releaseSynchronizer();
        } else {
            releaseSynchronizerAsync();
        }
    }

    private void releaseSynchronizer() {
        synchronizer.releaseLock();
        localLock.unlock();
    }

    private void releaseSynchronizerAsync() {
        localLock.unlock();
        asyncExecutor.execute(() -> {
            if (localLock.tryLock()) {
                try {
                    if (localLock.getHoldCount() == 1 && synchronizer.isLocked()) {
                        synchronizer.releaseLock();
                    }
                } finally {
                    localLock.unlock();
                }
            }
        });
    }

    public boolean isHeldByCurrentThread() {
        return localLock.isHeldByCurrentThread();
    }

    public boolean isLocked() {
        return localLock.isLocked();
    }

    public int getHoldCount() {
        return localLock.getHoldCount();
    }

    @NotNull
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("newCondition is not supported in RedisLock");
    }
}
