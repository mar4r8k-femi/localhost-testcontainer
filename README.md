# Testcontainers Integration Tests

Java integration test suite wired to the Harness CI pipeline. Tests cover
Redis, MySQL, Postgres, and AWS services (S3, SQS, DynamoDB) simulated via
LocalStack. The pipeline runs in two variants: local Docker on the Harness CI
VM and Testcontainers Cloud (TCC).

## Container design

Each container is scoped to a single test method, not a test class:

- `@Testcontainers` + `@Container` on a non-static (instance) field means the
  Testcontainers JUnit 5 extension starts a fresh container before each test
  method and destroys it immediately after.
- No container is shared across test methods or test classes. Each test starts
  from a clean slate — no teardown or state-restoration logic is needed.
- The CI system is completely unaware of any of this; Testcontainers handles
  Docker socket calls, port binding, and teardown automatically.

This per-method lifecycle is intentional: test isolation is guaranteed by
discarding the container rather than resetting its state.

AWS service tests use LocalStack as the container image — LocalStack simulates
S3, SQS, and DynamoDB locally. It is not the only thing being tested; Redis,
MySQL, and Postgres each run in their own native containers.

## Project structure

```
build.gradle            # Gradle build: dependencies, BOM imports, test config
settings.gradle         # project name
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
    s3/          S3LocalStackTest.java      # per-method LocalStack (S3)
    sqs/         SqsLocalStackTest.java     # per-method LocalStack (SQS)
    dynamodb/    DynamoLocalStackTest.java  # per-method LocalStack (DynamoDB)
    redis/       RedisContainerTest.java    # per-method Redis container
    mysql/       MySqlContainerTest.java    # per-method MySQL container
    postgres/    PostgresContainerTest.java # per-method Postgres container
    multicontainer/
                 TwoContainerTest.java      # 2 simultaneous: Postgres + Redis
                 ThreeContainerTest.java    # 3 simultaneous: LocalStack (S3) + Postgres + Redis
  test/resources/
    db/          mysql-init.sql          # products table + 3 fixture rows
                 postgres-init.sql       # users table + 3 fixture rows
scripts/
  check-prereqs.sh            # verify Docker, Java 17+, Gradle are available
  run-all-tests.sh            # run full suite
  run-s3-tests.sh             # run S3 tests only
  run-sqs-tests.sh            # run SQS tests only
  run-dynamo-tests.sh         # run DynamoDB tests only
  run-redis-tests.sh          # run Redis tests only
  run-mysql-tests.sh          # run MySQL tests only
  run-postgres-tests.sh       # run Postgres tests only
  run-multicontainer-tests.sh # run 2- and 3-container simultaneous tests
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
./scripts/run-multicontainer-tests.sh

# Or invoke Gradle directly
gradle test
gradle test --tests "com.example.s3.S3LocalStackTest.*"
gradle test --tests "com.example.sqs.SqsLocalStackTest.*"
gradle test --tests "com.example.dynamodb.DynamoLocalStackTest.*"
gradle test --tests "com.example.redis.RedisContainerTest.*"
gradle test --tests "com.example.mysql.MySqlContainerTest.*"
gradle test --tests "com.example.postgres.PostgresContainerTest.*"
gradle test --tests "com.example.multicontainer.*"
```

## CI pipeline variants

The Harness pipeline ([.harness/main-pipeline.yaml](.harness/main-pipeline.yaml)) runs both variants on every build:

|                   | Variant A: Local Docker                  | Variant B: Testcontainers Cloud          |
|-------------------|------------------------------------------|------------------------------------------|
| Docker location   | Harness Cloud VM (local socket)          | Testcontainers Cloud (remote)            |
| TCC agent         | None                                     | Background daemon on port 42145          |
| `DOCKER_HOST`     | Not set (auto-detects `/var/run/docker.sock`) | Parsed from `~/.testcontainers.properties` |
| `TC_CLOUD_TOKEN`  | Not needed                               | Required secret                          |

### Variant A: use cases

Variant A runs three step groups sequentially. Each group has a **Watch Container Events**
background step (`docker events`) that streams container lifecycle events
(`create → start → stop → destroy`) in real time, making the per-method container
churn visible in the Harness step log.

| Step group | Test class | Containers per test method | Scenario |
|---|---|---|---|
| UC1: Async Messaging (SQS) | `SqsLocalStackTest` | 1 — LocalStack | SQS send / receive / delete |
| UC2: Read-Through Cache | `TwoContainerTest` | 2 — Postgres + Redis | Cache miss → Postgres fallback → warm Redis |
| UC3: Document Storage | `ThreeContainerTest` | 3 — LocalStack (S3) + Postgres + Redis | Upload to S3, owner in Postgres, ref cached in Redis |

### Variant B: Testcontainers Cloud

The TCC agent runs as a background step, rewrites the Docker socket, and proxies all
`docker` calls to Testcontainers Cloud. The CI VM is a pure orchestrator — no containers
run locally. Steps: **TCC Agent Daemon → Verify Agent Ready → Run Integration Tests**
(full suite).

## Environment variables

| Variable             | Purpose                         | Default                     |
|----------------------|---------------------------------|-----------------------------|
| `LOCALSTACK_IMAGE`   | LocalStack Docker image to pull | `localstack/localstack:3.0` |
| `TC_CLOUD_TOKEN`     | Testcontainers Cloud auth token | (required in CI)            |
| `AWS_DEFAULT_REGION` | AWS region for LocalStack       | `us-east-1`                 |

Redis, MySQL, and Postgres images are pinned directly in each test class.

## Services tested

**Single-container tests** (one container per test method):

| Service  | Container image       | Tests                              |
|----------|-----------------------|------------------------------------|
| S3       | localstack/localstack | Upload, download, list, delete objects |
| SQS      | localstack/localstack | Send/receive/delete messages       |
| DynamoDB | localstack/localstack | Put/get/query/update/delete        |
| Redis    | redis:7-alpine        | Get/set, delete, increment, expire |
| MySQL    | mysql:8               | CRUD on pre-seeded products table  |
| Postgres | postgres:16-alpine    | CRUD on pre-seeded users table     |

**Multi-container tests** (all containers start in parallel per test method):

| Test class          | Containers                              | Scenario                                   |
|---------------------|-----------------------------------------|--------------------------------------------|
| `TwoContainerTest`  | Postgres + Redis                        | Read-through cache: miss → Postgres → warm Redis; write-through to both |
| `ThreeContainerTest`| LocalStack (S3) + Postgres + Redis      | Document storage: upload to S3, owner in Postgres, S3 ref cached in Redis |
