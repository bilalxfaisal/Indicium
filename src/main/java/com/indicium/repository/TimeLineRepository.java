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
            System.err.println("MySQL Driver not found.");
        }
    }

    private void loadDatabaseConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("database.properties")) {
            props.load(in);
            this.URL = props.getProperty("db.url");
            this.USER = props.getProperty("db.user");
            this.PASS = props.getProperty("db.password");
        } catch (IOException e) {
            System.err.println("CRITICAL: database.properties file not found! Please create it in the project root.");
        }
    }

    public boolean saveEvent(int caseID, String description, LocalDateTime timestamp, int evidenceID) {
        String sql = "INSERT INTO timeline_events (case_id, description, event_timestamp, linked_evidence_id) VALUES (?, ?, ?)";

        // Now uses the variables loaded from the properties file
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, caseID);
            stmt.setString(2, description);
            stmt.setTimestamp(3, Timestamp.valueOf(timestamp));
            stmt.setInt(4, evidenceID);

            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<TimeLineEvent> getEvents(int caseID) {
        List<TimeLineEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM timeline_events WHERE case_id = ? ORDER BY event_timestamp ASC";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, caseID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime timestamp = rs.getTimestamp("event_timestamp").toLocalDateTime();
                    String description = rs.getString("description");

                    // Get the ID. If it's NULL in the database, JDBC returns 0.
                    int linkedEvidenceID = rs.getInt("linked_evidence_id");

                    TimeLineEvent event = new TimeLineEvent(timestamp, description, linkedEvidenceID);
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch timeline: " + e.getMessage());
        }

        return events;
    }
}