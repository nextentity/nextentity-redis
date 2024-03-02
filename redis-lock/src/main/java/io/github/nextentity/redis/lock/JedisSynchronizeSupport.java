package io.github.nextentity.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.UnifiedJedis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JedisSynchronizeSupport implements SynchronizeSupport {

    private static final Logger logger = LoggerFactory.getLogger(JedisSynchronizeSupport.class);

    private final UnifiedJedis jedis;
    private final String channelId;
    private final JedisSubscriber subscribe;

    public JedisSynchronizeSupport(UnifiedJedis jedis, String channelId, long retrySubscribeInterval) {
        this.jedis = jedis;
        this.channelId = channelId;
        this.subscribe = new JedisSubscriber(jedis, channelId, retrySubscribeInterval);
    }

    @Override
    public void batchSetTimeToLive(Collection<String> keys, long milliseconds) {
        try (AbstractPipeline pipeline = jedis.pipelined()) {
            for (String key : keys) {
                pipeline.pexpire(key, milliseconds);
            }
            pipeline.sync();
        } catch (Exception e) {
            logger.error("Failed to set TTL for keys: {}", keys, e);
        }
    }

    @Override
    public boolean deleteIfValueEquals(String key, String expectedValue) {
        String script = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                 return redis.call('del', KEYS[1])
                else
                 return 0
                end""";
        try {
            Object result = jedis.eval(script, Collections.singletonList(key), Collections.singletonList(expectedValue));
            logger.debug("deleteIfValueEquals result: {}", result);
            return result.equals(1L);
        } catch (Exception e) {
            logger.warn("Error executing script: {}", script, e);
            return false;
        }
    }

    @Override
    public Long setIfAbsentOrGetRemainingTTL(String key, String value, long ttl) {
        String script = """
                if redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
                 return nil
                else
                 return redis.call('pttl', KEYS[1])
                end""";
        Object result = jedis.eval(script, Collections.singletonList(key), List.of(value, Long.toString(ttl)));
        logger.debug("setIfAbsentOrGetRemainingTTL result: {}", result);
        if (result instanceof Long) {
            return (Long) result;
        } else if (result == null) {
            logger.debug("Key set successfully");
            return null;
        } else {
            throw new IllegalStateException("Unexpected result: " + result);
        }
    }

    @Override
    public void publishKey(String key) {
        jedis.publish(channelId, key);
    }

    @Override
    public Cancelable subscribeToKey(String key, Runnable callback) {
        return subscribe.addSubscriber(key, callback);
    }

    @Override
    public void close() {
        subscribe.close();
    }
}
