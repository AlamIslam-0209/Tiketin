package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Application;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDAO {

    public ApplicationDAO() {}

    public Application getLatestApplicationByUser(int userId) {
        String sql = "SELECT * FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection()) {
            EventDAO.ensureImagePathColumn(conn, "applications");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapApplication(rs);
                    }
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
        try (Connection conn = DatabaseConfig.getConnection()) {
            EventDAO.ensureImagePathColumn(conn, "applications");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapApplication(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get all applications: " + e.getMessage());
        }
        return list;
    }

    public boolean submitApplication(Application app) throws SQLException {
        if (app == null) {
            throw new SQLException("Data pengajuan tidak boleh kosong.");
        }
        String fullName = app.getFullName() != null ? app.getFullName().trim() : "";
        String contact = app.getContact() != null ? app.getContact().trim() : "";
        String org = app.getOrganization() != null ? app.getOrganization().trim() : "";
        String evName = app.getEventName() != null ? app.getEventName().trim() : "";
        String evLoc = app.getEventLocation() != null ? app.getEventLocation().trim() : "";
        String evTime = app.getEventTime() != null && !app.getEventTime().trim().isEmpty()
                ? app.getEventTime().trim() : "19:00 WIB";
        String evCat = app.getEventCategory() != null && !app.getEventCategory().trim().isEmpty()
                ? app.getEventCategory().trim() : "Konser";

        String err = ValidationUtil.validateRequired(fullName, "Nama lengkap");
        if (err != null) throw new SQLException(err);
        err = ValidationUtil.validateContact(contact);
        if (err != null) throw new SQLException(err);
        err = ValidationUtil.validateRequired(org, "Nama organisasi / penyelenggara");
        if (err != null) throw new SQLException(err);
        err = ValidationUtil.validateRequired(evName, "Nama acara");
        if (err != null) throw new SQLException(err);
        err = ValidationUtil.validateRequired(evLoc, "Lokasi / venue acara");
        if (err != null) throw new SQLException(err);
        err = ValidationUtil.validateFutureSchedule(app.getEventDate(), evTime);
        if (err != null) throw new SQLException(err);
        if (app.getEventPrice() < 0) {
            throw new SQLException("Harga tiket tidak boleh kurang dari Rp 0.");
        }
        if (app.getEventSlots() < 1) {
            throw new SQLException("Kuota tiket minimal 1.");
        }
        err = ValidationUtil.validateEventImagePath(app.getImagePath());
        if (err != null) throw new SQLException(err);

        String roleSql = "SELECT role FROM users WHERE id = ? LIMIT 1";
        String checkSql = "SELECT status FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        String insertSql = "INSERT INTO applications (user_id, full_name, contact, organization, event_name, event_date, event_time, event_location, event_price, event_slots, event_category, event_description, image_path, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            EventDAO.ensureImagePathColumn(conn, "applications");
            conn.setAutoCommit(false);

            try (PreparedStatement rolePs = conn.prepareStatement(roleSql)) {
                rolePs.setInt(1, app.getUserId());
                try (ResultSet rs = rolePs.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Akun pengguna tidak ditemukan.");
                    }
                    String role = rs.getString("role");
                    if (!"user".equalsIgnoreCase(role)) {
                        throw new SQLException("Hanya akun pengguna biasa yang dapat mengajukan menjadi Organizer.");
                    }
                }
            }

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
                insertPs.setString(2, fullName);
                insertPs.setString(3, contact);
                insertPs.setString(4, org);
                insertPs.setString(5, evName);
                insertPs.setString(6, app.getEventDate());
                insertPs.setString(7, evTime);
                insertPs.setString(8, evLoc);
                insertPs.setInt(9, app.getEventPrice());
                insertPs.setInt(10, app.getEventSlots());
                insertPs.setString(11, evCat);
                insertPs.setString(12, app.getEventDescription());
                insertPs.setString(13, app.getImagePath().trim());

                int affected = insertPs.executeUpdate();
                if (affected > 0) {
                    try (ResultSet gk = insertPs.getGeneratedKeys()) {
                        if (gk.next()) {
                            app.setId(gk.getInt(1));
                        }
                    }
                    conn.commit();
                    return true;
                }
            }
            conn.rollback();
            return false;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean approveApplication(int applicationId) throws SQLException {
        int adminId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        return approveApplication(applicationId, adminId);
    }

    public boolean approveApplication(int applicationId, int adminUserId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            EventDAO.ensureImagePathColumn(conn, "applications");
            EventDAO.ensureImagePathColumn(conn, "events");
            conn.setAutoCommit(false);

            verifyAdminActor(conn, adminUserId);

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

            // 4. Terbitkan event pertama beserta foto dari pengajuan
            String insertEventSql = "INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description, image_path) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                psInEvent.setString(10, app.getImagePath());
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
        int adminId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        return rejectApplication(applicationId, adminId, reason);
    }

    public boolean rejectApplication(int applicationId, int adminUserId, String reason) throws SQLException {
        String cleanReason = reason != null ? reason.trim() : "";
        if (cleanReason.isEmpty()) {
            throw new SQLException("Alasan penolakan wajib diisi.");
        }
        String sql = "UPDATE applications SET status = 'rejected', reason = ? WHERE id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConfig.getConnection()) {
            verifyAdminActor(conn, adminUserId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanReason);
                ps.setInt(2, applicationId);
                if (ps.executeUpdate() <= 0) {
                    throw new SQLException("Pengajuan tidak ditemukan atau sudah diproses sebelumnya.");
                }
                return true;
            }
        }
    }

    private void verifyAdminActor(Connection conn, int adminUserId) throws SQLException {
        if (adminUserId <= 0) {
            throw new SQLException("Akses ditolak: autentikasi Administrator diperlukan.");
        }
        String sql = "SELECT role FROM users WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"admin".equalsIgnoreCase(rs.getString("role"))) {
                    throw new SQLException("Akses ditolak: hanya Administrator yang dapat memproses pengajuan.");
                }
            }
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
        try {
            app.setImagePath(rs.getString("image_path"));
        } catch (SQLException ignored) {}
        return app;
    }
}
