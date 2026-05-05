package com.indicium.repository;

import com.indicium.models.SystemUser;
import com.indicium.models.UserRole; // Make sure to import the enum!

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
    // CORE FUNCTIONS
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
        return null;
    }

    // ===================================================================================
    // EXTRA FUNCTIONS (Admin / Management Features)
    // ===================================================================================

    /**
     * Registers a new investigator/admin in the system.
     * UPDATED: Now accepts UserRole enum.
     */
    public static boolean addUser(String fullName, String email, String passwordHash, UserRole role) {
        String sql = "INSERT INTO Users (FullName, Email, PasswordHash, Role) VALUES (?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            // Convert Enum to String for the database
            stmt.setString(4, role.name());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR adding user: " + e.getMessage());
            return false;
        }
    }

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
     * Updates an investigator's role.
     * UPDATED: Now accepts UserRole enum.
     */
    public static boolean updateRole(int userID, UserRole newRole) {
        String sql = "UPDATE Users SET Role = ? WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            // Convert Enum to String for the database
            stmt.setString(1, newRole.name());
            stmt.setInt(2, userID);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR updating role: " + e.getMessage());
            return false;
        }
    }

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
        int id = rs.getInt("UserID");
        String name = rs.getString("FullName");
        String email = rs.getString("Email");
        String hash = rs.getString("PasswordHash");
        boolean isActive = rs.getBoolean("IsActive");

        // Fetch the role string from the database
        String roleString = rs.getString("Role");
        UserRole uRole;

        // Convert the string to the Enum safely
        try {
            // .toUpperCase() ensures it matches the enum exactly, even if DB has "admin"
            uRole = UserRole.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Warning: Unknown role found in DB. Defaulting to INVESTIGATOR.");
            uRole = UserRole.INVESTIGATOR; // Fallback to prevent application crashes
        }

        return new SystemUser(id, name, email, hash, uRole, isActive);
    }
}