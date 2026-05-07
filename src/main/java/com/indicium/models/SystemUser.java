package com.indicium.models;

import com.indicium.repository.UserDirectory;
import com.indicium.models.UserRole;
import java.util.List;

public class SystemUser {

    protected String   name;        // maps to FullName in DB
    protected int      userID;
    protected String   email;
    protected String   credentials; // maps to PasswordHash in DB
    protected UserRole role;
    protected boolean  isActive;

    public SystemUser(int userID, String name, String email,
                      String credentials, UserRole role, boolean isActive) {
        this.userID      = userID;
        this.name        = name;
        this.email       = email;
        this.credentials = credentials;
        this.role        = role;
        this.isActive    = isActive;
    }

    // ── Use Case #1 Methods ──────────────────────────────────

    public static boolean validateUniqueEmail(String email) {
        List<SystemUser> activeUsers = UserDirectory.getAllActiveUsers();
        for (SystemUser user : activeUsers) {
            if (user.getEmail().equalsIgnoreCase(email)) return false;
        }
        return true;
    }

    public static boolean checkValidRole(UserRole role) {
        return role != null
                && (role == UserRole.ADMIN || role == UserRole.INVESTIGATOR);
    }

    public boolean saveUserRecord() {
        return UserDirectory.addUser(this.name, this.email, this.credentials, this.role);
    }

    // ── Getters ──────────────────────────────────────────────

    public int      getUserID()     { return userID;      }
    public String   getName()       { return name;        }
    public String   getFullName()   { return name;        } // alias — used by SettingsController
    public String   getEmail()      { return email;       }
    public String   getCredentials(){ return credentials; }
    public UserRole getRole()       { return role;        }
    public boolean  isActive()      { return isActive;    }

    // ── Setters (consistent lowercase convention) ─────────────

    public void setName(String fullName)   { this.name  = fullName; }
    public void setEmail(String email)     { this.email = email;    }

    // Legacy uppercase setters — kept so nothing else breaks
    public void SetName(String fullName)   { this.name  = fullName; }
    public void SetEmail(String email)     { this.email = email;    }
}
