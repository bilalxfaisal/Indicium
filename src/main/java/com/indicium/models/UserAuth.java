package com.indicium.models;

import java.util.Date;

public class UserAuth {

    // ===================================================================================
    // ATTRIBUTES (From Class Diagram)
    // ===================================================================================
    private String hashedPassword;
    private Date lastLogin;

    /**
     * Constructor to initialize the authentication object for a user.
     */
    public UserAuth(String hashedPassword, Date lastLogin) {
        this.hashedPassword = hashedPassword;
        this.lastLogin = lastLogin;
    }

    // ===================================================================================
    // METHODS (From Class & Sequence Diagrams)
    // ===================================================================================

    /**
     * From Class Diagram: +authenticate() : boolean
     * Validates the provided hash against the stored hash and updates the login timestamp.
     */
    public boolean authenticate(String inputHash) {
        if (this.hashedPassword != null && this.hashedPassword.equals(inputHash)) {
            this.lastLogin = new Date(); // Update the timestamp on successful auth
            return true;
        }
        return false;
    }

    /**
     * From Sequence Diagram: VerifyUserPermissions(UserId) -> Returns Permissions
     * The CaseController calls this to ensure the user has the right to create a case.
     */
    public UserRole VerifyUserPermissions(int userId) {
        // In your SDA logic, this method checks the system state or an in-memory
        // list to determine if the specific userId has ADMIN or INVESTIGATOR rights.

        System.out.println("[UserAuth] Verifying permissions for User ID: " + userId);

        // Example integration: Returning the enum you established earlier
        // You would typically fetch the actual user object here and call getRole()
        return UserRole.INVESTIGATOR;
    }

    // ===================================================================================
    // GETTERS & SETTERS
    // ===================================================================================

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
}