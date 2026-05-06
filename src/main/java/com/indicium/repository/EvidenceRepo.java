package com.indicium.repository;

import com.indicium.models.Evidence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EvidenceRepo {

    // Database configuration
    private static String URL;
    private static String USER;
    private static String PASS;

    // Static block to load credentials once when the program starts
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


    public List<Evidence> findByCaseId(int caseID) {
        return findByCase(caseID);
    }

    public Evidence findById(int evidenceID) {
        return getEvidence(evidenceID);
    }



    // ===================================================================================
    // UC5: Save Evidence to Database (Replaces the static Map/List)
    // ===================================================================================
    public static void add(Evidence evidence, int caseID) {
        // We use UploaderID as 0 or null for now since your manager doesn't pass it yet
        String sql = "INSERT INTO Evidence (CaseID, FileName, FileHash, StoragePath, Status, UploaderID) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, caseID);
            // Constructing the full name to store in DB
            stmt.setString(2, evidence.getFile().getName());
            stmt.setString(3, evidence.getDigitalFingerprint());
            stmt.setString(4, evidence.getFilePath());
            stmt.setString(5, evidence.isLocked() ? "LOCKED" : "ACTIVE");
            stmt.setNull(6, java.sql.Types.INTEGER); // UploaderID (Update later if needed)

            int affectedRows = stmt.executeUpdate();

            // Retrieve the database-generated ID and update the Java object
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        evidence.setEvidenceID(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[EvidenceRepo] ERROR: Failed to save evidence - " + e.getMessage());
        }
    }

    // ===================================================================================
    // RETRIEVAL: Get a single piece of Evidence (Used in requestMedia)
    // ===================================================================================
    public static Evidence getEvidence(int evidenceID) {
        String sql = "SELECT * FROM Evidence WHERE EvidenceID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, evidenceID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Get the physical file path from the database
                    String path = rs.getString("StoragePath");
                    File retrievedFile = new File(path);

                    String hash = rs.getString("FileHash");

                    // 2. Reconstruct the Evidence object using your existing constructor
                    Evidence ev = new Evidence(retrievedFile, hash);

                    // 3. Override the auto-generated ID with the true Database ID
                    ev.setEvidenceID(rs.getInt("EvidenceID"));

                    // 4. Restore the lock status
                    if ("LOCKED".equals(rs.getString("Status"))) {
                        ev.lock();
                    }

                    // 5. Restore the Case Link
                    ev.linkWithCase(rs.getInt("CaseID"));

                    return ev;
                }
            }
        } catch (SQLException e) {
            System.err.println("[EvidenceRepo] ERROR: Failed to retrieve evidence - " + e.getMessage());
        }
        return null;
    }

    // ===================================================================================
    // RETRIEVAL: Find all evidence for a specific case
    // ===================================================================================
    public static List<Evidence> findByCase(int caseID) {
        List<Evidence> list = new ArrayList<>();
        String sql = "SELECT * FROM Evidence WHERE CaseID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, caseID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    File retrievedFile = new File(rs.getString("StoragePath"));
                    String hash = rs.getString("FileHash");

                    Evidence ev = new Evidence(retrievedFile, hash);
                    ev.setEvidenceID(rs.getInt("EvidenceID"));

                    if ("LOCKED".equals(rs.getString("Status"))) {
                        ev.lock();
                    }
                    ev.linkWithCase(rs.getInt("CaseID"));

                    list.add(ev);
                }
            }
        } catch (SQLException e) {
            System.err.println("[EvidenceRepo] ERROR: Failed to fetch evidence list - " + e.getMessage());
        }
        return list;
    }

    public static boolean checkDuplicate(String hash)
    {
        String sql = "SELECT COUNT(*) FROM Evidence WHERE FileHash = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql))
        {
            stmt.setString(1, hash);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    int count = rs.getInt(1);

                    // If count > 0, duplicate exists
                    return count == 0;
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("[EvidenceRepo] ERROR: Failed to check duplicate - " + e.getMessage());
        }

        return false;
    }
}
