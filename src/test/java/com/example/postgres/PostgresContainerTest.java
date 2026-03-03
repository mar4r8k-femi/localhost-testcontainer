package com.example.postgres;

import com.example.service.PostgresService;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PostgresService against a real Postgres container.
 *
 * The {@code @Container} annotation on a static field gives this test class
 * its own Postgres container scoped to the class lifetime: Testcontainers
 * starts it before the first test and discards it after the last. The
 * container is pre-seeded via {@code withInitScript("db/postgres-init.sql")}.
 * Tests that write rows use UUID-based usernames so they never collide with
 * each other or with the fixture rows; no cleanup logic is written.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PostgresContainerTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withInitScript("db/postgres-init.sql");

    private PostgresService postgresService;

    @BeforeEach
    void setUp() throws Exception {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        postgresService = new PostgresService(ds);
    }

    @AfterEach
    void tearDown() {
        // No row cleanup — the container is discarded after the class.
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. findAll returns the three seeded fixture users")
    void findAllReturnsSeededUsers() throws SQLException {
        List<String[]> rows = postgresService.findAll();

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(r -> r[0])
            .containsExactlyInAnyOrder("alice", "bob", "carol");
    }

    @Test
    @DisplayName("2. findByUsername finds a fixture user")
    void findByUsernameFindsFixtureUser() throws SQLException {
        Optional<String[]> result = postgresService.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get()[1]).isEqualTo("alice@example.com");
        assertThat(result.get()[2]).isEqualTo("true");
    }

    @Test
    @DisplayName("3. save a new user and retrieve it")
    void saveAndRetrieveNewUser() throws SQLException {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);

        postgresService.save(username, username + "@example.com");

        Optional<String[]> result = postgresService.findByUsername(username);
        assertThat(result).isPresent();
        assertThat(result.get()[1]).isEqualTo(username + "@example.com");
        assertThat(result.get()[2]).isEqualTo("true");
    }

    @Test
    @DisplayName("4. deactivate sets active to false")
    void deactivateSetsActiveFalse() throws SQLException {
        String username = "deact-" + UUID.randomUUID().toString().substring(0, 8);
        postgresService.save(username, username + "@example.com");

        postgresService.deactivate(username);

        Optional<String[]> result = postgresService.findByUsername(username);
        assertThat(result).isPresent();
        assertThat(result.get()[2]).isEqualTo("false");
    }

    @Test
    @DisplayName("5. deleted user is no longer found")
    void deletedUserNotFound() throws SQLException {
        String username = "del-" + UUID.randomUUID().toString().substring(0, 8);
        postgresService.save(username, username + "@example.com");
        assertThat(postgresService.findByUsername(username)).isPresent();

        postgresService.delete(username);

        assertThat(postgresService.findByUsername(username)).isEmpty();
    }
}
