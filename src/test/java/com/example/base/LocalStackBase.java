package com.example.base;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Shared base for all LocalStack integration tests.
 *
 * A single LocalStackContainer is started per JVM (static field), shared
 * across all subclasses via @Testcontainers. The LOCALSTACK_IMAGE env var
 * is set by the Harness pipeline so the image version is pinned in CI
 * without touching source code.
 */
@Testcontainers
public abstract class LocalStackBase {

    private static final DockerImageName IMAGE = DockerImageName.parse(
        System.getenv().getOrDefault("LOCALSTACK_IMAGE", "localstack/localstack:3.0")
    );

    @Container
    protected static final LocalStackContainer localstack =
        new LocalStackContainer(IMAGE)
            .withServices(Service.S3, Service.SQS, Service.DYNAMODB);

    // ── AWS SDK client factories ───────────────────────────────────────────

    protected static S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(Service.S3))
            .credentialsProvider(credentials())
            .region(region())
            .forcePathStyle(true) // required: LocalStack uses path-style S3 URLs
            .build();
    }

    protected static SqsClient sqsClient() {
        return SqsClient.builder()
            .endpointOverride(localstack.getEndpointOverride(Service.SQS))
            .credentialsProvider(credentials())
            .region(region())
            .build();
    }

    protected static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
            .endpointOverride(localstack.getEndpointOverride(Service.DYNAMODB))
            .credentialsProvider(credentials())
            .region(region())
            .build();
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                localstack.getAccessKey(),
                localstack.getSecretKey()
            )
        );
    }

    private static Region region() {
        return Region.of(localstack.getRegion());
    }
}