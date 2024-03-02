package io.github.nextentity.redis.lock;

import io.github.nextentity.redis.lock.cache.ReferenceType;
import io.github.nextentity.redis.lock.cache.ReferenceValueMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.UnifiedJedis;

import java.util.Map;
import java.util.concurrent.locks.Lock;

public class LockFactory implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(JedisSubscriber.class);

    // Cache to store locks
    private final Map<String, Lock> cache;
    // Options for configuring lock behavior
    private final Option option;
    // Support for synchronizing Redis operations
    private final SynchronizeSupport synchronizeSupport;
    // Watchdog for monitoring lock state and renewal
    private final LockKeyManager lockKeyManager;

    /**
     * Constructor to initialize LockFactory with cache, option, and synchronization support.
     *
     * @param cache              Cache for storing locks
     * @param option             Configuration options
     * @param synchronizeSupport Support for synchronizing Redis operations
     */
    public LockFactory(ReferenceValueMap<String, Lock> cache, Option option, SynchronizeSupport synchronizeSupport) {
        this.cache = cache;
        this.option = option;
        this.synchronizeSupport = synchronizeSupport;
        this.lockKeyManager = new LockKeyManager(synchronizeSupport, option);
    }

    /**
     * Get a lock for the specified key, creating it if necessary.
     *
     * @param key The key for the lock
     * @return The lock associated with the key
     */
    public Lock get(@NotNull String key) {
        return cache.computeIfAbsent(key, this::createLock);
    }

    /**
     * Static factory method to create a LockFactory with default options.
     *
     * @param jedis The UnifiedJedis client
     * @return A new instance of LockFactory
     */
    public static LockFactory of(UnifiedJedis jedis) {
        return of(new Option(), jedis);
    }

    /**
     * Static factory method to create a LockFactory with specified options.
     *
     * @param option The configuration options
     * @param jedis  The UnifiedJedis client
     * @return A new instance of LockFactory
     */
    public static LockFactory of(Option option, UnifiedJedis jedis) {
        SynchronizeSupport support = new JedisSynchronizeSupport(jedis, option.getChannelId(), option.getRetrySubscribeInterval());
        ReferenceValueMap<String, Lock> cache = new ReferenceValueMap<>(ReferenceType.WEAK);
        return new LockFactory(cache, option, support);
    }

    /**
     * Create a new lock for the specified key.
     *
     * @param key The key for the lock
     * @return The newly created lock
     */
    private Lock createLock(String key) {
        LockSynchronizer synchronizer = new LockSynchronizer(synchronizeSupport, lockKeyManager, key, option);
        return new RedisLock(synchronizer, option.getCommandAsyncExecutor(), option.getMaxReleaseDelay());
    }

    /**
     * Close the LockFactory, releasing resources.
     */
    @Override
    public void close() {
        try (SynchronizeSupport support = this.synchronizeSupport; LockKeyManager lockKeyManager = this.lockKeyManager) {
            logger.debug("Closing synchronizeSupport: {}, watchDog: {}", support, lockKeyManager);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.debug("LockFactory {} closed", this);
    }
}
