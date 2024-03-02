package io.github.nextentity.redis.lock;

import java.util.Collection;

/**
 * Operations required to implement distributed locks.
 */
public interface SynchronizeSupport extends AutoCloseable {

    /**
     * Set the time to live (TTL) for a batch of keys.
     *
     * @param keys         Collection of keys
     * @param milliseconds Time to live (milliseconds)
     */
    void batchSetTimeToLive(Collection<String> keys, long milliseconds);

    /**
     * Delete a key if the current value equals {@code expectedValue}.
     *
     * @param key           Key
     * @param expectedValue The expected value
     * @return {@code true} if successful
     */
    boolean deleteIfValueEquals(String key, String expectedValue);

    /**
     * Set {@code value} and time to live (TTL) {@code ttl} (milliseconds) if {@code key} does not exist,
     * or get the remaining TTL if the {@code key} already exists.
     *
     * @param key   Key
     * @param value Value
     * @param ttl   Time to live (milliseconds)
     * @return {@code null} if {@code key} does not exist,
     *         or the remaining time to live (milliseconds) if the {@code key} already exists.
     */
    Long setIfAbsentOrGetRemainingTTL(String key, String value, long ttl);

    /**
     * Publish a key so that all clients can subscribe to it.
     *
     * @param key Key
     * @see SynchronizeSupport#subscribeToKey(String, Runnable)
     */
    void publishKey(String key);

    /**
     * Subscribe to a key published by the {@link SynchronizeSupport#publishKey(String)} method.
     *
     * @param key      Key
     * @param callback Callback to be executed when the key is published
     * @return A cancelable API to cancel the subscription
     * @see SynchronizeSupport#publishKey(String)
     */
    Cancelable subscribeToKey(String key, Runnable callback);

    interface Cancelable {
        void cancel();
    }
}
