package com.example.mysql;

import com.example.service.MySqlService;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MySqlService against a real MySQL container.
 *
 * The {@code @Container} annotation on an instance field gives each test
 * method its own MySQL container: Testcontainers starts it (running
 * {@code withInitScript} to seed fixture data) before {@code @BeforeEach}
 * and destroys it after the test completes.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.DisplayName.class)
class MySqlContainerTest {

    @Container
    @SuppressWarnings("resource")
    MySQLContainer<?> mysql =
        new MySQLContainer<>("mysql:8")
            .withDatabaseName("testdb")
            .withInitScript("db/mysql-init.sql");

    private MySqlService mySqlService;
    private MysqlDataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new MysqlDataSource();
        dataSource.setURL(mysql.getJdbcUrl());
        dataSource.setUser(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        mySqlService = new MySqlService(dataSource);
    }

    @AfterEach
    void tearDown() {
        // Container is destroyed by Testcontainers after each test.
        // DataSource is lightweight (no pool); nothing to close.
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("1. findAll returns the three seeded fixture rows")
    void findAllReturnsSeededRows() throws SQLException {
        List<String[]> rows = mySqlService.findAll();

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(r -> r[0])
            .containsExactlyInAnyOrder("SKU-001", "SKU-002", "SKU-003");
    }

    @Test
    @DisplayName("2. findBySku finds a fixture product")
    void findBySkuFindsFixtureProduct() throws SQLException {
        Optional<String[]> result = mySqlService.findBySku("SKU-001");

        assertThat(result).isPresent();
        assertThat(result.get()[1]).isEqualTo("Widget A");
    }

    @Test
    @DisplayName("3. save a new product and retrieve it")
    void saveAndRetrieveNewProduct() throws SQLException {
        String sku = "TEST-" + UUID.randomUUID().toString().substring(0, 8);

        mySqlService.save(sku, "Test Product", 12.50, 25);

        Optional<String[]> result = mySqlService.findBySku(sku);
        assertThat(result).isPresent();
        assertThat(result.get()[1]).isEqualTo("Test Product");
        assertThat(result.get()[2]).isEqualTo("12.50");
        assertThat(result.get()[3]).isEqualTo("25");
    }

    @Test
    @DisplayName("4. updateStock changes the stock value")
    void updateStockChangesValue() throws SQLException {
        String sku = "UPD-" + UUID.randomUUID().toString().substring(0, 8);
        mySqlService.save(sku, "Updatable", 5.00, 10);

        mySqlService.updateStock(sku, 99);

        Optional<String[]> result = mySqlService.findBySku(sku);
        assertThat(result).isPresent();
        assertThat(result.get()[3]).isEqualTo("99");
    }

    @Test
    @DisplayName("5. deleted product is no longer found")
    void deletedProductNotFound() throws SQLException {
        String sku = "DEL-" + UUID.randomUUID().toString().substring(0, 8);
        mySqlService.save(sku, "Doomed", 1.00, 1);
        assertThat(mySqlService.findBySku(sku)).isPresent();

        mySqlService.delete(sku);

        assertThat(mySqlService.findBySku(sku)).isEmpty();
    }
}
