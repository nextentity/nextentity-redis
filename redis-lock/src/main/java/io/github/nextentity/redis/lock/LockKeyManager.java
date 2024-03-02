package io.github.nextentity.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors and manages the renewal of lock keys to ensure they remain valid.
 */
public class LockKeyManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LockKeyManager.class);

    private final SynchronizeSupport synchronizeSupport;
    private final Set<String> keys = ConcurrentHashMap.newKeySet();
    private final long timeToLive;
    private final ScheduledFuture<?> renewalTask;

    /**
     * Constructs a LockKeyManager with the provided SynchronizeSupport and options.
     *
     * @param synchronizeSupport SynchronizeSupport for synchronizing operations
     * @param option Configuration options
     */
    public LockKeyManager(SynchronizeSupport synchronizeSupport, Option option) {
        this.synchronizeSupport = synchronizeSupport;
        this.timeToLive = option.getKeyTimeToLive();
        long renewalInterval = option.getRenewalInterval();
        this.renewalTask = option.getScheduler()
                .scheduleAtFixedRate(this::renewKeys, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Renews the time-to-live (TTL) for all managed keys.
     */
    private void renewKeys() {
        if (keys.isEmpty()) {
            return;
        }
        logger.debug("Renewing TTL for keys: {}", keys);
        synchronizeSupport.batchSetTimeToLive(keys, timeToLive);
    }

    /**
     * Adds a key to the manager for TTL renewal.
     *
     * @param key The key to be added
     */
    public void addKey(String key) {
        keys.add(Objects.requireNonNull(key));
    }

    /**
     * Removes a key from the manager.
     *
     * @param key The key to be removed
     */
    public void removeKey(String key) {
        keys.remove(key);
    }

    /**
     * Stops the renewal task and cleans up resources.
     */
    @Override
    public void close() {
        renewalTask.cancel(false);
    }
}
