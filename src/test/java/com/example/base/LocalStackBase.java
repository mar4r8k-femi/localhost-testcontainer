package com.example.base;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
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
 * Uses the Testcontainers Singleton pattern: the container is started once
 * via a static initializer and shared across all test classes for the life
 * of the JVM. Ryuk cleans it up on JVM exit.
 *
 * Replacing @Testcontainers + @Container with a static initializer prevents
 * the container from being stopped between test classes (the @Testcontainers
 * extension, being inherited, called afterAll per class and shut the container
 * down before the next class could use it).
 */
public abstract class LocalStackBase {

    private static final DockerImageName IMAGE = DockerImageName.parse(
        System.getenv().getOrDefault("LOCALSTACK_IMAGE", "localstack/localstack:3.0")
    );

    protected static final LocalStackContainer localstack =
        new LocalStackContainer(IMAGE)
            .withServices(Service.S3, Service.SQS, Service.DYNAMODB);

    static {
        localstack.start();
    }

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