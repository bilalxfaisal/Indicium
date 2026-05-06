package com.indicium.services;

import com.indicium.models.SystemUser;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;

import java.time.LocalDateTime;

public class SessionManager {
    private static SessionManager instance;
    private SystemUser currentUser;
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

    public void loginUser(SystemUser user) {
        this.currentUser = user;
        // call auditlog
        AuditLog ad =  new AuditLog();
        ad.logEvent(user.getUserID(), "User logged in", AuditCategory.SYSTEM);
    }

    public void logoutUser() {
        this.currentUser = null;
    }

    public SystemUser getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}