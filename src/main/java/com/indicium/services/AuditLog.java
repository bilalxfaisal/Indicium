package com.indicium.services;
import com.indicium.repository.LogsRepo;

public class AuditLog {
    LogsRepo logsRepo;
    public AuditLog() {
        this.logsRepo = new LogsRepo();
    }

    // timestamp not needed as DB already adds that, I hope so
    public String logEvent(int userID, String action, AuditCategory auditCategory) {
        logsRepo.saveLog(auditCategory, action,  userID);
        return "Event logged: " + auditCategory.name() + " - " + action;
    }
    public String logEvent(int userID, String action, AuditCategory auditCategory, int caseID) {
        logsRepo.saveLog(auditCategory, action,  userID, caseID);
        return "Event logged: " + auditCategory.name() + " - " + action;
    }
    public String logEvent(int userID, String action, AuditCategory auditCategory, int caseID, int evidenceID) {
        logsRepo.saveLog(auditCategory, action,  userID, caseID, evidenceID);
        return "Event logged: " + auditCategory.name() + " - " + action;
    }

}
