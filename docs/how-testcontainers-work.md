# How Testcontainers are invoked

Testcontainers is a Java library. There is no separate Docker Compose file,
no CI service block, no pre-started daemon. Containers are started and stopped
entirely from inside JUnit lifecycle hooks — the CI system never sees them.

## The JUnit 5 extension mechanism

Two annotations wire Testcontainers into the JUnit 5 lifecycle:

```java
@Testcontainers                          // 1. registers TestcontainersExtension
class MySqlContainerTest {

    @Container                           // 2. marks a field for lifecycle management
    static MySQLContainer<?> mysql =     //    static  → class scope
        new MySQLContainer<>("mysql:8")  //    instance → method scope
            .withInitScript("db/mysql-init.sql");
```

When JUnit runs the class, `TestcontainersExtension` scans for `@Container`
fields and drives their lifecycle automatically:

| Field modifier | Container starts | Container stops |
|----------------|-----------------|-----------------|
| `static`       | Before first `@Test` in the class | After last `@Test` in the class |
| instance       | Before each `@Test` | After each `@Test` |

All containers in this project use `static` (class scope). The container is
discarded after the class finishes — no cleanup logic is needed to restore
state between tests, because a fresh container is used for each class.

## What happens at container start

When Testcontainers starts a container it:

1. Pulls the image if not already cached locally
2. Calls the Docker socket to create and start the container
3. Binds the container's ports to random high ports on the host (`getMappedPort()`)
4. Blocks until the container passes its wait strategy (log message, HTTP
   health check, or JDBC connectivity — depending on the container type)
5. Returns control to the test's `@BeforeEach`

The test then reads the dynamically-assigned host/port from the container object:

```java
// Redis — generic container
new Jedis(redis.getHost(), redis.getMappedPort(6379))

// MySQL — typed container exposes a ready-made JDBC URL
dataSource.setURL(mysql.getJdbcUrl())   // e.g. jdbc:mysql://localhost:32891/testdb

// LocalStack — endpoint per AWS service
S3Client.builder().endpointOverride(localstack.getEndpointOverride(Service.S3))
```

The service under test never has a hardcoded port. It receives a client or
DataSource that already points at the live container.

## Ryuk — the cleanup safety net

Testcontainers starts a sidecar container called Ryuk alongside the first
container of each JVM session. Ryuk monitors all containers created during
the session and removes any that are still running when the JVM exits — even
if it exits abnormally (crash, OOM kill, Ctrl-C). This means orphaned
containers are cleaned up automatically without any teardown code.

## Locally vs Testcontainers Cloud

The test code is identical in both environments. What changes is where the
Docker socket points:

**Local:** Docker socket → local Docker daemon → containers run on your machine.

**CI (Harness + TCC):** The pipeline starts the TCC agent as a background
step before the tests run. The agent rewrites `~/.testcontainers.properties`
so the Docker socket points at `tcp://localhost:42145`. Testcontainers reads
this file at startup and routes all Docker API calls through the agent to
Testcontainers Cloud, where the containers actually run on remote
infrastructure. The CI VM is a pure orchestrator — it never runs a container
locally. The test code, `mvn test -B`, and every Testcontainers API call are
unchanged.

```
CI VM                              Testcontainers Cloud
─────────────────────────────      ──────────────────────────
mvn test -B
  └─ TestcontainersExtension
       └─ DockerClient
            └─ tcp://localhost:42145 ──► TCC Agent ──► remote Docker daemon
                                                         ├─ redis:7-alpine
                                                         ├─ mysql:8
                                                         └─ localstack/localstack
```
