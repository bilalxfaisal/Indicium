package com.indicium.repository;

import com.indicium.models.TimeLineEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TimeLineRepository {

    private String URL;
    private String USER;
    private String PASS;

    public TimeLineRepository() {
        loadDatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[TimeLineRepo] MySQL Driver not found.");
        }
    }

    private void loadDatabaseConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("database.properties")) {
            props.load(in);
            this.URL  = props.getProperty("db.url");
            this.USER = props.getProperty("db.user");
            this.PASS = props.getProperty("db.password");
        } catch (IOException e) {
            System.err.println("[TimeLineRepo] CRITICAL: database.properties not found!");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ── Save new event ──────────────────────────────────────────

    public boolean saveEvent(TimeLineEvent event) {
        String sql = """
                INSERT INTO timeline_events
                    (case_id, title, description, event_timestamp, linked_evidence_id, added_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, event.getCaseID());
            ps.setString(2, event.getTitle());
            ps.setString(3, event.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(event.getTimestamp()));
            ps.setInt(5, event.getLinkedEvidenceID());
            ps.setString(6, event.getAddedBy());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) event.setEventID(keys.getInt(1));

            return true;
        } catch (SQLException e) {
            System.err.println("[TimeLineRepo] saveEvent failed: " + e.getMessage());
            return false;
        }
    }

    // ── Update existing event ───────────────────────────────────

    public boolean updateEvent(TimeLineEvent event) {
        String sql = """
                UPDATE timeline_events
                SET title = ?, description = ?, event_timestamp = ?, linked_evidence_id = ?
                WHERE event_id = ?
                """;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, event.getTitle());
            ps.setString(2, event.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(event.getTimestamp()));
            ps.setInt(4, event.getLinkedEvidenceID());
            ps.setInt(5, event.getEventID());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[TimeLineRepo] updateEvent failed: " + e.getMessage());
            return false;
        }
    }

    // ── Delete event ────────────────────────────────────────────

    public boolean deleteEvent(int eventID) {
        String sql = "DELETE FROM timeline_events WHERE event_id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventID);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[TimeLineRepo] deleteEvent failed: " + e.getMessage());
            return false;
        }
    }

    // ── Get all events for a case ───────────────────────────────

    public List<TimeLineEvent> findByCaseId(int caseID) {
        String sql = """
                SELECT event_id, case_id, title, description,
                       event_timestamp, linked_evidence_id, added_by
                FROM timeline_events
                WHERE case_id = ?
                ORDER BY event_timestamp ASC
                """;
        List<TimeLineEvent> events = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, caseID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                TimeLineEvent e = new TimeLineEvent();
                e.setEventID(rs.getInt("event_id"));
                e.setCaseID(rs.getInt("case_id"));
                e.setTitle(rs.getString("title"));
                e.setDescription(rs.getString("description"));
                e.setTimestamp(rs.getTimestamp("event_timestamp").toLocalDateTime());
                e.setLinkedEvidenceID(rs.getInt("linked_evidence_id"));
                e.setAddedBy(rs.getString("added_by"));
                events.add(e);
            }
        } catch (SQLException e) {
            System.err.println("[TimeLineRepo] findByCaseId failed: " + e.getMessage());
        }
        return events;
    }

    // ── Find single event ───────────────────────────────────────

    public TimeLineEvent findById(int eventID) {
        String sql = """
                SELECT event_id, case_id, title, description,
                       event_timestamp, linked_evidence_id, added_by
                FROM timeline_events
                WHERE event_id = ?
                """;
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, eventID);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                TimeLineEvent e = new TimeLineEvent();
                e.setEventID(rs.getInt("event_id"));
                e.setCaseID(rs.getInt("case_id"));
                e.setTitle(rs.getString("title"));
                e.setDescription(rs.getString("description"));
                e.setTimestamp(rs.getTimestamp("event_timestamp").toLocalDateTime());
                e.setLinkedEvidenceID(rs.getInt("linked_evidence_id"));
                e.setAddedBy(rs.getString("added_by"));
                return e;
            }
        } catch (SQLException e) {
            System.err.println("[TimeLineRepo] findById failed: " + e.getMessage());
        }
        return null;
    }
}
