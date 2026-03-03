package com.example.service;

import redis.clients.jedis.Jedis;

import java.util.Optional;

/**
 * Thin wrapper around a Jedis client exposing common Redis operations.
 * The caller owns the Jedis instance lifecycle (open before use, close after).
 */
public class RedisService {

    private final Jedis jedis;

    public RedisService(Jedis jedis) {
        this.jedis = jedis;
    }

    public void set(String key, String value) {
        jedis.set(key, value);
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(jedis.get(key));
    }

    public long delete(String key) {
        return jedis.del(key);
    }

    public boolean exists(String key) {
        return jedis.exists(key);
    }

    /** Atomically increments an integer counter stored at key. */
    public long increment(String key) {
        return jedis.incr(key);
    }

    /** Sets a TTL in seconds on an existing key. Returns true if the key exists. */
    public boolean expire(String key, long seconds) {
        return jedis.expire(key, seconds) == 1L;
    }
}
