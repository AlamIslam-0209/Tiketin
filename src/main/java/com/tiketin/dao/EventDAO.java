package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Event;
import com.tiketin.util.SessionManager;
import com.tiketin.util.ValidationUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EventDAO {

    private static final Set<String> ensuredImageTables = ConcurrentHashMap.newKeySet();

    public EventDAO() {}

    /**
     * Memastikan kolom image_path ada pada tabel target ('events' atau 'applications') secara idempoten.
     * Harus dipanggil sebelum transaksi dimulai agar DDL MySQL tidak melakukan commit implisit.
     */
    public static void ensureImagePathColumn(Connection conn, String tableName) throws SQLException {
        if (!"events".equals(tableName) && !"applications".equals(tableName)) {
            throw new IllegalArgumentException("Tabel tidak didukung untuk image_path: " + tableName);
        }
        if (ensuredImageTables.contains(tableName)) {
            return;
        }
        DatabaseMetaData meta = conn.getMetaData();
        boolean exists = false;
        try (ResultSet cols = meta.getColumns(conn.getCatalog(), null, tableName, "image_path")) {
            exists = cols.next();
        }
        if (!exists) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN image_path VARCHAR(255) DEFAULT NULL");
            } catch (SQLException e) {
                try (ResultSet cols = meta.getColumns(conn.getCatalog(), null, tableName, "image_path")) {
                    if (!cols.next()) {
                        throw e;
                    }
                }
            }
        }
        ensuredImageTables.add(tableName);
    }

    public List<Event> getAllPublishedEvents() {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT e.*, u.organization AS owner_org, u.full_name AS owner_name " +
                     "FROM events e LEFT JOIN users u ON e.owner_id = u.id " +
                     "ORDER BY e.date ASC";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureImagePathColumn(conn, "events");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapEvent(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get all published events: " + e.getMessage());
        }
        return list;
    }

    public List<Event> getEventsByOwner(int ownerId) {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT e.*, u.organization AS owner_org, u.full_name AS owner_name " +
                     "FROM events e LEFT JOIN users u ON e.owner_id = u.id " +
                     "WHERE e.owner_id = ? ORDER BY e.date ASC";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureImagePathColumn(conn, "events");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, ownerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapEvent(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get events by owner: " + e.getMessage());
        }
        return list;
    }

    public Event getEventById(int id) {
        String sql = "SELECT e.*, u.organization AS owner_org, u.full_name AS owner_name " +
                     "FROM events e LEFT JOIN users u ON e.owner_id = u.id " +
                     "WHERE e.id = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            ensureImagePathColumn(conn, "events");
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapEvent(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get event by id: " + e.getMessage());
        }
        return null;
    }

    public boolean createEvent(Event event) throws SQLException {
        if (event == null) {
            throw new SQLException("Data acara tidak boleh kosong.");
        }
        String name = event.getName() != null ? event.getName().trim() : "";
        String loc = event.getLocation() != null ? event.getLocation().trim() : "";
        String time = event.getTime() != null ? event.getTime().trim() : "";
        String cat = event.getCategory() != null && !event.getCategory().trim().isEmpty()
                ? event.getCategory().trim() : "Lainnya";
        String err = ValidationUtil.validateEventFields(
                name, loc, com.tiketin.util.DateUtil.parseDateSafe(event.getDate()), time,
                event.getPrice(), event.getSlots(), true);
        if (err != null) {
            throw new SQLException(err);
        }
        String imgErr = ValidationUtil.validateEventImagePath(event.getImagePath());
        if (imgErr != null) {
            throw new SQLException(imgErr);
        }

        String roleSql = "SELECT role FROM users WHERE id = ? LIMIT 1";
        String sql = "INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description, image_path) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            ensureImagePathColumn(conn, "events");
            conn.setAutoCommit(false);

            try (PreparedStatement rolePs = conn.prepareStatement(roleSql)) {
                rolePs.setInt(1, event.getOwnerId());
                try (ResultSet rs = rolePs.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Akun penyelenggara tidak ditemukan.");
                    }
                    String role = rs.getString("role");
                    if (!"organizer".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
                        throw new SQLException("Hanya Organizer atau Admin yang dapat membuat acara.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, event.getOwnerId());
                ps.setString(2, name);
                ps.setString(3, event.getDate());
                ps.setString(4, time);
                ps.setString(5, loc);
                ps.setInt(6, event.getPrice());
                ps.setInt(7, event.getSlots());
                ps.setString(8, cat);
                ps.setString(9, event.getDescription());
                ps.setString(10, event.getImagePath().trim());

                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) {
                            event.setId(gk.getInt(1));
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

    public boolean updateEvent(Event event) throws SQLException {
        int actorId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        return updateEvent(event, actorId);
    }

    public boolean updateEvent(Event event, int actorUserId) throws SQLException {
        if (event == null || event.getId() <= 0) {
            throw new SQLException("Acara tidak ditemukan.");
        }
        if (actorUserId <= 0) {
            throw new SQLException("Autentikasi pengguna diperlukan untuk mengubah acara.");
        }
        String name = event.getName() != null ? event.getName().trim() : "";
        String loc = event.getLocation() != null ? event.getLocation().trim() : "";
        String time = event.getTime() != null ? event.getTime().trim() : "";
        String cat = event.getCategory() != null && !event.getCategory().trim().isEmpty()
                ? event.getCategory().trim() : "Lainnya";
        String err = ValidationUtil.validateEventFields(
                name, loc, com.tiketin.util.DateUtil.parseDateSafe(event.getDate()), time,
                event.getPrice(), event.getSlots(), false);
        if (err != null) {
            throw new SQLException(err);
        }
        String imgErr = ValidationUtil.validateEventImagePath(event.getImagePath());
        if (imgErr != null) {
            throw new SQLException(imgErr);
        }

        String actorSql = "SELECT role FROM users WHERE id = ? LIMIT 1";
        String ownerSql = "SELECT owner_id FROM events WHERE id = ? LIMIT 1";
        String sql = "UPDATE events SET name = ?, date = ?, time = ?, location = ?, price = ?, slots = ?, category = ?, description = ?, image_path = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            ensureImagePathColumn(conn, "events");
            conn.setAutoCommit(false);

            String actorRole;
            try (PreparedStatement aps = conn.prepareStatement(actorSql)) {
                aps.setInt(1, actorUserId);
                try (ResultSet rs = aps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Akun pengguna tidak ditemukan.");
                    }
                    actorRole = rs.getString("role");
                }
            }

            try (PreparedStatement ops = conn.prepareStatement(ownerSql)) {
                ops.setInt(1, event.getId());
                try (ResultSet rs = ops.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Acara tidak ditemukan.");
                    }
                    int ownerId = rs.getInt("owner_id");
                    if (!"admin".equalsIgnoreCase(actorRole) && ownerId != actorUserId) {
                        throw new SQLException("Anda tidak memiliki wewenang mengubah acara ini.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, event.getDate());
                ps.setString(3, time);
                ps.setString(4, loc);
                ps.setInt(5, event.getPrice());
                ps.setInt(6, event.getSlots());
                ps.setString(7, cat);
                ps.setString(8, event.getDescription());
                ps.setString(9, event.getImagePath().trim());
                ps.setInt(10, event.getId());
                if (ps.executeUpdate() > 0) {
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

    public boolean deleteEvent(int eventId) throws SQLException {
        int actorId = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : -1;
        return deleteEvent(eventId, actorId);
    }

    public boolean deleteEvent(int eventId, int actorUserId) throws SQLException {
        if (actorUserId <= 0) {
            throw new SQLException("Autentikasi pengguna diperlukan untuk menghapus acara.");
        }
        String actorSql = "SELECT role FROM users WHERE id = ? LIMIT 1";
        String ownerSql = "SELECT owner_id FROM events WHERE id = ? LIMIT 1";
        String activeOrdersSql = "SELECT COUNT(*) AS cnt FROM orders WHERE event_id = ? AND status IN ('menunggu', 'berhasil')";
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection()) {
            String actorRole;
            try (PreparedStatement aps = conn.prepareStatement(actorSql)) {
                aps.setInt(1, actorUserId);
                try (ResultSet rs = aps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Akun pengguna tidak ditemukan.");
                    }
                    actorRole = rs.getString("role");
                }
            }

            try (PreparedStatement ops = conn.prepareStatement(ownerSql)) {
                ops.setInt(1, eventId);
                try (ResultSet rs = ops.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Acara tidak ditemukan.");
                    }
                    int ownerId = rs.getInt("owner_id");
                    if (!"admin".equalsIgnoreCase(actorRole) && ownerId != actorUserId) {
                        throw new SQLException("Anda tidak memiliki wewenang menghapus acara ini.");
                    }
                }
            }

            try (PreparedStatement cps = conn.prepareStatement(activeOrdersSql)) {
                cps.setInt(1, eventId);
                try (ResultSet rs = cps.executeQuery()) {
                    if (rs.next() && rs.getInt("cnt") > 0) {
                        throw new SQLException("Acara tidak dapat dihapus karena masih memiliki "
                                + rs.getInt("cnt") + " pesanan menunggu/aktif.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        Event evt = new Event();
        evt.setId(rs.getInt("id"));
        evt.setOwnerId(rs.getInt("owner_id"));
        evt.setName(rs.getString("name"));
        evt.setDate(rs.getString("date"));
        evt.setTime(rs.getString("time"));
        evt.setLocation(rs.getString("location"));
        evt.setPrice(rs.getInt("price"));
        evt.setSlots(rs.getInt("slots"));
        evt.setCategory(rs.getString("category"));
        evt.setDescription(rs.getString("description"));
        try {
            evt.setCreatedAt(rs.getString("created_at"));
        } catch (SQLException ignored) {}
        try {
            evt.setOwnerName(rs.getString("owner_name"));
        } catch (SQLException ignored) {}
        try {
            evt.setOwnerOrg(rs.getString("owner_org"));
        } catch (SQLException ignored) {}
        try {
            evt.setImagePath(rs.getString("image_path"));
        } catch (SQLException ignored) {}
        return evt;
    }
}
