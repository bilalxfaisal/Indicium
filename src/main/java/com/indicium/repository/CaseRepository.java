package com.indicium.repository;

import com.indicium.controllers.CaseFilter;
import com.indicium.controllers.FilterCriteria;
import com.indicium.models.Case;
import com.indicium.models.CaseStatus;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CaseRepository {

    // Database configuration
    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        loadDatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found.");
        }
    }

    private static void loadDatabaseConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("database.properties")) {
            props.load(in);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASS = props.getProperty("db.password");
        } catch (IOException e) {
            System.err.println("CRITICAL: database.properties file not found!");
        }
    }

    // ===================================================================================
    // INITIALIZATION & SAVING
    // ===================================================================================
    public void save(Case newCase) {
        String sql = "INSERT INTO Cases (CaseID, Title, IncidentDate, Status) VALUES (?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, newCase.getCaseID());
            stmt.setString(2, newCase.getTitle());
            stmt.setTimestamp(3, Timestamp.valueOf(newCase.getIncidentDate()));
            stmt.setString(4, newCase.getStatus().name());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CaseRepo] ERROR saving case: " + e.getMessage());
        }
    }

    // ===================================================================================
    // RETRIEVAL & FILTERING
    // ===================================================================================
    public Case findById(int caseID) {
        String sql = "SELECT * FROM Cases WHERE CaseID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, caseID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractCaseFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CaseRepo] ERROR finding case: " + e.getMessage());
        }
        return null;
    }

    public List<Case> findByFilter(FilterCriteria criteria)
    {
        // Build the query condition to apply
        CaseFilter filter = new CaseFilter();
        String condition = filter.buildQuery(criteria);

        List<Case> cases = new ArrayList<>();

        // Append the dynamically built condition to the base SELECT statement
        String sql = "SELECT * FROM Cases WHERE " + condition;

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Assuming you already have this helper method in your class
                cases.add(extractCaseFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CaseRepo] ERROR executing dynamic filter query: " + e.getMessage());
        }

        return cases;
    }

    // ===================================================================================
    // UPDATING
    // ===================================================================================
    // Supports the call: caseRepo.update(c, "status = 'ARCHIVED'") from CaseManager
    public void update(Case c, String customSetClause) {
        // We sync the entire object state back to the database safely
        String sql = "UPDATE Cases SET Title = ?, IncidentDate = ?, Status = ? WHERE CaseID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, c.getTitle());
            stmt.setTimestamp(2, Timestamp.valueOf(c.getIncidentDate()));
            stmt.setString(3, c.getStatus().name());
            stmt.setInt(4, c.getCaseID());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[CaseRepo] ERROR updating case: " + e.getMessage());
        }
    }

    // ===================================================================================
    // SECURITY & ACCESS CONTROL (Must be static for Case.java)
    // ===================================================================================
    public static boolean isUserAssignedToCase(int investigatorID, int caseID) {
        String sql = "SELECT 1 FROM CaseAssignments WHERE InvestigatorID = ? AND CaseID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, investigatorID);
            stmt.setInt(2, caseID);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Returns true if a match is found
            }
        } catch (SQLException e) {
            System.err.println("[CaseRepo] ERROR checking assignment: " + e.getMessage());
            return false;
        }
    }

    // ===================================================================================
    // HELPER METHOD
    // ===================================================================================
    private Case extractCaseFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("CaseID");
        String title = rs.getString("Title");
        LocalDateTime date = rs.getTimestamp("IncidentDate").toLocalDateTime();
        CaseStatus status = CaseStatus.valueOf(rs.getString("Status"));

        return new Case(id, title, date, status);
    }
    public List<Case> findAll() {
        String sql = "SELECT * FROM Cases ORDER BY IncidentDate DESC";
        List<Case> cases = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                cases.add(extractCaseFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CaseRepo] ERROR fetching all cases: " + e.getMessage());
        }
        return cases;
    }
    public List<Case> searchCases(String query, String status) {
        List<Case> results = new ArrayList<>();
        String sql = """
            SELECT CaseID, Title, IncidentDate, Status
            FROM Cases
            WHERE (LOWER(Title) LIKE ? OR CAST(CaseID AS CHAR) LIKE ?)
              AND (? = 'All' OR Status = ?)
            ORDER BY CaseID DESC LIMIT 8
            """;
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {
            String like = "%" + query + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, status);
            ps.setString(4, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(new Case(
                        rs.getInt("CaseID"),
                        rs.getString("Title"),
                        rs.getTimestamp("IncidentDate").toLocalDateTime(),
                        CaseStatus.valueOf(rs.getString("Status"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("[CaseRepository] searchCases failed: " + e.getMessage());
        }
        return results;
    }


}