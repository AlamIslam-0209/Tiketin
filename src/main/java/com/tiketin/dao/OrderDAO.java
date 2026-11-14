package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Order;
import com.tiketin.util.DateUtil;
import com.tiketin.util.TicketCodeGenerator;
import com.tiketin.util.ValidationUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private static volatile boolean terpakaiEnumChecked = false;

    public OrderDAO() {}

    private void ensureTerpakaiStatusSupported(Connection conn) {
        if (terpakaiEnumChecked) {
            return;
        }
        synchronized (OrderDAO.class) {
            if (terpakaiEnumChecked) {
                return;
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SHOW COLUMNS FROM orders LIKE 'status'")) {
                if (rs.next()) {
                    String type = rs.getString("Type");
                    if (type != null && !type.toLowerCase().contains("'terpakai'")) {
                        st.execute("ALTER TABLE orders MODIFY COLUMN status ENUM('menunggu','berhasil','terpakai','dibatalkan') NOT NULL DEFAULT 'menunggu'");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Info migrasi status terpakai opsional: " + e.getMessage());
            } finally {
                terpakaiEnumChecked = true;
            }
        }
    }

    /**
     * Automatically marks verified ('berhasil') orders whose event date+time has passed
     * as 'terpakai' (and individual tickets as 'used').
     */
    public void syncExpiredOrders(Connection conn) {
        ensureTerpakaiStatusSupported(conn);
        String qSql = "SELECT o.id, e.date, e.time " +
                      "FROM orders o JOIN events e ON o.event_id = e.id " +
                      "WHERE o.status = 'berhasil'";
        List<Integer> expiredOrderIds = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(qSql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String dateStr = rs.getString("date");
                String timeStr = rs.getString("time");
                if (DateUtil.isEventPassed(dateStr, timeStr)) {
                    expiredOrderIds.add(id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Info cek tiket terpakai: " + e.getMessage());
            return;
        }

        if (expiredOrderIds.isEmpty()) {
            return;
        }

        String upOrderSql = "UPDATE orders SET status = 'terpakai' WHERE id = ? AND status = 'berhasil'";
        String upTicketSql = "UPDATE tickets SET status = 'used' WHERE order_id = ? AND status = 'valid'";
        try (PreparedStatement psO = conn.prepareStatement(upOrderSql);
             PreparedStatement psT = conn.prepareStatement(upTicketSql)) {
            for (int oid : expiredOrderIds) {
                psO.setInt(1, oid);
                psO.addBatch();
                psT.setInt(1, oid);
                psT.addBatch();
            }
            psO.executeBatch();
            psT.executeBatch();
        } catch (SQLException e) {
            System.err.println("Gagal sync tiket terpakai: " + e.getMessage());
        }
    }

    public String createOrderAtomic(int userId, int eventId, int quantity, String paymentMethod, String proofFilename) throws SQLException {
        String qtyErr = ValidationUtil.validateOrderQuantity(quantity);
        if (qtyErr != null) {
            throw new SQLException(qtyErr);
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // 0. Validasi akun pemesan (Admin dilarang memesan tiket)
            String userSql = "SELECT role FROM users WHERE id = ? LIMIT 1";
            try (PreparedStatement psUser = conn.prepareStatement(userSql)) {
                psUser.setInt(1, userId);
                try (ResultSet rs = psUser.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Akun pengguna tidak ditemukan.");
                    }
                    if ("admin".equalsIgnoreCase(rs.getString("role"))) {
                        throw new SQLException("Akun Admin tidak dapat melakukan pemesanan tiket.");
                    }
                }
            }

            // 1. Lock baris event
            String lockSql = "SELECT id, owner_id, name, date, time, price, slots FROM events WHERE id = ? FOR UPDATE";
            int ownerId;
            int price;
            int currentSlots;
            String eventDate;
            String eventTime;
            try (PreparedStatement psLock = conn.prepareStatement(lockSql)) {
                psLock.setInt(1, eventId);
                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Event tidak ditemukan.");
                    }
                    ownerId = rs.getInt("owner_id");
                    eventDate = rs.getString("date");
                    eventTime = rs.getString("time");
                    price = rs.getInt("price");
                    currentSlots = rs.getInt("slots");
                }
            }

            // 2. Validasi jadwal & slot
            if (DateUtil.isEventPassed(eventDate, eventTime)) {
                throw new SQLException("Acara sudah berakhir dan tiket tidak lagi tersedia.");
            }
            if (currentSlots < quantity) {
                throw new SQLException("Slot tiket tidak mencukupi (sisa slot: " + currentSlots + ").");
            }

            // 2b. Validasi metode bayar & bukti pembayaran untuk acara berbayar
            String normalizedMethod = paymentMethod != null ? paymentMethod.trim() : "";
            String normalizedProof = proofFilename != null && !proofFilename.trim().isEmpty() ? proofFilename.trim() : null;
            if (price > 0) {
                if (normalizedMethod.isEmpty()) {
                    throw new SQLException("Metode pembayaran wajib dipilih.");
                }
                if (!normalizedMethod.toLowerCase(java.util.Locale.ROOT).contains("qris")) {
                    String payCheckSql = "SELECT id FROM payment WHERE user_id = ? AND is_active = 1 AND LOWER(bank_name) = LOWER(?) LIMIT 1";
                    try (PreparedStatement psPay = conn.prepareStatement(payCheckSql)) {
                        psPay.setInt(1, ownerId);
                        psPay.setString(2, normalizedMethod);
                        try (ResultSet rs = psPay.executeQuery()) {
                            if (!rs.next()) {
                                throw new SQLException("Metode pembayaran '" + normalizedMethod + "' tidak valid atau sudah dinonaktifkan penyelenggara.");
                            }
                        }
                    }
                }
                if (normalizedProof == null) {
                    throw new SQLException("Bukti pembayaran wajib diunggah untuk acara berbayar.");
                }
            } else if (normalizedMethod.isEmpty()) {
                normalizedMethod = "Gratis";
            }

            // 3. Kurangi kuota event
            String updateSlotSql = "UPDATE events SET slots = slots - ? WHERE id = ?";
            try (PreparedStatement psSlot = conn.prepareStatement(updateSlotSql)) {
                psSlot.setInt(1, quantity);
                psSlot.setInt(2, eventId);
                psSlot.executeUpdate();
            }

            // 4. Generate kode pesanan dan hitung total
            String orderCode = TicketCodeGenerator.generateOrderCode();
            int totalPrice = price * quantity;

            // 5. Simpan order
            String insertOrderSql = "INSERT INTO orders (order_code, user_id, event_id, quantity, unit_price, total_price, payment_method, payment_proof, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'menunggu')";
            int newOrderId;
            try (PreparedStatement psOrder = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                psOrder.setString(1, orderCode);
                psOrder.setInt(2, userId);
                psOrder.setInt(3, eventId);
                psOrder.setInt(4, quantity);
                psOrder.setInt(5, price);
                psOrder.setInt(6, totalPrice);
                psOrder.setString(7, normalizedMethod);
                psOrder.setString(8, normalizedProof);
                psOrder.executeUpdate();

                try (ResultSet gk = psOrder.getGeneratedKeys()) {
                    if (gk.next()) {
                        newOrderId = gk.getInt(1);
                    } else {
                        throw new SQLException("Gagal mendapatkan ID pesanan baru.");
                    }
                }
            }

            // 6. Simpan rincian tiket individual
            String insertTicketSql = "INSERT INTO tickets (order_id, event_id, ticket_code, purchase_price, status) VALUES (?, ?, ?, ?, 'valid')";
            try (PreparedStatement psTicket = conn.prepareStatement(insertTicketSql)) {
                for (int i = 1; i <= quantity; i++) {
                    String singleTicketCode = TicketCodeGenerator.formatSingleTicketCode(orderCode, i);
                    psTicket.setInt(1, newOrderId);
                    psTicket.setInt(2, eventId);
                    psTicket.setString(3, singleTicketCode);
                    psTicket.setInt(4, price);
                    psTicket.addBatch();
                }
                psTicket.executeBatch();
            }

            conn.commit();
            return orderCode;
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

    public List<Order> getOrdersByUser(int userId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, e.name AS event_name, e.date AS event_date, e.time AS event_time, " +
                     "       e.location AS event_location, e.category AS event_category, " +
                     "       GROUP_CONCAT(t.ticket_code ORDER BY t.id ASC SEPARATOR ', ') AS ticket_codes " +
                     "FROM orders o " +
                     "JOIN events e ON o.event_id = e.id " +
                     "LEFT JOIN tickets t ON t.order_id = o.id " +
                     "WHERE o.user_id = ? " +
                     "GROUP BY o.id " +
                     "ORDER BY o.purchased_at DESC, o.id DESC";

        try (Connection conn = DatabaseConfig.getConnection()) {
            syncExpiredOrders(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapOrder(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get orders by user: " + e.getMessage());
        }
        return list;
    }

    public List<Order> getOrdersByOrganizer(int organizerId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, u.full_name AS buyer_name, u.username AS buyer_username, " +
                     "       e.name AS event_name, e.date AS event_date, e.time AS event_time, " +
                     "       e.location AS event_location, e.category AS event_category, " +
                     "       GROUP_CONCAT(t.ticket_code ORDER BY t.id ASC SEPARATOR ', ') AS ticket_codes " +
                     "FROM orders o " +
                     "JOIN events e ON o.event_id = e.id " +
                     "JOIN users u ON o.user_id = u.id " +
                     "LEFT JOIN tickets t ON t.order_id = o.id " +
                     "WHERE e.owner_id = ? " +
                     "GROUP BY o.id " +
                     "ORDER BY o.purchased_at DESC, o.id DESC";

        try (Connection conn = DatabaseConfig.getConnection()) {
            syncExpiredOrders(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, organizerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapOrder(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get orders by organizer: " + e.getMessage());
        }
        return list;
    }

    public boolean updateOrderStatus(int orderId, int organizerId, String newStatus) throws SQLException {
        String targetStatus = newStatus != null ? newStatus.trim().toLowerCase() : "";
        if (!"berhasil".equals(targetStatus) && !"dibatalkan".equals(targetStatus)) {
            throw new SQLException("Status pesanan tujuan tidak valid.");
        }
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT o.*, e.owner_id, e.id AS evt_id " +
                              "FROM orders o " +
                              "JOIN events e ON o.event_id = e.id " +
                              "WHERE o.id = ? FOR UPDATE";

            int eventOwnerId;
            int eventId;
            int quantity;
            String oldStatus;

            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, orderId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Order tidak ditemukan.");
                    }
                    eventOwnerId = rs.getInt("owner_id");
                    eventId = rs.getInt("evt_id");
                    quantity = rs.getInt("quantity");
                    oldStatus = rs.getString("status");
                }
            }

            if (eventOwnerId != organizerId) {
                throw new SQLException("Anda tidak memiliki wewenang mengelola order ini.");
            }

            String normalizedOld = oldStatus != null ? oldStatus.trim().toLowerCase() : "";
            if (!"menunggu".equals(normalizedOld) && !"pending".equals(normalizedOld)) {
                throw new SQLException("Pesanan berstatus '" + oldStatus + "' sudah diproses dan tidak dapat diubah lagi.");
            }

            // Jika dibatalkan dan status lama masih menunggu, kembalikan slot
            if ("dibatalkan".equals(targetStatus)) {
                String restoreSql = "UPDATE events SET slots = slots + ? WHERE id = ?";
                try (PreparedStatement psRestore = conn.prepareStatement(restoreSql)) {
                    psRestore.setInt(1, quantity);
                    psRestore.setInt(2, eventId);
                    psRestore.executeUpdate();
                }
            }

            // Update status pesanan
            String updateOrderSql = "UPDATE orders SET status = ? WHERE id = ?";
            try (PreparedStatement psUpOrder = conn.prepareStatement(updateOrderSql)) {
                psUpOrder.setString(1, targetStatus);
                psUpOrder.setInt(2, orderId);
                psUpOrder.executeUpdate();
            }

            // Update status seluruh tiket
            String ticketStatus = "dibatalkan".equals(targetStatus) ? "cancelled" : "valid";
            String updateTicketSql = "UPDATE tickets SET status = ? WHERE order_id = ?";
            try (PreparedStatement psUpTicket = conn.prepareStatement(updateTicketSql)) {
                psUpTicket.setString(1, ticketStatus);
                psUpTicket.setInt(2, orderId);
                psUpTicket.executeUpdate();
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

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setOrderCode(rs.getString("order_code"));
        o.setUserId(rs.getInt("user_id"));
        o.setEventId(rs.getInt("event_id"));
        o.setQuantity(rs.getInt("quantity"));
        o.setUnitPrice(rs.getInt("unit_price"));
        o.setTotalPrice(rs.getInt("total_price"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaymentProof(rs.getString("payment_proof"));
        o.setStatus(rs.getString("status"));
        try {
            o.setPurchasedAt(rs.getString("purchased_at"));
        } catch (SQLException ignored) {}
        try {
            o.setBuyerName(rs.getString("buyer_name"));
        } catch (SQLException ignored) {}
        try {
            o.setBuyerUsername(rs.getString("buyer_username"));
        } catch (SQLException ignored) {}
        try {
            o.setEventName(rs.getString("event_name"));
        } catch (SQLException ignored) {}
        try {
            o.setEventDate(rs.getString("event_date"));
        } catch (SQLException ignored) {}
        try {
            o.setEventTime(rs.getString("event_time"));
        } catch (SQLException ignored) {}
        try {
            o.setEventLocation(rs.getString("event_location"));
        } catch (SQLException ignored) {}
        try {
            o.setEventCategory(rs.getString("event_category"));
        } catch (SQLException ignored) {}
        try {
            o.setTicketCodes(rs.getString("ticket_codes"));
        } catch (SQLException ignored) {}
        if ("berhasil".equalsIgnoreCase(o.getStatus())
                && DateUtil.isEventPassed(o.getEventDate(), o.getEventTime())) {
            o.setStatus("terpakai");
        }
        return o;
    }
}
