package com.example.redis;

import com.example.service.RedisService;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RedisService against a real Redis container.
 *
 * The {@code @Container} annotation on an instance field gives each test
 * method its own Redis container: Testcontainers starts it before
 * {@code @BeforeEach} and destroys it after the test completes.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RedisContainerTest {

    @Container
    GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private Jedis jedis;
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        jedis        = new Jedis(redis.getHost(), redis.getMappedPort(6379));
        redisService = new RedisService(jedis);
    }

    @AfterEach
    void tearDown() {
        jedis.close();
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. set and get a value round-trip")
    void setAndGetRoundTrip() {
        String key = "key:" + UUID.randomUUID();

        redisService.set(key, "hello-redis");

        assertThat(redisService.get(key)).hasValue("hello-redis");
    }

    @Test
    @DisplayName("2. non-existent key returns empty Optional")
    void nonExistentKeyReturnsEmpty() {
        assertThat(redisService.get("no-such-key:" + UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("3. deleted key no longer exists")
    void deletedKeyNoLongerExists() {
        String key = "key:" + UUID.randomUUID();
        redisService.set(key, "temp");
        assertThat(redisService.exists(key)).isTrue();

        redisService.delete(key);

        assertThat(redisService.exists(key)).isFalse();
    }

    @Test
    @DisplayName("4. increment builds a counter atomically")
    void incrementBuildsCounter() {
        String key = "counter:" + UUID.randomUUID();

        assertThat(redisService.increment(key)).isEqualTo(1L);
        assertThat(redisService.increment(key)).isEqualTo(2L);
        assertThat(redisService.increment(key)).isEqualTo(3L);
    }

    @Test
    @DisplayName("5. expire marks a key with a TTL")
    void expireMarksKeyWithTtl() {
        String key = "ttl-key:" + UUID.randomUUID();
        redisService.set(key, "ephemeral");

        boolean applied = redisService.expire(key, 60);

        assertThat(applied).isTrue();
        assertThat(redisService.exists(key)).isTrue();
    }
}
