package com.example.multicontainer;

import com.example.service.PostgresService;
import com.example.service.RedisService;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates TWO containers running simultaneously for each test method.
 *
 * Containers started in parallel before @BeforeEach, destroyed in parallel after:
 *   [1] postgres:16-alpine  — seeded users table (alice / bob / carol)
 *   [2] redis:7-alpine      — empty key/value store
 *
 * Scenario: read-through cache — check Redis first; on a miss, fall back to
 * Postgres and warm the cache. On write, update both stores together.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TwoContainerTest {

    // ── Container 1 of 2: Postgres ────────────────────────────────────────────
    @Container
    @SuppressWarnings("resource")
    PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withInitScript("db/postgres-init.sql");

    // ── Container 2 of 2: Redis ───────────────────────────────────────────────
    @Container
    GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private PostgresService postgresService;
    private RedisService    redisService;
    private Jedis           jedis;

    @BeforeEach
    void setUp() throws Exception {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        postgresService = new PostgresService(ds);

        jedis        = new Jedis(redis.getHost(), redis.getMappedPort(6379));
        redisService = new RedisService(jedis);
    }

    @AfterEach
    void tearDown() {
        jedis.close();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. cache miss — Redis empty, falls back to Postgres, then warms cache")
    void cacheMissFallsBackToPostgresAndWarmsCache() throws SQLException {
        String cacheKey = "user:alice";

        // Fresh container — cache is empty
        assertThat(redisService.get(cacheKey)).isEmpty();

        // Fall back to Postgres
        String email = postgresService.findByUsername("alice")
            .map(row -> row[1])
            .orElseThrow();

        // Warm the cache
        redisService.set(cacheKey, email);

        assertThat(redisService.get(cacheKey)).hasValue("alice@example.com");
    }

    @Test
    @DisplayName("2. cache hit — value served from Redis, Postgres not needed")
    void cacheHitServedFromRedis() {
        String cacheKey = "user:bob:" + UUID.randomUUID();
        redisService.set(cacheKey, "bob@example.com");

        assertThat(redisService.get(cacheKey)).hasValue("bob@example.com");
    }

    @Test
    @DisplayName("3. write-through — new user saved to Postgres and cached in Redis")
    void writeThroughPopulatesBothStores() throws SQLException {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String email    = username + "@example.com";
        String cacheKey = "user:" + username;

        postgresService.save(username, email);
        redisService.set(cacheKey, email);

        assertThat(postgresService.findByUsername(username))
            .isPresent()
            .hasValueSatisfying(row -> assertThat(row[1]).isEqualTo(email));
        assertThat(redisService.get(cacheKey)).hasValue(email);
    }

    @Test
    @DisplayName("4. cache eviction — after Redis delete, Postgres remains authoritative")
    void cacheEvictionLeavesPostgresIntact() throws SQLException {
        String username = "evict-" + UUID.randomUUID().toString().substring(0, 8);
        String email    = username + "@example.com";
        String cacheKey = "user:" + username;

        postgresService.save(username, email);
        redisService.set(cacheKey, email);

        // Evict from cache only
        redisService.delete(cacheKey);

        assertThat(redisService.get(cacheKey)).isEmpty();
        assertThat(postgresService.findByUsername(username)).isPresent();
    }
}
