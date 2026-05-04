package com.indicium.controllers;

import com.indicium.models.SystemUser;
import com.indicium.models.UserRole;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;
import com.indicium.repository.UserDirectory;

/**
 * ID_Controller manages the lifecycle of user identities.
 * It coordinates between the UI and the UserRepository to create and modify profiles.
 *
 */
public class ID_Controller {

    private AuditLog auditLog;
    private UserDirectory userRepo;

    public ID_Controller() {
        this.auditLog = new AuditLog();
        this.userRepo = new UserDirectory();
    }

    /**
     * UC1: Manage User Identity
     * Validates input and updates the system database.
     */
    public String manageIdentity(int adminID, int targetUserID, String name, String email, UserRole role, String credentials) {

        // 1. Validation: Check if email is unique for new/modified accounts
        //
        if (!userRepo.isEmailUnique(email)) {
            // Extension 2a: Duplicate ID conflict
            return "Error: Duplicate ID conflict. The email " + email + " is already registered.";
        }

        // 2. Validation: Ensure a valid role is assigned
        //
        if (role == null) {
            return "Security Policy Violation: Role assignment rejected.";
        }

        // 3. Update Database
        // Creating the profile object to be saved
        // Note: If SystemUser remains abstract, this line will require a concrete subclass.
        SystemUser profile = new SystemUser(targetUserID, name, email, credentials, role) {};

        boolean isSaved = userRepo.save(profile);

        if (!isSaved) {
            // Extension 4a: Database Connectivity Failure
            return "System Error: Database connectivity failure. Please try again later.";
        }

        // 4. Record the transaction in the audit log
        //
        auditLog.logEvent(
                adminID,
                "Updated identity for user: " + name + " (Role: " + role + ")",
                AuditCategory.USER
        );

        return "Success: User identity for " + name + " has been updated and logged.";
    }
}