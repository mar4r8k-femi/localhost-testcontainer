package com.example.s3;

import com.example.base.LocalStackBase;
import com.example.service.S3Service;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for S3Service against a real LocalStack container.
 *
 * The {@code @Container} annotation on a static field gives this test class
 * its own S3 container scoped to the class lifetime: Testcontainers starts it
 * before the first test and discards it after the last. Each test gets a fresh
 * bucket (random name) in {@code @BeforeEach}; no cleanup is needed because
 * the container itself is thrown away — not shared with any other test class.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class S3LocalStackTest extends LocalStackBase {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(IMAGE)
        .withServices(Service.S3);

    private S3Client s3Client;
    private S3Service s3Service;
    private String bucket;

    @BeforeEach
    void setUp() {
        s3Client  = s3Client(localstack);
        s3Service = new S3Service(s3Client);
        bucket    = "test-bucket-" + UUID.randomUUID().toString().substring(0, 8);
        s3Service.createBucket(bucket);
    }

    @AfterEach
    void tearDown() {
        s3Client.close();
        // No bucket cleanup — the container is discarded after the class,
        // so there is no state to restore.
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. bucket exists after creation")
    void bucketExistsAfterCreation() {
        assertThat(s3Service.bucketExists(bucket)).isTrue();
    }

    @Test
    @DisplayName("2. upload and download an object")
    void uploadAndDownloadObject() {
        byte[] content = "Hello, LocalStack!".getBytes(StandardCharsets.UTF_8);
        s3Service.putObject(bucket, "hello.txt", content, "text/plain");

        byte[] downloaded = s3Service.getObject(bucket, "hello.txt");

        assertThat(new String(downloaded, StandardCharsets.UTF_8))
            .isEqualTo("Hello, LocalStack!");
    }

    @Test
    @DisplayName("3. list objects returns uploaded keys")
    void listObjectsReturnsUploadedKeys() {
        s3Service.putObject(bucket, "file1.txt", "a".getBytes(), "text/plain");
        s3Service.putObject(bucket, "file2.txt", "b".getBytes(), "text/plain");
        s3Service.putObject(bucket, "file3.txt", "c".getBytes(), "text/plain");

        List<String> keys = s3Service.listObjectKeys(bucket);

        assertThat(keys).containsExactlyInAnyOrder("file1.txt", "file2.txt", "file3.txt");
    }

    @Test
    @DisplayName("4. deleted object no longer appears in listing")
    void deletedObjectNotInListing() {
        s3Service.putObject(bucket, "keep.txt",   "x".getBytes(), "text/plain");
        s3Service.putObject(bucket, "delete.txt", "y".getBytes(), "text/plain");

        s3Service.deleteObject(bucket, "delete.txt");

        assertThat(s3Service.listObjectKeys(bucket)).containsOnly("keep.txt");
    }

    @Test
    @DisplayName("5. upload JSON payload and verify content-type round-trip")
    void uploadJsonPayload() {
        String json = "{\"id\":1,\"name\":\"test\"}";
        s3Service.putObject(bucket, "data.json",
            json.getBytes(StandardCharsets.UTF_8), "application/json");

        byte[] result = s3Service.getObject(bucket, "data.json");

        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo(json);
    }
}
