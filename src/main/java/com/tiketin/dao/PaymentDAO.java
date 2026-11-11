package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public PaymentDAO() {}

    public List<Payment> getPaymentsByOrganizer(int organizerId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payment WHERE user_id = ? AND is_active = 1 ORDER BY id ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, organizerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setUserId(rs.getInt("user_id"));
                    p.setBankName(rs.getString("bank_name"));
                    p.setAccountNumber(rs.getString("account_number"));
                    p.setAccountName(rs.getString("account_name"));
                    p.setActive(rs.getInt("is_active") == 1);
                    try {
                        p.setCreatedAt(rs.getString("created_at"));
                    } catch (SQLException ignored) {}
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get payments by organizer: " + e.getMessage());
        }
        return list;
    }

    public boolean addPayment(int organizerId, String bankName, String accountNumber, String accountName) throws SQLException {
        String sql = "INSERT INTO payment (user_id, bank_name, account_number, account_name, is_active) VALUES (?, ?, ?, ?, 1)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, organizerId);
            ps.setString(2, bankName);
            ps.setString(3, accountNumber);
            ps.setString(4, accountName);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletePayment(int paymentId, int organizerId) throws SQLException {
        String sql = "DELETE FROM payment WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            ps.setInt(2, organizerId);
            return ps.executeUpdate() > 0;
        }
    }
}
