package com.indicium.repository;

import com.indicium.services.AuditCategory;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LogsRepo {
    private String URL;
    private String USER;
    private String PASS;

    public LogsRepo() {
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
            System.err.println("CRITICAL: database.properties file not found!");
        }
    }

    // ===================================================================================
    // OVERLOAD 1: EVIDENCE LEVEL LOG (Most detailed)
    // Use Case: "Evidence Uploaded", "Evidence Locked", "Duplicate Found"
    // ===================================================================================
    public boolean saveLog(AuditCategory category, String description, int investigatorID, int caseID, int evidenceID) {
        String sql = "INSERT INTO ForensicAuditLog (Category, Description, InvestigatorID, LinkedCaseID, LinkedEvidenceID) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, category.name());
            stmt.setString(2, description);
            stmt.setInt(3, investigatorID);
            stmt.setInt(4, caseID);
            stmt.setInt(5, evidenceID);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===================================================================================
    // OVERLOAD 2: CASE LEVEL LOG
    // Use Case: "Case Created", "Report Generated", "Timeline Event Added"
    // ===================================================================================
    public boolean saveLog(AuditCategory category, String description, int investigatorID, int caseID) {
        String sql = "INSERT INTO ForensicAuditLog (Category, Description, InvestigatorID, LinkedCaseID) VALUES (?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, category.name());
            stmt.setString(2, description);
            stmt.setInt(3, investigatorID);
            stmt.setInt(4, caseID);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===================================================================================
    // OVERLOAD 3: GLOBAL / SYSTEM LEVEL LOG (No Case, No Evidence)
    // Use Case: "Investigator Logged In", "Database Backup Started", "Unauthorized Access Attempt"
    // ===================================================================================
    public boolean saveLog(AuditCategory category, String description, int investigatorID) {
        String sql = "INSERT INTO ForensicAuditLog (Category, Description, InvestigatorID) VALUES (?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, category.name());
            stmt.setString(2, description);
            stmt.setInt(3, investigatorID);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ===================================================================================
    // RETRIEVAL: Fetching logs for the Audit Dashboard
    // ===================================================================================
    public List<String> getLogsByCategory(AuditCategory category) {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT * FROM ForensicAuditLog WHERE Category = ? ORDER BY LogTimestamp DESC";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, category.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Create a clean string for your UI list
                    String entry = String.format("[%s] %s: %s (User: %d) | Case: %d | EvID: %d",
                            rs.getTimestamp("LogTimestamp"),
                            rs.getString("Category"),
                            rs.getString("Description"),
                            rs.getInt("InvestigatorID"),
                            rs.getInt("LinkedCaseID"),
                            rs.getInt("LinkedEvidenceID"));
                    logs.add(entry);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch logs: " + e.getMessage());
        }
        return logs;
    }
}