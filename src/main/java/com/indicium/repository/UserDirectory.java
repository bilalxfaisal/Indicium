package com.indicium.repository;

import com.indicium.models.SystemUser;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class UserDirectory {

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
    // CORE FUNCTIONS (From Sequence/Class Diagrams)
    // ===================================================================================

    public static SystemUser findUser(int userID) {
        String sql = "SELECT * FROM Users WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR finding user: " + e.getMessage());
        }
        return null;
    }

    public static SystemUser authenticate(String email, String hashedInputPassword) {
        // Only allows active users to log in
        String sql = "SELECT * FROM Users WHERE Email = ? AND PasswordHash = ? AND IsActive = TRUE";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email);
            stmt.setString(2, hashedInputPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR authenticating: " + e.getMessage());
        }
        return null; // Login failed (wrong email, password, or account deactivated)
    }

    // ===================================================================================
    // EXTRA FUNCTIONS (Admin / Management Features)
    // ===================================================================================

    /**
     * Registers a new investigator/admin in the system.
     */
    public static boolean addUser(String fullName, String email, String passwordHash, String role) {
        String sql = "INSERT INTO Users (FullName, Email, PasswordHash, Role) VALUES (?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            stmt.setString(4, role);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR adding user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Soft-deletes a user (prevents login but keeps their audit logs intact).
     * In forensics, you NEVER physically delete a user from the database.
     */
    public static boolean deactivateUser(int userID) {
        String sql = "UPDATE Users SET IsActive = FALSE WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userID);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR deactivating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an investigator's role (e.g., promoting them to ADMIN).
     */
    public static boolean updateRole(int userID, String newRole) {
        String sql = "UPDATE Users SET Role = ? WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setInt(2, userID);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR updating role: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches all active users (useful for populating assignment dropdowns in the UI).
     */
    public static List<SystemUser> getAllActiveUsers() {
        List<SystemUser> activeUsers = new ArrayList<>();
        String sql = "SELECT * FROM Users WHERE IsActive = TRUE";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                activeUsers.add(extractUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR fetching active users: " + e.getMessage());
        }
        return activeUsers;
    }

    // ===================================================================================
    // HELPER METHOD
    // ===================================================================================
    private static SystemUser extractUserFromResultSet(ResultSet rs) throws SQLException {
        // Adjust these variables based on your actual SystemUser.java constructor
        int id = rs.getInt("UserID");
        String name = rs.getString("FullName");
        String email = rs.getString("Email");
        String hash = rs.getString("PasswordHash");
        String role = rs.getString("Role");
        boolean isActive = rs.getBoolean("IsActive");

        return new SystemUser(id, name, email, hash, role, isActive);
    }
}