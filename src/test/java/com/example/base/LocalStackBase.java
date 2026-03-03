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
 * Utility base for LocalStack integration tests.
 *
 * Does NOT manage container lifecycle. Each subclass declares its own
 * {@code @Container static LocalStackContainer} field and annotates the class
 * with {@code @Testcontainers}. Testcontainers' JUnit 5 extension then starts
 * the container before the first test in the class and stops (discards) it
 * after the last — CI is entirely unaware of any of this.
 *
 * This class only exposes the shared image name and parameterised AWS client
 * factory methods so subclasses don't repeat boilerplate.
 */
public abstract class LocalStackBase {

    public static final DockerImageName IMAGE = DockerImageName.parse(
        System.getenv().getOrDefault("LOCALSTACK_IMAGE", "localstack/localstack:3.0")
    );

    // ── AWS SDK client factories ───────────────────────────────────────────

    protected static S3Client s3Client(LocalStackContainer container) {
        return S3Client.builder()
            .endpointOverride(container.getEndpointOverride(Service.S3))
            .credentialsProvider(credentials(container))
            .region(region(container))
            .forcePathStyle(true) // required: LocalStack uses path-style S3 URLs
            .build();
    }

    protected static SqsClient sqsClient(LocalStackContainer container) {
        return SqsClient.builder()
            .endpointOverride(container.getEndpointOverride(Service.SQS))
            .credentialsProvider(credentials(container))
            .region(region(container))
            .build();
    }

    protected static DynamoDbClient dynamoDbClient(LocalStackContainer container) {
        return DynamoDbClient.builder()
            .endpointOverride(container.getEndpointOverride(Service.DYNAMODB))
            .credentialsProvider(credentials(container))
            .region(region(container))
            .build();
    }

    private static StaticCredentialsProvider credentials(LocalStackContainer container) {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                container.getAccessKey(),
                container.getSecretKey()
            )
        );
    }

    private static Region region(LocalStackContainer container) {
        return Region.of(container.getRegion());
    }
}
