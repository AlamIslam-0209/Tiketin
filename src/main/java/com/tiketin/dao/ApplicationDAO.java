package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDAO {

    public ApplicationDAO() {}

    public Application getLatestApplicationByUser(int userId) {
        String sql = "SELECT * FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapApplication(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get latest application: " + e.getMessage());
        }
        return null;
    }

    public List<Application> getAllApplications() {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT * FROM applications ORDER BY id DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapApplication(rs));
            }
        } catch (SQLException e) {
            System.err.println("Gagal get all applications: " + e.getMessage());
        }
        return list;
    }

    public boolean submitApplication(Application app) throws SQLException {
        String checkSql = "SELECT status FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        String insertSql = "INSERT INTO applications (user_id, full_name, contact, organization, event_name, event_date, event_time, event_location, event_price, event_slots, event_category, event_description, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, app.getUserId());
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && "pending".equalsIgnoreCase(rs.getString("status"))) {
                        throw new SQLException("Anda sudah memiliki pengajuan yang sedang ditinjau.");
                    }
                }
            }

            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setInt(1, app.getUserId());
                insertPs.setString(2, app.getFullName());
                insertPs.setString(3, app.getContact());
                insertPs.setString(4, app.getOrganization());
                insertPs.setString(5, app.getEventName());
                insertPs.setString(6, app.getEventDate());
                insertPs.setString(7, app.getEventTime() != null ? app.getEventTime() : "19:00 WIB");
                insertPs.setString(8, app.getEventLocation() != null ? app.getEventLocation() : "Jakarta");
                insertPs.setInt(9, app.getEventPrice());
                insertPs.setInt(10, app.getEventSlots());
                insertPs.setString(11, app.getEventCategory() != null ? app.getEventCategory() : "Konser");
                insertPs.setString(12, app.getEventDescription());

                int affected = insertPs.executeUpdate();
                if (affected > 0) {
                    try (ResultSet gk = insertPs.getGeneratedKeys()) {
                        if (gk.next()) {
                            app.setId(gk.getInt(1));
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean approveApplication(int applicationId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Lock row application
            String selectSql = "SELECT * FROM applications WHERE id = ? FOR UPDATE";
            Application app = null;
            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setInt(1, applicationId);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        app = mapApplication(rs);
                    }
                }
            }

            if (app == null || !"pending".equalsIgnoreCase(app.getStatus())) {
                throw new SQLException("Pengajuan tidak ditemukan atau sudah diproses.");
            }

            // 2. Update status application
            String updateAppSql = "UPDATE applications SET status = 'approved', reason = NULL WHERE id = ?";
            try (PreparedStatement psUpApp = conn.prepareStatement(updateAppSql)) {
                psUpApp.setInt(1, applicationId);
                psUpApp.executeUpdate();
            }

            // 3. Update role user ke organizer
            String updateUserSql = "UPDATE users SET role = 'organizer', full_name = ?, organization = ? WHERE id = ?";
            try (PreparedStatement psUpUser = conn.prepareStatement(updateUserSql)) {
                psUpUser.setString(1, app.getFullName());
                psUpUser.setString(2, app.getOrganization());
                psUpUser.setInt(3, app.getUserId());
                psUpUser.executeUpdate();
            }

            // 4. Terbitkan event pertama
            String insertEventSql = "INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psInEvent = conn.prepareStatement(insertEventSql)) {
                psInEvent.setInt(1, app.getUserId());
                psInEvent.setString(2, app.getEventName());
                psInEvent.setString(3, app.getEventDate());
                psInEvent.setString(4, app.getEventTime() != null ? app.getEventTime() : "19:00 WIB");
                psInEvent.setString(5, app.getEventLocation() != null ? app.getEventLocation() : "Jakarta");
                psInEvent.setInt(6, app.getEventPrice());
                psInEvent.setInt(7, app.getEventSlots());
                psInEvent.setString(8, app.getEventCategory() != null ? app.getEventCategory() : "Konser");
                psInEvent.setString(9, app.getEventDescription());
                psInEvent.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public boolean rejectApplication(int applicationId, String reason) throws SQLException {
        String sql = "UPDATE applications SET status = 'rejected', reason = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, applicationId);
            return ps.executeUpdate() > 0;
        }
    }

    private Application mapApplication(ResultSet rs) throws SQLException {
        Application app = new Application();
        app.setId(rs.getInt("id"));
        app.setUserId(rs.getInt("user_id"));
        app.setFullName(rs.getString("full_name"));
        app.setContact(rs.getString("contact"));
        app.setOrganization(rs.getString("organization"));
        app.setEventName(rs.getString("event_name"));
        app.setEventDate(rs.getString("event_date"));
        app.setEventTime(rs.getString("event_time"));
        app.setEventLocation(rs.getString("event_location"));
        app.setEventPrice(rs.getInt("event_price"));
        app.setEventSlots(rs.getInt("event_slots"));
        app.setEventCategory(rs.getString("event_category"));
        app.setEventDescription(rs.getString("event_description"));
        app.setStatus(rs.getString("status"));
        app.setReason(rs.getString("reason"));
        try {
            app.setCreatedAt(rs.getString("created_at"));
        } catch (SQLException ignored) {}
        return app;
    }
}
