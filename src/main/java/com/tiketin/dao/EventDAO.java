package com.tiketin.dao;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.model.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public EventDAO() {}

    public List<Event> getAllPublishedEvents() {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT e.*, u.organization AS owner_org, u.full_name AS owner_name " +
                     "FROM events e LEFT JOIN users u ON e.owner_id = u.id " +
                     "ORDER BY e.date ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapEvent(rs));
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
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapEvent(rs));
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
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapEvent(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal get event by id: " + e.getMessage());
        }
        return null;
    }

    public boolean createEvent(Event event) {
        String sql = "INSERT INTO events (owner_id, name, date, time, location, price, slots, category, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, event.getOwnerId());
            ps.setString(2, event.getName());
            ps.setString(3, event.getDate());
            ps.setString(4, event.getTime());
            ps.setString(5, event.getLocation());
            ps.setInt(6, event.getPrice());
            ps.setInt(7, event.getSlots());
            ps.setString(8, event.getCategory());
            ps.setString(9, event.getDescription());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) {
                        event.setId(gk.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Gagal create event: " + e.getMessage());
        }
        return false;
    }

    public boolean updateEvent(Event event) {
        String sql = "UPDATE events SET name = ?, date = ?, time = ?, location = ?, price = ?, slots = ?, category = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getName());
            ps.setString(2, event.getDate());
            ps.setString(3, event.getTime());
            ps.setString(4, event.getLocation());
            ps.setInt(5, event.getPrice());
            ps.setInt(6, event.getSlots());
            ps.setString(7, event.getCategory());
            ps.setString(8, event.getDescription());
            ps.setInt(9, event.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Gagal update event: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEvent(int eventId) {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Gagal delete event: " + e.getMessage());
            return false;
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
        return evt;
    }
}
