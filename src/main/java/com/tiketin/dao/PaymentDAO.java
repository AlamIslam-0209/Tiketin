package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Payment;
import com.tiketin.util.ValidationUtil;

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
        String err = ValidationUtil.validatePaymentAccount(bankName, accountNumber, accountName);
        if (err != null) {
            throw new SQLException(err);
        }
        String cleanBank = bankName.trim();
        String cleanAccNum = ValidationUtil.normalizeAccountNumber(cleanBank, accountNumber);
        String cleanAccName = accountName.trim();

        String roleSql = "SELECT role FROM users WHERE id = ? LIMIT 1";
        String dupSql = "SELECT id FROM payment WHERE user_id = ? AND LOWER(bank_name) = LOWER(?) AND account_number = ? AND is_active = 1 LIMIT 1";
        String sql = "INSERT INTO payment (user_id, bank_name, account_number, account_name, is_active) VALUES (?, ?, ?, ?, 1)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement rolePs = conn.prepareStatement(roleSql)) {
                rolePs.setInt(1, organizerId);
                try (ResultSet rs = rolePs.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Akun pengguna tidak ditemukan.");
                    }
                    String role = rs.getString("role");
                    if (!"organizer".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
                        throw new SQLException("Hanya Organizer yang dapat menambahkan rekening pembayaran.");
                    }
                }
            }

            try (PreparedStatement dupPs = conn.prepareStatement(dupSql)) {
                dupPs.setInt(1, organizerId);
                dupPs.setString(2, cleanBank);
                dupPs.setString(3, cleanAccNum);
                try (ResultSet rs = dupPs.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Rekening / metode " + cleanBank + " (" + cleanAccNum + ") sudah terdaftar.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, organizerId);
                ps.setString(2, cleanBank);
                ps.setString(3, cleanAccNum);
                ps.setString(4, cleanAccName);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean deletePayment(int paymentId, int organizerId) throws SQLException {
        String sql = "DELETE FROM payment WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            ps.setInt(2, organizerId);
            if (ps.executeUpdate() <= 0) {
                throw new SQLException("Rekening tidak ditemukan atau bukan milik Anda.");
            }
            return true;
        }
    }
}
