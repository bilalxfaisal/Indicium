package com.indicium.models;

import com.indicium.repository.UserDirectory;
import com.indicium.services.AccessManager;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;
import com.indicium.services.SessionManager;

/**
 * Admin model extending SystemUser.
 * Enforces ADMIN role and wraps privileged operations with a session-level security check.
 * Every admin action first verifies that the currently logged-in user is actually an Admin
 * via SessionManager, preventing direct method invocation from bypassing the UI.
 */
public class Admin extends SystemUser {

    private String clearanceLevel;

    // =====================================================================
    // Constructor — role is always forced to ADMIN
    // =====================================================================

    public Admin(int userID, String name, String email, String credentials, String clearanceLevel) {
        super(userID, name, email, credentials, UserRole.ADMIN, true);
        this.clearanceLevel = clearanceLevel;
    }

    /**
     * Convenience constructor when the clearance level is not yet assigned.
     * Defaults to "FULL" for system administrators.
     */
    public Admin(int userID, String name, String email, String credentials) {
        this(userID, name, email, credentials, "FULL");
    }

    // =====================================================================
    // Admin-Specific Privileged Operations (Wrapper Methods)
    // =====================================================================

    /**
     * Activates an emergency system-wide lockdown.
     * All case access will be denied while active.
     *
     * @return true if lockdown was activated, false if the security check failed.
     */
    public boolean activateSystemLockdown() {
        if (!verifyAdminSession()) {
            return false;
        }
        AuditLog auditLog = new AuditLog();
        AccessManager.activateLockdown(this.userID, auditLog);
        return true;
    }

    /**
     * Lifts a previously activated system-wide lockdown.
     *
     * @return true if lockdown was lifted, false if the security check failed.
     */
    public boolean liftSystemLockdown() {
        if (!verifyAdminSession()) {
            return false;
        }
        AuditLog auditLog = new AuditLog();
        AccessManager.liftLockdown(this.userID, auditLog);
        return true;
    }

    /**
     * Registers a new user (Investigator or Admin) in the system.
     *
     * @param fullName     Full name of the new user.
     * @param email        Email address (must be unique).
     * @param passwordHash Pre-hashed password.
     * @param role         The role to assign (ADMIN or INVESTIGATOR).
     * @return true if the user was successfully added, false otherwise.
     */
    public boolean registerNewUser(String fullName, String email, String passwordHash, UserRole role) {
        if (!verifyAdminSession()) {
            return false;
        }
        AuditLog auditLog = new AuditLog();
        boolean success = UserDirectory.addUser(fullName, email, passwordHash, role);
        if (success) {
            auditLog.logEvent(this.userID, "Registered new user: " + email + " [" + role.name() + "]", AuditCategory.USER);
        } else {
            auditLog.logEvent(this.userID, "Failed to register user: " + email, AuditCategory.USER);
        }
        return success;
    }

    /**
     * Deactivates an existing user account, preventing further logins.
     *
     * @param targetUserID The ID of the user to deactivate.
     * @return true if the account was deactivated, false otherwise.
     */
    public boolean deactivateUser(int targetUserID) {
        if (!verifyAdminSession()) {
            return false;
        }
        AuditLog auditLog = new AuditLog();
        boolean success = UserDirectory.deactivateUser(targetUserID);
        if (success) {
            auditLog.logEvent(this.userID, "Deactivated user ID: " + targetUserID, AuditCategory.USER);
        } else {
            auditLog.logEvent(this.userID, "Failed to deactivate user ID: " + targetUserID, AuditCategory.USER);
        }
        return success;
    }

    /**
     * Updates the role of an existing user.
     *
     * @param targetUserID The ID of the user whose role is being changed.
     * @param newRole      The new role to assign.
     * @return true if the role was updated, false otherwise.
     */
    public boolean updateUserRole(int targetUserID, UserRole newRole) {
        if (!verifyAdminSession()) {
            return false;
        }
        AuditLog auditLog = new AuditLog();
        boolean success = UserDirectory.updateRole(targetUserID, newRole);
        if (success) {
            auditLog.logEvent(this.userID, "Updated user ID " + targetUserID + " role to " + newRole.name(), AuditCategory.USER);
        } else {
            auditLog.logEvent(this.userID, "Failed to update role for user ID: " + targetUserID, AuditCategory.USER);
        }
        return success;
    }

    // =====================================================================
    // Security Gate — called before every privileged operation
    // =====================================================================

    /**
     * CRITICAL SECURITY: Verifies the current session belongs to an authenticated Admin.
     * This prevents a malicious caller from instantiating this class and invoking
     * admin methods without being logged in through the legitimate UI flow.
     *
     * @return true if the current session user is an Admin, false otherwise.
     */
    private boolean verifyAdminSession() {
        boolean authorized = SessionManager.getInstance().isAdminLoggedIn();
        if (!authorized) {
            System.err.println("[SECURITY] Unauthorized Admin operation attempted by userID: " + this.userID);
            AuditLog auditLog = new AuditLog();
            auditLog.logEvent(this.userID, "UNAUTHORIZED ADMIN ACTION BLOCKED", AuditCategory.SECURITY);
        }
        return authorized;
    }

    // =====================================================================
    // Getters / Setters
    // =====================================================================

    public String getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(String clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "userID=" + userID +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", clearanceLevel='" + clearanceLevel + '\'' +
                ", active=" + isActive +
                '}';
    }

    /**
     * Updates the role of an existing user.
     * Step 13 from sequence diagram — recordTransaction(adminID, "User Modified")
     */
    public boolean modifyUserRole(int targetUserID, UserRole newRole) {
        if (!verifyAdminSession()) return false;

        AuditLog auditLog = new AuditLog();
        boolean success = UserDirectory.updateRole(targetUserID, newRole);
        if (success) {
            auditLog.logEvent(this.userID,
                    "Modified role for user ID: " + targetUserID + " → " + newRole.name(),
                    AuditCategory.USER);
        } else {
            auditLog.logEvent(this.userID,
                    "Failed to modify role for user ID: " + targetUserID,
                    AuditCategory.USER);
        }
        return success;
    }

}
