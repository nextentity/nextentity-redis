package io.github.nextentity.redis.lock;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

public class RedisConfig {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 6379;
    public static final String PASSWORD = "root";

    public static JedisPooled getJedisPooled() {
        HostAndPort addr = new HostAndPort(HOST, PORT);
        JedisClientConfig config = DefaultJedisClientConfig.builder()
//                .password(PASSWORD)
                .database(1)
                .build();
        return new JedisPooled(addr, config);
    }

    public static RedisClient getRedisClient() {
        RedisURI redisUri = RedisURI.builder()
                .withHost(HOST)
                .withPort(PORT)
                .withDatabase(1)
//                .withPassword(PASSWORD.toCharArray())
                .build();
        return RedisClient.create(redisUri);
    }
}
