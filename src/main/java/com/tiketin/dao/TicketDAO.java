package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Ticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDAO {

    public TicketDAO() {}

    public List<Ticket> getTicketsByOrder(int orderId) {
        List<Ticket> list = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE order_id = ? ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket t = new Ticket();
                    t.setId(rs.getInt("id"));
                    t.setOrderId(rs.getInt("order_id"));
                    t.setEventId(rs.getInt("event_id"));
                    t.setTicketCode(rs.getString("ticket_code"));
                    t.setPurchasePrice(rs.getInt("purchase_price"));
                    t.setStatus(rs.getString("status"));
                    try {
                        t.setCreatedAt(rs.getString("created_at"));
                    } catch (SQLException ignored) {}
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get tickets by order: " + e.getMessage());
        }
        return list;
    }
}
