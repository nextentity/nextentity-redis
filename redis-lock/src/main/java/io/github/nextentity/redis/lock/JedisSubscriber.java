package io.github.nextentity.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.UnifiedJedis;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Manages Redis pub/sub for lock notifications.
 */
public class JedisSubscriber implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(JedisSubscriber.class);

    private final UnifiedJedis jedis;
    private final Map<String, Set<Runnable>> subscribers = new ConcurrentHashMap<>();
    private final String channelId;
    private final JedisPubSub listener;
    private final long retryIntervalMillis;

    private volatile boolean isShutdown;

    /**
     * Constructs a JedisSubscriber with the specified parameters.
     *
     * @param jedis               UnifiedJedis client for Redis operations
     * @param channelId           Channel ID for pub/sub communication
     * @param retryIntervalMillis Retry interval in milliseconds for reconnection attempts
     */
    public JedisSubscriber(UnifiedJedis jedis, String channelId, long retryIntervalMillis) {
        this.jedis = jedis;
        this.channelId = channelId;
        this.retryIntervalMillis = retryIntervalMillis;
        this.listener = new SubscriberListener();
        startSubscription();
    }

    /**
     * Subscribes to a key with a callback to be executed when a message is received.
     *
     * @param key      The key to subscribe to
     * @param callback The callback to execute upon message receipt
     * @return A cancelable subscription
     */
    public SynchronizeSupport.Cancelable addSubscriber(String key, Runnable callback) {
        if (isShutdown) {
            throw new IllegalStateException("Subscriber has been shutdown");
        }
        Set<Runnable> callbacks = subscribers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        callbacks.add(callback);
        return () -> callbacks.remove(callback);
    }

    /**
     * Shuts down the subscriber, unsubscribing from the Redis channel.
     */
    @Override
    public void close() {
        isShutdown = true;
        listener.unsubscribe(channelId);
    }

    /**
     * Starts the subscription thread to listen for messages on the specified Redis channel.
     */
    private void startSubscription() {
        new Thread(this::subscribeToChannel).start();
    }

    /**
     * Handles the subscription logic, including retrying upon failure.
     */
    private void subscribeToChannel() {
        while (!isShutdown) {
            try {
                jedis.subscribe(listener, channelId);
            } catch (Exception e) {
                logger.warn("Subscription error, retrying...", e);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(retryIntervalMillis));
            }
        }
    }

    /**
     * Inner class that extends JedisPubSub to handle Redis messages and subscriptions.
     */
    private class SubscriberListener extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            Set<Runnable> callbacks = subscribers.getOrDefault(message, Collections.emptySet());
            for (Runnable callback : callbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.error("Error executing callback {}", callback, e);
                }
            }
        }
    }
}
