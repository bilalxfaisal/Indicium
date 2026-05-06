package com.indicium.services;

import com.indicium.models.SystemUser;
import com.indicium.models.UserAuth;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public class SessionManager {

    private static SessionManager instance;
    private SystemUser    currentUser;
    private UserAuth      currentUserAuth;
    private LocalDateTime startTime;

    private SessionManager() {
        startTime = LocalDateTime.now();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ══════════════════════════════════════════
    //  Login / Logout
    // ══════════════════════════════════════════

    public void loginUser(SystemUser user) {
        this.currentUser     = user;
        // credentials = hashed password stored in DB
        this.currentUserAuth = new UserAuth(user.getCredentials(), null);

        AuditLog ad = new AuditLog();
        ad.logEvent(user.getUserID(), "User logged in", AuditCategory.SYSTEM);
    }

    public void logoutUser() {
        if (currentUser != null) {
            AuditLog ad = new AuditLog();
            ad.logEvent(currentUser.getUserID(), "User logged out", AuditCategory.SYSTEM);
        }
        this.currentUser     = null;
        this.currentUserAuth = null;
    }

    // ══════════════════════════════════════════
    //  Getters
    // ══════════════════════════════════════════

    public SystemUser getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns the UserAuth object for the logged-in user.
     * Use this in controllers instead of touching credentials directly.
     */
    public UserAuth getCurrentUserAuth() {
        if (currentUserAuth == null)
            throw new IllegalStateException("No user is currently logged in.");
        return currentUserAuth;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }


    public boolean verifyPassword(String rawInput) {
        if (currentUserAuth == null || rawInput == null) return false;
        String hashed = hashSHA256(rawInput);
        return currentUserAuth.authenticate(hashed);
    }

    /**
     * Shared SHA-256 utility — same algorithm as LoginController.
     * Call this anywhere instead of duplicating the hashing logic.
     *
     * Usage: SessionManager.hashSHA256("plainText")
     */
    public static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(2 * encoded.length);
            for (byte b : encoded) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available on this JVM.", e);
        }
    }
}
