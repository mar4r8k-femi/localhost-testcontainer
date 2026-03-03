# LocalStack + Testcontainers Integration Tests

Java integration test suite wired to the Harness CI pipeline using
Testcontainers Cloud (TCC) as the container runtime.

## Container design

Each test class owns its own container, scoped to that class:

- `@Testcontainers` + `@Container static` on each test class tells the
  Testcontainers JUnit 5 extension to start the container before the first
  test in the class and discard it after the last.
- No container is shared across classes. No cleanup or teardown logic is
  needed to restore state — the container is simply thrown away.
- The CI system is completely unaware of any of this; Testcontainers handles
  Docker socket calls, port binding, and teardown automatically.

See [docs/how-testcontainers-work.md](docs/how-testcontainers-work.md) for a
detailed explanation of the extension mechanism, port binding, Ryuk, and how
local Docker and Testcontainers Cloud are both invoked.

## Project structure

```
src/
  main/java/com/example/
    model/       OrderItem.java          # DynamoDB entity
    service/     S3Service.java          # S3 wrapper
                 SqsService.java         # SQS wrapper
                 DynamoDbService.java    # DynamoDB wrapper (enhanced client)
                 RedisService.java       # Redis wrapper (Jedis)
                 MySqlService.java       # MySQL wrapper (JDBC)
                 PostgresService.java    # Postgres wrapper (JDBC)
  test/java/com/example/
    base/        LocalStackBase.java        # IMAGE constant + AWS client factories
    s3/          S3LocalStackTest.java      # owns its own S3 container
    sqs/         SqsLocalStackTest.java     # owns its own SQS container
    dynamodb/    DynamoLocalStackTest.java  # owns its own DynamoDB container
    redis/       RedisContainerTest.java    # owns its own Redis container
    mysql/       MySqlContainerTest.java    # owns its own MySQL container
    postgres/    PostgresContainerTest.java # owns its own Postgres container
  test/resources/
    db/          mysql-init.sql          # products table + 3 fixture rows
                 postgres-init.sql       # users table + 3 fixture rows
docs/
  how-testcontainers-work.md            # extension mechanism, TCC, Ryuk
scripts/
  check-prereqs.sh      # verify Docker, Java 17+, Maven are available
  run-all-tests.sh      # run full suite
  run-s3-tests.sh       # run S3 tests only
  run-sqs-tests.sh      # run SQS tests only
  run-dynamo-tests.sh   # run DynamoDB tests only
  run-redis-tests.sh    # run Redis tests only
  run-mysql-tests.sh    # run MySQL tests only
  run-postgres-tests.sh # run Postgres tests only
```

## Running locally

```bash
# Check prerequisites first
./scripts/check-prereqs.sh

# Run the full suite
./scripts/run-all-tests.sh

# Run a single service's tests
./scripts/run-s3-tests.sh
./scripts/run-sqs-tests.sh
./scripts/run-dynamo-tests.sh
./scripts/run-redis-tests.sh
./scripts/run-mysql-tests.sh
./scripts/run-postgres-tests.sh

# Or invoke Maven directly
mvn test -B
mvn test -B -Dtest="RedisContainerTest"
mvn test -B -Dtest="MySqlContainerTest"
mvn test -B -Dtest="PostgresContainerTest"
```

## Environment variables

| Variable             | Purpose                         | Default                     |
|----------------------|---------------------------------|-----------------------------|
| `LOCALSTACK_IMAGE`   | LocalStack Docker image to pull | `localstack/localstack:3.0` |
| `TC_CLOUD_TOKEN`     | Testcontainers Cloud auth token | (required in CI)            |
| `AWS_DEFAULT_REGION` | AWS region for LocalStack       | `us-east-1`                 |

Redis, MySQL, and Postgres images are pinned directly in each test class.

## Services tested

| Service  | Container image       | Tests                              |
|----------|-----------------------|------------------------------------|
| S3       | localstack/localstack | CRUD objects, bucket lifecycle     |
| SQS      | localstack/localstack | Send/receive/delete messages       |
| DynamoDB | localstack/localstack | Put/get/query/update/delete        |
| Redis    | redis:7-alpine        | Get/set, delete, increment, expire |
| MySQL    | mysql:8               | CRUD on pre-seeded products table  |
| Postgres | postgres:16-alpine    | CRUD on pre-seeded users table     |
