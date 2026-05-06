package com.indicium.repository;

import com.indicium.models.AuditLogEntry;
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
            this.URL  = props.getProperty("db.url");
            this.USER = props.getProperty("db.user");
            this.PASS = props.getProperty("db.password");
        } catch (IOException e) {
            System.err.println("CRITICAL: database.properties file not found!");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ═══════════════════════════════════════════════════════════
    //  SAVE OVERLOADS (your existing ones — unchanged)
    // ═══════════════════════════════════════════════════════════

    public boolean saveLog(AuditCategory category, String description,
                           int investigatorID, int caseID, int evidenceID) {
        String sql = "INSERT INTO ForensicAuditLog (Category, Description, InvestigatorID, LinkedCaseID, LinkedEvidenceID) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = connect();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            stmt.setString(2, description);
            stmt.setInt(3, investigatorID);
            stmt.setInt(4, caseID);
            stmt.setInt(5, evidenceID);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace(); return false;
        }
    }

    public boolean saveLog(AuditCategory category, String description,
                           int investigatorID, int caseID) {
        String sql = "INSERT INTO ForensicAuditLog (Category, Description, InvestigatorID, LinkedCaseID) VALUES (?, ?, ?, ?)";
        try (Connection con = connect();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            stmt.setString(2, description);
            stmt.setInt(3, investigatorID);
            stmt.setInt(4, caseID);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace(); return false;
        }
    }

    public boolean saveLog(AuditCategory category, String description, int investigatorID) {
        String sql = "INSERT INTO ForensicAuditLog (Category, Description, InvestigatorID) VALUES (?, ?, ?)";
        try (Connection con = connect();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            stmt.setString(2, description);
            stmt.setInt(3, investigatorID);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace(); return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FETCH — paginated + filtered (used by AuditLogController)
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns one page of audit log entries with optional filters.
     *
     * @param search       text to match against Description or username (null = no filter)
     * @param category     AuditCategory filter (null = all)
     * @param role         role string filter (null = all)
     * @param pageSize     rows per page
     * @param offset       (currentPage - 1) * pageSize
     */
    public List<AuditLogEntry> fetchLogs(String search, AuditCategory category,
                                         String role, int pageSize, int offset) {
        List<AuditLogEntry> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT
                    f.LogID,
                    f.Category,
                    f.Description,
                    f.InvestigatorID,
                    f.LinkedCaseID,
                    f.LinkedEvidenceID,
                    f.LogTimestamp,
                    u.FullName,
                    u.Role
                FROM ForensicAuditLog f
                LEFT JOIN users u ON f.InvestigatorID = u.UserID
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (f.Description LIKE ? OR u.FullName LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        if (category != null) {
            sql.append(" AND f.Category = ?");
            params.add(category.name());
        }
        if (role != null && !role.equalsIgnoreCase("All")) {
            sql.append(" AND u.Role = ?");
            params.add(role);
        }

        sql.append(" ORDER BY f.LogTimestamp DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        try (Connection con = connect();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++)
                stmt.setObject(i + 1, params.get(i));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                AuditLogEntry entry = new AuditLogEntry();
                entry.setLogID(rs.getInt("LogID"));
                entry.setCategory(rs.getString("Category"));
                entry.setDescription(rs.getString("Description"));
                entry.setInvestigatorID(rs.getInt("InvestigatorID"));
                entry.setLinkedCaseID(rs.getInt("LinkedCaseID"));
                entry.setLinkedEvidenceID(rs.getInt("LinkedEvidenceID"));
                entry.setTimestamp(rs.getTimestamp("LogTimestamp").toLocalDateTime());
                entry.setFullName(rs.getString("FullName") != null
                        ? rs.getString("FullName") : "Unknown");
                entry.setRole(rs.getString("Role") != null
                        ? rs.getString("Role") : "Unknown");
                results.add(entry);
            }

        } catch (SQLException e) {
            System.err.println("[LogsRepo] fetchLogs failed: " + e.getMessage());
        }
        return results;
    }

    // ── Count query (for pagination total) ──────────────────────

    public int countLogs(String search, AuditCategory category, String role) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM ForensicAuditLog f
                LEFT JOIN users u ON f.InvestigatorID = u.UserID
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (f.Description LIKE ? OR u.FullName LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        if (category != null) {
            sql.append(" AND f.Category = ?");
            params.add(category.name());
        }
        if (role != null && !role.equalsIgnoreCase("All")) {
            sql.append(" AND u.Role = ?");
            params.add(role);
        }

        try (Connection con = connect();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++)
                stmt.setObject(i + 1, params.get(i));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            System.err.println("[LogsRepo] countLogs failed: " + e.getMessage());
        }
        return 0;
    }

    // ── Fetch ALL for CSV export (no pagination) ─────────────────

    public List<AuditLogEntry> fetchAllForExport(String search,
                                                 AuditCategory category,
                                                 String role) {
        // Reuse fetchLogs with a very large limit
        return fetchLogs(search, category, role, Integer.MAX_VALUE, 0);
    }
}
