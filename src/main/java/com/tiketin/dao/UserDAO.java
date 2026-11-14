package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.User;
import com.tiketin.util.ValidationUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static volatile boolean profileColumnsChecked = false;

    public UserDAO() {}

    private void ensureProfileColumns(Connection conn) {
        if (profileColumnsChecked) {
            return;
        }
        synchronized (UserDAO.class) {
            if (profileColumnsChecked) {
                return;
            }
            try {
                DatabaseMetaData meta = conn.getMetaData();
                boolean hasBio = false;
                boolean hasAvatar = false;
                try (ResultSet cols = meta.getColumns(conn.getCatalog(), null, "users", null)) {
                    while (cols.next()) {
                        String colName = cols.getString("COLUMN_NAME");
                        if ("bio".equalsIgnoreCase(colName)) hasBio = true;
                        if ("avatar_path".equalsIgnoreCase(colName)) hasAvatar = true;
                    }
                }
                try (Statement st = conn.createStatement()) {
                    if (!hasBio) {
                        st.execute("ALTER TABLE users ADD COLUMN bio TEXT NULL AFTER organization");
                    }
                    if (!hasAvatar) {
                        st.execute("ALTER TABLE users ADD COLUMN avatar_path VARCHAR(255) NULL AFTER bio");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Info migrasi kolom profil opsional: " + e.getMessage());
            } finally {
                profileColumnsChecked = true;
            }
        }
    }

    public User login(String username, String password) throws SQLException {
        String cleanUser = username != null ? username.trim() : "";
        if (cleanUser.isEmpty() || password == null || password.isEmpty()) {
            throw new SQLException("Username dan password wajib diisi.");
        }
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureProfileColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanUser);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String dbPass = rs.getString("password");
                        if (dbPass != null && dbPass.equals(password)) {
                            return mapUser(rs);
                        }
                    }
                }
            }
        }
        return null;
    }

    public User register(String username, String password, String fullName) throws SQLException {
        String cleanFull = fullName != null ? fullName.trim() : "";
        String cleanUser = username != null ? username.trim() : "";
        String fullErr = ValidationUtil.validateRequired(cleanFull, "Nama lengkap");
        if (fullErr != null) throw new SQLException(fullErr);
        String userErr = ValidationUtil.validateUsername(cleanUser);
        if (userErr != null) throw new SQLException(userErr);
        String passErr = ValidationUtil.validatePassword(password);
        if (passErr != null) throw new SQLException(passErr);

        String checkSql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";
        String insertSql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'user', ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureProfileColumns(conn);
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, cleanUser);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Username sudah dipakai oleh pengguna lain.");
                    }
                }
            }

            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setString(1, cleanUser);
                insertPs.setString(2, password);
                insertPs.setString(3, cleanFull);
                insertPs.executeUpdate();

                try (ResultSet gk = insertPs.getGeneratedKeys()) {
                    if (gk.next()) {
                        int id = gk.getInt(1);
                        return new User(id, cleanUser, password, "user", cleanFull, null, null);
                    }
                }
            }
        }
        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureProfileColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUser(rs);
                    }
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

    public User updateProfile(int userId, String newUsername, String fullName, String organization, String newPassword) throws SQLException {
        User existing = getUserById(userId);
        String existingBio = existing != null ? existing.getBio() : null;
        String existingAvatar = existing != null ? existing.getAvatarPath() : null;
        return updateProfile(userId, newUsername, fullName, organization, existingBio, existingAvatar, newPassword);
    }

    public User updateProfile(int userId, String newUsername, String fullName, String organization,
                              String bio, String avatarPath, String newPassword) throws SQLException {
        String trimmedUsername = newUsername != null ? newUsername.trim() : "";
        String trimmedFullName = fullName != null ? fullName.trim() : "";
        String safeOrg = organization != null ? organization.trim() : "";
        String safeBio = bio != null ? bio.trim() : "";
        String safeAvatar = avatarPath != null && !avatarPath.trim().isEmpty() ? avatarPath.trim() : null;

        String nameErr = ValidationUtil.validateRequired(trimmedFullName, "Nama lengkap");
        if (nameErr != null) {
            throw new SQLException(nameErr);
        }
        String userErr = ValidationUtil.validateUsername(trimmedUsername);
        if (userErr != null) {
            throw new SQLException(userErr);
        }
        boolean updatePassword = newPassword != null && !newPassword.isEmpty();
        if (updatePassword) {
            String passErr = ValidationUtil.validatePassword(newPassword);
            if (passErr != null) {
                throw new SQLException(passErr);
            }
        }

        String checkSql = "SELECT id FROM users WHERE LOWER(username) = LOWER(?) AND id != ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureProfileColumns(conn);
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, trimmedUsername);
                checkPs.setInt(2, userId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Username '" + trimmedUsername + "' sudah digunakan.");
                    }
                }
            }

            String updateSql = updatePassword
                    ? "UPDATE users SET username = ?, full_name = ?, organization = ?, bio = ?, avatar_path = ?, password = ? WHERE id = ?"
                    : "UPDATE users SET username = ?, full_name = ?, organization = ?, bio = ?, avatar_path = ? WHERE id = ?";

            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setString(1, trimmedUsername);
                updatePs.setString(2, trimmedFullName);
                updatePs.setString(3, safeOrg);
                updatePs.setString(4, safeBio);
                updatePs.setString(5, safeAvatar);
                if (updatePassword) {
                    updatePs.setString(6, newPassword);
                    updatePs.setInt(7, userId);
                } else {
                    updatePs.setInt(6, userId);
                }
                updatePs.executeUpdate();
            }
        }

        return getUserById(userId);
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
            u.setBio(rs.getString("bio"));
        } catch (SQLException ignored) {}
        try {
            u.setAvatarPath(rs.getString("avatar_path"));
        } catch (SQLException ignored) {}
        try {
            u.setCreatedAt(rs.getString("created_at"));
        } catch (SQLException ignored) {}
        return u;
    }
}
