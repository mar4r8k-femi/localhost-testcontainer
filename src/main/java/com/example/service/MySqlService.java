package com.example.service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC service for the {@code products} table.
 *
 * The caller supplies a {@link DataSource}; this class opens and closes a
 * {@link Connection} per operation so tests can close the DataSource in
 * {@code @AfterEach} without leaving dangling connections.
 */
public class MySqlService {

    private final DataSource dataSource;

    public MySqlService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<String[]> findAll() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT sku, name, price, stock FROM products")) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("price"),
                    rs.getString("stock")
                });
            }
        }
        return rows;
    }

    public Optional<String[]> findBySku(String sku) throws SQLException {
        String sql = "SELECT sku, name, price, stock FROM products WHERE sku = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{
                        rs.getString("sku"),
                        rs.getString("name"),
                        rs.getString("price"),
                        rs.getString("stock")
                    });
                }
            }
        }
        return Optional.empty();
    }

    public void save(String sku, String name, double price, int stock) throws SQLException {
        String sql = "INSERT INTO products (sku, name, price, stock) VALUES (?, ?, ?, ?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setString(2, name);
            ps.setDouble(3, price);
            ps.setInt(4, stock);
            ps.executeUpdate();
        }
    }

    public void updateStock(String sku, int newStock) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE sku = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setString(2, sku);
            ps.executeUpdate();
        }
    }

    public void delete(String sku) throws SQLException {
        String sql = "DELETE FROM products WHERE sku = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.executeUpdate();
        }
    }
}
