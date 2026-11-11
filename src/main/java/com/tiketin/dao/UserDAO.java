package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDAO() {}

    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    if (dbPass != null && dbPass.equals(password)) {
                        return mapUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal login: " + e.getMessage());
        }
        return null;
    }

    public User register(String username, String password, String fullName) throws SQLException {
        String checkSql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        String insertSql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'user', ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, username);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Username sudah dipakai oleh pengguna lain.");
                    }
                }
            }

            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setString(1, username);
                insertPs.setString(2, password);
                insertPs.setString(3, fullName);
                insertPs.executeUpdate();

                try (ResultSet gk = insertPs.getGeneratedKeys()) {
                    if (gk.next()) {
                        int id = gk.getInt(1);
                        return new User(id, username, password, "user", fullName, null, null);
                    }
                }
            }
        }
        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get user by id: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal get all users: " + e.getMessage());
        }
        return list;
    }

    public boolean updateRoleAndOrg(int userId, String role, String fullName, String organization) {
        String sql = "UPDATE users SET role = ?, full_name = ?, organization = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, fullName);
            ps.setString(3, organization);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Gagal update role and org: " + e.getMessage());
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setOrganization(rs.getString("organization"));
        try {
            u.setCreatedAt(rs.getString("created_at"));
        } catch (SQLException ignored) {}
        return u;
    }
}
