package com.example.s3;

import com.example.base.LocalStackBase;
import com.example.service.S3Service;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for S3Service against a real LocalStack container.
 *
 * Each test gets a fresh bucket (random name) created in @BeforeEach and
 * cleaned up in @AfterEach so tests are fully isolated.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class S3LocalStackTest extends LocalStackBase {

    private S3Client s3Client;
    private S3Service s3Service;
    private String bucket;

    @BeforeEach
    void setUp() {
        s3Client  = s3Client();
        s3Service = new S3Service(s3Client);
        bucket    = "test-bucket-" + UUID.randomUUID().toString().substring(0, 8);
        s3Service.createBucket(bucket);
    }

    @AfterEach
    void tearDown() {
        s3Service.deleteBucket(bucket);
        s3Client.close();
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