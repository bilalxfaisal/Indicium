package com.indicium.controllers;

import com.indicium.models.CorrelationLink;
import com.indicium.models.Evidence;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.CorrelationRepo;
import com.indicium.repository.EvidenceRepo;
import com.indicium.services.AuditCategory;
import com.indicium.services.AuditLog;

import java.util.List;

public class CorrelationManager {

    private final AuditLog       auditLog;
    private final CorrelationRepo corrRepo;
    private final EvidenceRepo    evidenceRepo;
    private final CaseRepository  caseRepo;

    public CorrelationManager() {
        this.auditLog     = new AuditLog();
        this.corrRepo     = new CorrelationRepo();
        this.evidenceRepo = new EvidenceRepo();
        this.caseRepo     = new CaseRepository();
    }

    // ═══════════════════════════════════════════════════════════
    //  STEP 1 — Authorize access to target case
    // ═══════════════════════════════════════════════════════════

    public boolean initiateLink(int investigatorID, int targetCaseID) {
        boolean authorized = CaseRepository.isUserAssignedToCase(
                investigatorID, targetCaseID);

        if (!authorized) {
            auditLog.logEvent(investigatorID,
                    "Unauthorized link attempt to Case #" + targetCaseID,
                    AuditCategory.SECURITY, targetCaseID);
            return false;
        }

        auditLog.logEvent(investigatorID,
                "Authorized access to Case #" + targetCaseID + " for correlation",
                AuditCategory.EVIDENCE, targetCaseID);
        return true;
    }

    // ═══════════════════════════════════════════════════════════
    //  STEP 2 — Get evidence list for a case (target panel)
    // ═══════════════════════════════════════════════════════════

    public List<Evidence> getEvidenceForCase(int caseID) {
        return evidenceRepo.findByCaseId(caseID);
    }

    // ═══════════════════════════════════════════════════════════
    //  STEP 3 — Duplicate check
    // ═══════════════════════════════════════════════════════════

    public boolean linkAlreadyExists(int sourceEvidID, int targetEvidID) {
        return corrRepo.linkExists(sourceEvidID, targetEvidID);
    }

    // ═══════════════════════════════════════════════════════════
    //  STEP 4 — Create the link
    // ═══════════════════════════════════════════════════════════

    public boolean createCrossCaseLink(int investigatorID,
                                       int sourceEvidID, int sourceCaseID,
                                       int targetEvidID, int targetCaseID) {

        // Re-check authorization before final commit
        if (!CaseRepository.isUserAssignedToCase(investigatorID, targetCaseID)) {
            auditLog.logEvent(investigatorID,
                    "Final auth check failed for Case #" + targetCaseID,
                    AuditCategory.SECURITY, targetCaseID);
            return false;
        }

        // Duplicate guard
        if (corrRepo.linkExists(sourceEvidID, targetEvidID)) {
            System.err.println("[CorrelationManager] Link already exists.");
            return false;
        }

        boolean saved = corrRepo.createLink(sourceEvidID, targetEvidID, investigatorID);
        if (!saved) return false;

        // Audit log — case level
        auditLog.logEvent(investigatorID,
                "Cross-case link created: EV-" + sourceEvidID
                        + " (Case #" + sourceCaseID + ")"
                        + " ⇄ EV-" + targetEvidID
                        + " (Case #" + targetCaseID + ")",
                AuditCategory.EVIDENCE, sourceCaseID);

        return true;
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════

    public boolean removeLink(int investigatorID, int linkID, int caseID) {
        boolean deleted = corrRepo.deleteLink(linkID);
        if (deleted) {
            auditLog.logEvent(investigatorID,
                    "Cross-case link #" + linkID + " removed",
                    AuditCategory.EVIDENCE, caseID);
        }
        return deleted;
    }

    // ═══════════════════════════════════════════════════════════
    //  FETCH
    // ═══════════════════════════════════════════════════════════

    public List<CorrelationLink> getLinks(String search,
                                          String caseFilter,
                                          String typeFilter) {
        return corrRepo.fetchLinks(search, caseFilter, typeFilter);
    }

    public List<String> getDistinctCaseTitles() {
        return corrRepo.fetchDistinctCaseTitles();
    }
}
