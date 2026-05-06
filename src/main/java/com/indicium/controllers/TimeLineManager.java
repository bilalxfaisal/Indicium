package com.indicium.controllers;

import com.indicium.models.Case;
import com.indicium.models.CaseStatus;
import com.indicium.models.TimeLineEvent;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.EvidenceRepo;
import com.indicium.repository.TimeLineRepository;
import com.indicium.services.AuditCategory;
import com.indicium.services.AuditLog;
import com.indicium.services.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class TimeLineManager {

    private final CaseRepository    caseRepo;
    private final AuditLog          auditLog;
    private final TimeLineRepository timeLineRepo;
    private final EvidenceRepo       evidenceRepo;

    public TimeLineManager() {
        caseRepo      = new CaseRepository();
        auditLog      = new AuditLog();
        timeLineRepo  = new TimeLineRepository();
        evidenceRepo  = new EvidenceRepo();
    }

    // ── Load case and its events ────────────────────────────────

    public Case selectCase(int caseID) {
        return caseRepo.findById(caseID);
    }

    public List<TimeLineEvent> getEventsForCase(int caseID) {
        return timeLineRepo.findByCaseId(caseID);
    }

    // ── Add event ───────────────────────────────────────────────

    public TimeLineEvent addEvent(int caseID, String title, String description,
                                  LocalDateTime timestamp, int linkedEvidenceID) {

        // Validate evidence belongs to this case
        if (linkedEvidenceID > 0) {
            boolean valid = evidenceRepo.findByCaseId(caseID)
                    .stream()
                    .anyMatch(e -> e.getEvidenceID() == linkedEvidenceID);
            if (!valid) {
                System.err.println("[TimeLineManager] Evidence EV-" + linkedEvidenceID
                        + " does not belong to case #" + caseID);
                return null;
            }
        }

        String addedBy = SessionManager.getInstance().getCurrentUser().getName();

        TimeLineEvent event = new TimeLineEvent(
                caseID, title, description, timestamp, linkedEvidenceID, addedBy
        );

        boolean saved = timeLineRepo.saveEvent(event);
        if (!saved) return null;

        // Audit log
        int userID = SessionManager.getInstance().getCurrentUser().getUserID();
        auditLog.logEvent(userID,
                "Added timeline event '" + title + "' to case #" + caseID,
                AuditCategory.TIMELINE);

        return event;
    }

    // ── Edit event ──────────────────────────────────────────────

    public boolean editEvent(int eventID, int caseID, String title, String description,
                             LocalDateTime timestamp, int linkedEvidenceID) {

        TimeLineEvent existing = timeLineRepo.findById(eventID);
        if (existing == null) return false;

        // Validate evidence still belongs to this case
        if (linkedEvidenceID > 0) {
            boolean valid = evidenceRepo.findByCaseId(caseID)
                    .stream()
                    .anyMatch(e -> e.getEvidenceID() == linkedEvidenceID);
            if (!valid) return false;
        }

        existing.setTitle(title);
        existing.setDescription(description);
        existing.setTimestamp(timestamp);
        existing.setLinkedEvidenceID(linkedEvidenceID);

        boolean updated = timeLineRepo.updateEvent(existing);

        if (updated) {
            int userID = SessionManager.getInstance().getCurrentUser().getUserID();
            auditLog.logEvent(userID,
                    "Edited timeline event #" + eventID + " on case #" + caseID,
                    AuditCategory.TIMELINE);
        }
        return updated;
    }

    // ── Delete event ────────────────────────────────────────────

    public boolean deleteEvent(int eventID, int caseID) {
        boolean deleted = timeLineRepo.deleteEvent(eventID);
        if (deleted) {
            int userID = SessionManager.getInstance().getCurrentUser().getUserID();
            auditLog.logEvent(userID,
                    "Deleted timeline event #" + eventID + " from case #" + caseID,
                    AuditCategory.TIMELINE);
        }
        return deleted;
    }

    // ── Read-only guard ─────────────────────────────────────────

    public boolean isCaseReadOnly(int caseID) {
        Case c = caseRepo.findById(caseID);
        if (c == null) return true;
        return c.getStatus() == CaseStatus.CLOSED;

    }
}
