package com.indicium.controllers;

import com.indicium.models.SystemUser;
import com.indicium.models.UserRole;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;

public class ID_Controller
{

    private AuditLog auditLog;

    public ID_Controller()
    {
        this.auditLog = new AuditLog();
    }

    /**
     * Call #6 in UC1 Diagram: submitChanges(details)
     */
    public String submitChanges(int adminID, int targetUserID, String name, String email, UserRole role, String credentials)
    {
        // Call #7: validateUniqueEmail(email) -> :User
        if (!SystemUser.validateUniqueEmail(email))
        {
            return "Error: Email already exists.";
        }

        // Call #9: checkValidRole(role) -> :User
        if (!SystemUser.checkValidRole(role))
        {
            return "Error: Invalid role assignment.";
        }

        // Create the object instance
        SystemUser profile = new SystemUser(targetUserID, name, email, credentials, role, true);

        // Call #11: saveUserRecord(details) -> :User
        boolean isSaved = profile.saveUserRecord();

        if (isSaved)
        {
            // Call #13: recordTransaction -> :AuditLog
            auditLog.logEvent(adminID, "User Created: " + name, AuditCategory.USER);

            // Call #15: actionComplete
            return "Success: User profile updated.";
        }

        return "Database Error: Could not save record.";
    }
}