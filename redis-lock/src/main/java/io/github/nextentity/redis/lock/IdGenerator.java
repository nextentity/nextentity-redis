package io.github.nextentity.redis.lock;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Utility class for generating unique IDs.
 */
public class IdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a unique ID.
     *
     * @return A unique ID as a String
     */
    public static @NotNull String generateUniqueId() {
        byte[] id = new byte[20];
        SECURE_RANDOM.nextBytes(id);
        ByteBuffer.wrap(id).putLong(System.currentTimeMillis());
        return new BigInteger(id).toString(Character.MAX_RADIX);
    }
}
