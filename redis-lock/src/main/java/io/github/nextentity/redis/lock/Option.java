package io.github.nextentity.redis.lock;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Option {

    /**
     * Default publish-subscribe release lock message topic
     */
    public static final String DEFAULT_CHANNEL_ID = "nextentity.redis.lock.event";

    /**
     * Default client ID for uniquely identifying different clients
     */
    public static final String DEFAULT_CLIENT_ID = IdGenerator.generateUniqueId();

    /**
     * Default scheduler for renewing the time-to-live (TTL) for locked keys
     */
    private static final ScheduledExecutorService DEFAULT_SCHEDULER = createDefaultScheduler();

    /**
     * Default async command executor using virtual threads
     */
    private static final Executor DEFAULT_COMMAND_ASYNC_EXECUTOR = createDefaultAsyncExecutor();

    /**
     * Publish-subscribe release lock message topic
     */
    private String channelId = DEFAULT_CHANNEL_ID;

    /**
     * Client ID for uniquely identifying different clients
     */
    private String clientId = DEFAULT_CLIENT_ID;

    /**
     * Scheduler for renewing the time-to-live (TTL) for locked keys
     */
    private ScheduledExecutorService scheduler = DEFAULT_SCHEDULER;

    /**
     * Asynchronous command executor for executing commands asynchronously
     */
    private Executor commandAsyncExecutor = DEFAULT_COMMAND_ASYNC_EXECUTOR;

    /**
     * Lock's time-to-live (TTL) in milliseconds
     */
    private long keyTimeToLive = TimeUnit.SECONDS.toMillis(60);

    /**
     * Lock renewal interval in milliseconds
     */
    private long renewalInterval = TimeUnit.SECONDS.toMillis(40);

    /**
     * Maximum try interval for loop acquisition lock
     */
    private long waitLimit = TimeUnit.SECONDS.toMillis(10);

    /**
     * Retry subscribe interval in milliseconds
     */
    private long retrySubscribeInterval = TimeUnit.SECONDS.toMillis(5);

    /**
     * Maximum release delay in milliseconds.
     * This parameter affects the priority of releasing distributed locks.
     * <p>
     * When other threads in this distributed node compete for the current lock,
     * if the current node holds the lock for more than {@code maxReleaseDelay},
     * the lock will be released immediately.
     * <p>
     * Otherwise, the lock will be released asynchronously (if there are other threads
     * in this distributed node that acquire the lock first).
     * <p>
     * The lock will be released
     * immediately when no other threads in this distributed node compete for the current lock.
     */
    private long maxReleaseDelay = TimeUnit.SECONDS.toMillis(5);

    /**
     * Factory method to create the default scheduler.
     *
     * @return a ScheduledExecutorService with a single daemon thread
     */
    private static ScheduledExecutorService createDefaultScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("lock-event-loop");
            thread.setDaemon(true); // Set as daemon thread
            return thread;
        });
    }

    /**
     * Factory method to create the default asynchronous command executor.
     *
     * @return an Executor for executing commands asynchronously using virtual threads
     */
    private static Executor createDefaultAsyncExecutor() {
        return command -> Thread.ofVirtual().name("async-command-executor").start(command);
    }
}
