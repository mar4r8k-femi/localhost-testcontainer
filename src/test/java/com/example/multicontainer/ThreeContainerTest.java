package com.example.multicontainer;

import com.example.base.LocalStackBase;
import com.example.service.PostgresService;
import com.example.service.RedisService;
import com.example.service.S3Service;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates THREE containers running simultaneously for each test method.
 *
 * Containers started in parallel before @BeforeEach, destroyed in parallel after:
 *   [1] localstack/localstack  — S3 service
 *   [2] postgres:16-alpine     — seeded users table (alice / bob / carol)
 *   [3] redis:7-alpine         — empty key/value store
 *
 * Scenario: document storage — upload a document to S3, record the owner in
 * Postgres, and cache the S3 reference in Redis for fast lookup. All three
 * containers are live and exercised within each individual test method.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ThreeContainerTest extends LocalStackBase {

    // ── Container 1 of 3: LocalStack (S3) ────────────────────────────────────
    @Container
    LocalStackContainer localstack =
        new LocalStackContainer(IMAGE).withServices(Service.S3);

    // ── Container 2 of 3: Postgres ───────────────────────────────────────────
    @Container
    @SuppressWarnings("resource")
    PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withInitScript("db/postgres-init.sql");

    // ── Container 3 of 3: Redis ───────────────────────────────────────────────
    @Container
    GenericContainer<?> redis =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private S3Client        s3Client;
    private S3Service       s3Service;
    private PostgresService postgresService;
    private RedisService    redisService;
    private Jedis           jedis;
    private String          bucket;

    @BeforeEach
    void setUp() throws Exception {
        // S3
        s3Client  = s3Client(localstack);
        s3Service = new S3Service(s3Client);
        bucket    = "docs-" + UUID.randomUUID().toString().substring(0, 8);
        s3Service.createBucket(bucket);

        // Postgres
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        postgresService = new PostgresService(ds);

        // Redis
        jedis        = new Jedis(redis.getHost(), redis.getMappedPort(6379));
        redisService = new RedisService(jedis);
    }

    @AfterEach
    void tearDown() {
        s3Client.close();
        jedis.close();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. all three containers start independently with clean state")
    void allThreeContainersStartWithCleanState() throws SQLException {
        // S3 bucket exists but is empty
        assertThat(s3Service.bucketExists(bucket)).isTrue();
        assertThat(s3Service.listObjectKeys(bucket)).isEmpty();

        // Postgres seeded with fixture users
        assertThat(postgresService.findAll()).hasSize(3);

        // Redis has no entries
        assertThat(redisService.get("any-key")).isEmpty();
    }

    @Test
    @DisplayName("2. upload document — written to S3, owner verified in Postgres, key cached in Redis")
    void uploadDocumentAcrossAllThreeStores() throws SQLException {
        String docKey   = "doc-" + UUID.randomUUID();
        byte[] content  = "{\"title\":\"quarterly-report\"}".getBytes(StandardCharsets.UTF_8);
        String cacheKey = "s3-ref:" + docKey;

        // Write to S3
        s3Service.putObject(bucket, docKey, content, "application/json");

        // Owner is alice — look her up in Postgres to confirm she exists
        String ownerEmail = postgresService.findByUsername("alice")
            .map(row -> row[1])
            .orElseThrow();

        // Cache the S3 reference in Redis
        redisService.set(cacheKey, bucket + "/" + docKey);

        assertThat(s3Service.listObjectKeys(bucket)).contains(docKey);
        assertThat(ownerEmail).isEqualTo("alice@example.com");
        assertThat(redisService.get(cacheKey)).hasValue(bucket + "/" + docKey);
    }

    @Test
    @DisplayName("3. retrieve document — content from S3, owner from Postgres, ref from Redis cache")
    void retrieveDocumentFromAllThreeStores() throws SQLException {
        String docKey   = "doc-" + UUID.randomUUID();
        byte[] content  = "report-content".getBytes(StandardCharsets.UTF_8);
        String cacheKey = "s3-ref:" + docKey;

        s3Service.putObject(bucket, docKey, content, "text/plain");
        redisService.set(cacheKey, bucket + "/" + docKey);

        // Read document content from S3
        assertThat(new String(s3Service.getObject(bucket, docKey), StandardCharsets.UTF_8))
            .isEqualTo("report-content");

        // Owner from Postgres
        assertThat(postgresService.findByUsername("bob")).isPresent();

        // S3 reference from Redis cache
        assertThat(redisService.get(cacheKey)).hasValue(bucket + "/" + docKey);
    }

    @Test
    @DisplayName("4. delete document — removed from S3 and Redis, Postgres users unaffected")
    void deleteDocumentLeavesPostgresIntact() throws SQLException {
        String docKey   = "doc-" + UUID.randomUUID();
        String cacheKey = "s3-ref:" + docKey;

        s3Service.putObject(bucket, docKey, "data".getBytes(), "text/plain");
        redisService.set(cacheKey, bucket + "/" + docKey);

        // Delete document from S3 and evict from Redis
        s3Service.deleteObject(bucket, docKey);
        redisService.delete(cacheKey);

        assertThat(s3Service.listObjectKeys(bucket)).doesNotContain(docKey);
        assertThat(redisService.get(cacheKey)).isEmpty();

        // Postgres fixture users are untouched
        assertThat(postgresService.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("5. writes to each container are fully isolated from the others")
    void writesToEachContainerAreIsolated() throws SQLException {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        String docKey   = "doc-" + UUID.randomUUID();
        String cacheKey = "key-" + UUID.randomUUID();

        // Write to each independently
        postgresService.save(username, username + "@example.com");
        s3Service.putObject(bucket, docKey, "payload".getBytes(), "text/plain");
        redisService.set(cacheKey, "cached-value");

        // Each store only contains what was written to it
        assertThat(postgresService.findByUsername(username)).isPresent();
        assertThat(postgresService.findByUsername(docKey)).isEmpty();   // not in Postgres

        assertThat(s3Service.listObjectKeys(bucket)).containsOnly(docKey);

        assertThat(redisService.get(cacheKey)).hasValue("cached-value");
        assertThat(redisService.get(docKey)).isEmpty();                 // not in Redis
    }
}
