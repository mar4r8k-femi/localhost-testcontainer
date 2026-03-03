# LocalStack + Testcontainers Integration Tests

Java integration test suite wired to the Harness CI pipeline using
Testcontainers Cloud (TCC) as the container runtime.

## Project Structure

```
src/
  main/java/com/example/
    model/       OrderItem.java          # DynamoDB entity
    service/     S3Service.java          # S3 wrapper
                 SqsService.java         # SQS wrapper
                 DynamoDbService.java    # DynamoDB wrapper (enhanced client)
  test/java/com/example/
    base/        LocalStackBase.java     # shared container + client factories
    s3/          S3LocalStackTest.java
    sqs/         SqsLocalStackTest.java
    dynamodb/    DynamoLocalStackTest.java
```

## Running locally

```bash
# Requires Docker running locally (or a TCC account)
./mvnw test

# Run only LocalStack tests (matches the Harness pipeline filter)
./mvnw test -Dtest="**/*LocalStack*,**/*S3*,**/*SQS*,**/*Dynamo*"
```

## Environment variables (set by Harness)

| Variable            | Purpose                                  | Default                     |
|---------------------|------------------------------------------|-----------------------------|
| `LOCALSTACK_IMAGE`  | LocalStack Docker image to pull          | `localstack/localstack:3.0` |
| `TC_CLOUD_TOKEN`    | Testcontainers Cloud auth token          | (required in CI)            |
| `AWS_DEFAULT_REGION`| AWS region for LocalStack               | `us-east-1`                 |

## Services tested

| AWS Service | Tests                          |
|-------------|--------------------------------|
| S3          | CRUD objects, bucket lifecycle |
| SQS         | Send/receive/delete messages   |
| DynamoDB    | Put/get/query/update/delete    |
