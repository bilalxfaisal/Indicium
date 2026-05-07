package com.indicium.repository;

import com.indicium.models.CorrelationLink;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CorrelationRepo {

    private String URL;
    private String USER;
    private String PASS;

    public CorrelationRepo() {
        loadDatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[CorrelationRepo] MySQL Driver not found.");
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
            System.err.println("[CorrelationRepo] CRITICAL: database.properties not found!");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ═══════════════════════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════════════════════

    public boolean createLink(int sourceEvidID, int targetEvidID, int createdByUserID) {
        String sql = """
                INSERT INTO correlation_links
                    (source_ev_id, target_ev_id, created_by, created_at)
                VALUES (?, ?, ?, NOW())
                """;
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sourceEvidID);
            ps.setInt(2, targetEvidID);
            ps.setInt(3, createdByUserID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CorrelationRepo] createLink failed: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════

    public boolean deleteLink(int linkID) {
        String sql = "DELETE FROM correlation_links WHERE link_id = ?";
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, linkID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CorrelationRepo] deleteLink failed: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FETCH ALL (with optional filters)
    // ═══════════════════════════════════════════════════════════

    public List<CorrelationLink> fetchLinks(String search, String caseFilter,
                                            String typeFilter) {
        List<CorrelationLink> results = new ArrayList<>();

        // ✅ All column names now match your actual schema (PascalCase)
        StringBuilder sql = new StringBuilder("""
                SELECT
                    cl.link_id,
                    src_ev.EvidenceID   AS src_ev_id,
                    src_ev.FileName     AS src_ev_name,
                    src_ev.Status       AS src_ev_type,
                    src_c.CaseID        AS src_case_id,
                    src_c.Title         AS src_case_title,
                    tgt_ev.EvidenceID   AS tgt_ev_id,
                    tgt_ev.FileName     AS tgt_ev_name,
                    tgt_ev.Status       AS tgt_ev_type,
                    tgt_c.CaseID        AS tgt_case_id,
                    tgt_c.Title         AS tgt_case_title,
                    u.FullName          AS linked_by,
                    cl.created_at
                FROM correlation_links cl
                JOIN Evidence src_ev ON cl.source_ev_id = src_ev.EvidenceID
                JOIN Cases    src_c  ON src_ev.CaseID   = src_c.CaseID
                JOIN Evidence tgt_ev ON cl.target_ev_id = tgt_ev.EvidenceID
                JOIN Cases    tgt_c  ON tgt_ev.CaseID   = tgt_c.CaseID
                LEFT JOIN Users u    ON cl.created_by   = u.UserID
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append("""
                    AND (src_ev.FileName LIKE ?
                      OR tgt_ev.FileName LIKE ?
                      OR src_c.Title     LIKE ?
                      OR tgt_c.Title     LIKE ?)
                    """);
            String like = "%" + search + "%";
            params.add(like); params.add(like);
            params.add(like); params.add(like);
        }
        if (caseFilter != null && !caseFilter.isBlank()
                && !caseFilter.equalsIgnoreCase("All")) {
            sql.append(" AND (src_c.Title = ? OR tgt_c.Title = ?)");
            params.add(caseFilter); params.add(caseFilter);
        }
        // Evidence table has no Type column — filter by FileName extension instead
        if (typeFilter != null && !typeFilter.isBlank()
                && !typeFilter.equalsIgnoreCase("All")) {
            sql.append(" AND (src_ev.FileName LIKE ? OR tgt_ev.FileName LIKE ?)");
            String extFilter = "%" + typeFilter + "%";
            params.add(extFilter); params.add(extFilter);
        }

        sql.append(" ORDER BY cl.created_at DESC");

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++)
                ps.setObject(i + 1, params.get(i));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CorrelationLink link = new CorrelationLink();
                link.setLinkID(rs.getInt("link_id"));
                link.setSrcEvidID(rs.getInt("src_ev_id"));
                link.setSrcEvidName(rs.getString("src_ev_name"));
                link.setSrcEvidType(rs.getString("src_ev_type"));
                link.setSrcCaseID(rs.getInt("src_case_id"));
                link.setSrcCaseTitle(rs.getString("src_case_title"));
                link.setTgtEvidID(rs.getInt("tgt_ev_id"));
                link.setTgtEvidName(rs.getString("tgt_ev_name"));
                link.setTgtEvidType(rs.getString("tgt_ev_type"));
                link.setTgtCaseID(rs.getInt("tgt_case_id"));
                link.setTgtCaseTitle(rs.getString("tgt_case_title"));
                link.setLinkedBy(rs.getString("linked_by") != null
                        ? rs.getString("linked_by") : "Unknown");
                link.setCreatedAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                results.add(link);
            }

        } catch (SQLException e) {
            System.err.println("[CorrelationRepo] fetchLinks failed: " + e.getMessage());
        }
        return results;
    }

    // ═══════════════════════════════════════════════════════════
    //  DUPLICATE CHECK
    // ═══════════════════════════════════════════════════════════

    public boolean linkExists(int sourceEvidID, int targetEvidID) {
        String sql = """
                SELECT COUNT(*) FROM correlation_links
                WHERE (source_ev_id = ? AND target_ev_id = ?)
                   OR (source_ev_id = ? AND target_ev_id = ?)
                """;
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, sourceEvidID); ps.setInt(2, targetEvidID);
            ps.setInt(3, targetEvidID); ps.setInt(4, sourceEvidID);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[CorrelationRepo] linkExists failed: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  FETCH DISTINCT CASE TITLES (for filter combo)
    // ═══════════════════════════════════════════════════════════

    public List<String> fetchDistinctCaseTitles() {
        List<String> titles = new ArrayList<>();
        // ✅ Uses CaseID and EvidenceID — matches your schema
        String sql = """
                SELECT DISTINCT c.Title FROM Cases c
                WHERE c.CaseID IN (
                    SELECT src_ev.CaseID FROM correlation_links cl
                    JOIN Evidence src_ev ON cl.source_ev_id = src_ev.EvidenceID
                    UNION
                    SELECT tgt_ev.CaseID FROM correlation_links cl
                    JOIN Evidence tgt_ev ON cl.target_ev_id = tgt_ev.EvidenceID
                )
                ORDER BY c.Title
                """;
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) titles.add(rs.getString("Title"));
        } catch (SQLException e) {
            System.err.println("[CorrelationRepo] fetchDistinctCaseTitles failed: " + e.getMessage());
        }
        return titles;
    }
}
