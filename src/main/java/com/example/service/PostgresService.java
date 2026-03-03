package com.example.service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC service for the {@code users} table.
 *
 * The caller supplies a {@link DataSource}; this class opens and closes a
 * {@link Connection} per operation so tests can close the DataSource in
 * {@code @AfterEach} without leaving dangling connections.
 */
public class PostgresService {

    private final DataSource dataSource;

    public PostgresService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<String[]> findAll() throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT username, email, active FROM users")) {
            while (rs.next()) {
                rows.add(new String[]{
                    rs.getString("username"),
                    rs.getString("email"),
                    String.valueOf(rs.getBoolean("active"))
                });
            }
        }
        return rows;
    }

    public Optional<String[]> findByUsername(String username) throws SQLException {
        String sql = "SELECT username, email, active FROM users WHERE username = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new String[]{
                        rs.getString("username"),
                        rs.getString("email"),
                        String.valueOf(rs.getBoolean("active"))
                    });
                }
            }
        }
        return Optional.empty();
    }

    public void save(String username, String email) throws SQLException {
        String sql = "INSERT INTO users (username, email, active) VALUES (?, ?, TRUE)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }

    public void deactivate(String username) throws SQLException {
        String sql = "UPDATE users SET active = FALSE WHERE username = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public void delete(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
}
