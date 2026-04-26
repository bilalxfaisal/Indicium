package com.indicium.controllers;
import com.indicium.models.TimeLineEvent;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.TimeLineRepository;
import com.indicium.services.AuditLog;
import com.indicium.services.AccessManager;

import java.util.ArrayList;
import java.util.List;

public class TimeLineManager {
    private final CaseRepository caseRepo;
    private final AuditLog auditLog;
    private final AccessManager accessManager;
    private final TimeLineRepository timeLineRepo;

    public TimeLineManager() {
        caseRepo = new CaseRepository();
        auditLog = new AuditLog();
        accessManager = new AccessManager();
        timeLineRepo = new TimeLineRepository();
    }

    public List<TimeLineEvent> selectCase(int caseID){
        return timeLineRepo.getEvents(caseID);
    }

    public boolean addNewEvent(int caseID, int evidenceID, String description){
        try {
            TimeLineEvent tEvent = new TimeLineEvent(evidenceID, description, caseID);
        }
        catch(IllegalArgumentException e){
            System.out.println("[TimeLineManager] ERROR: " + e.getMessage());
             auditLog.logEvent("[TimeLineManager] Failed to add event for Case ID " + caseID + " with Evidence ID " + evidenceID + ": " + e.getMessage());
             return false;
        }
        return true;
    }

    public String saveEvent(int caseID, int evidenceID, String description){
        timeLineRepo.saveEvent(caseID, description, java.time.LocalDateTime.now(), evidenceID);
        String logMessage = "Added new timeline event to Case ID " + caseID + " with Evidence ID " + evidenceID + ": " + description;
        auditLog.logEvent(logMessage);
        return logMessage;
    }
}
