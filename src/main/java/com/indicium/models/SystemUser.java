package com.indicium.models;

import com.indicium.repository.UserDirectory;
import com.indicium.models.UserRole;
import java.util.List;

public class SystemUser
{
    protected String name;
    protected int userID;
    protected String email;
    protected String credentials;
    protected UserRole role;
    protected boolean isActive;

    public SystemUser(int userID, String name, String email, String credentials, UserRole role, boolean isActive) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.credentials = credentials;
        this.role = role;
        this.isActive = isActive;
    }

    // Use Case #1 Methods (Called by IdentityController)

    public static boolean validateUniqueEmail(String email)
    {
        List<SystemUser> activeUsers = UserDirectory.getAllActiveUsers();
        for (SystemUser user : activeUsers)
        {
            if (user.getEmail().equalsIgnoreCase(email))
            {
                return false; // Match found, email is not unique
            }
        }
        return true; // No match found, email is unique
    }

    /**
     * Call #9: checkValidRole(role)
     */
    public static boolean checkValidRole(UserRole role)
    {
        return role != null && (role == UserRole.ADMIN || role == UserRole.INVESTIGATOR);
    }

    /**
     * Call #11: saveUserRecord(details)
     * Maps to partner's UserDirectory.addUser() method.
     */
    public boolean saveUserRecord()
    {
        // addUser takes String arguments. We convert the UserRole enum to its String name.
        return UserDirectory.addUser(this.name, this.email, this.credentials, this.role);
    }

    // =======================================================================
    // Getters
    // =======================================================================
    public int getUserID() { return userID; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCredentials() { return credentials; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return isActive; }
}