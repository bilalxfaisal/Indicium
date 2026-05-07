package com.indicium.repository;

import com.indicium.models.SystemUser;
import com.indicium.models.UserRole;

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
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[UserDirectory] MySQL driver loaded OK.");
        } catch (ClassNotFoundException e) {
            System.err.println("[UserDirectory] CRITICAL: Driver not found: " + e.getMessage());
        }
        loadDatabaseConfig();
    }

    private static void loadDatabaseConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("database.properties")) {
            props.load(in);
            URL  = props.getProperty("db.url");
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
                if (rs.next()) return extractUserFromResultSet(rs);
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
                if (rs.next()) return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR authenticating: " + e.getMessage());
        }
        return null;
    }

    // ===================================================================================
    // SETTINGS — Profile & Password (used by SettingsController)
    // ===================================================================================

    /**
     * Updates FullName and Email for a user.
     * Called by SettingsController.handleSaveProfile()
     */
    public static boolean updateProfile(int userID, String fullName, String email) {
        String sql = "UPDATE Users SET FullName = ?, Email = ? WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setInt(3, userID);
            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR updating profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether the given hashed password matches the stored PasswordHash.
     * Called by SettingsController.handleChangePassword() to verify current password.
     */
    public static boolean verifyPassword(int userID, String hashedPassword) {
        String sql = "SELECT 1 FROM Users WHERE UserID = ? AND PasswordHash = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userID);
            stmt.setString(2, hashedPassword);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Writes a new hashed password to PasswordHash for a user.
     * Called by SettingsController.handleChangePassword() after validation passes.
     */
    public static boolean updatePassword(int userID, String hashedNewPassword) {
        String sql = "UPDATE Users SET PasswordHash = ? WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, hashedNewPassword);
            stmt.setInt(2, userID);
            return stmt.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR updating password: " + e.getMessage());
            return false;
        }
    }

    // ===================================================================================
    // EXTRA FUNCTIONS (Admin / Management Features)
    // ===================================================================================

    /**
     * Registers a new investigator/admin in the system.
     */
    public static boolean addUser(String fullName, String email, String passwordHash, UserRole role) {
        String sql = "INSERT INTO Users (FullName, Email, PasswordHash, Role) VALUES (?, ?, ?, ?)";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
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
     */
    public static boolean updateRole(int userID, UserRole newRole) {
        String sql = "UPDATE Users SET Role = ? WHERE UserID = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

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

            while (rs.next()) activeUsers.add(extractUserFromResultSet(rs));

        } catch (SQLException e) {
            System.err.println("[UserDirectory] ERROR fetching active users: " + e.getMessage());
        }
        return activeUsers;
    }

    // ===================================================================================
    // HELPER METHOD
    // ===================================================================================

    private static SystemUser extractUserFromResultSet(ResultSet rs) throws SQLException {
        int     id       = rs.getInt("UserID");
        String  name     = rs.getString("FullName");
        String  email    = rs.getString("Email");
        String  hash     = rs.getString("PasswordHash");
        boolean isActive = rs.getBoolean("IsActive");

        String   roleString = rs.getString("Role");
        UserRole uRole;
        try {
            uRole = UserRole.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Warning: Unknown role found in DB. Defaulting to INVESTIGATOR.");
            uRole = UserRole.INVESTIGATOR;
        }

        return new SystemUser(id, name, email, hash, uRole, isActive);
    }
}
