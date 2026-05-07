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
    private final CaseRepository  caseRepo;

    public CorrelationManager() {
        this.auditLog  = new AuditLog();
        this.corrRepo  = new CorrelationRepo();
        this.caseRepo  = new CaseRepository();
    }

    // ── Auth check (always true for now — no session) ───────────
    public boolean initiateLink(int investigatorID, int targetCaseID) {
        auditLog.logEvent(investigatorID,
                "Initiated correlation search for Case #" + targetCaseID,
                AuditCategory.EVIDENCE, targetCaseID);
        return true;
    }

    // ── Get evidence for a case ──────────────────────────────────
    public List<Evidence> getEvidenceForCase(int caseID) {
        return EvidenceRepo.findByCase(caseID);
    }

    // ── Duplicate check ──────────────────────────────────────────
    public boolean linkAlreadyExists(int sourceEvidID, int targetEvidID) {
        return corrRepo.linkExists(sourceEvidID, targetEvidID);
    }

    // ── Create link ──────────────────────────────────────────────
    public boolean createCrossCaseLink(int investigatorID,
                                       int sourceEvidID, int sourceCaseID,
                                       int targetEvidID, int targetCaseID) {
        if (corrRepo.linkExists(sourceEvidID, targetEvidID)) {
            System.err.println("[CorrelationManager] Link already exists.");
            return false;
        }

        boolean saved = corrRepo.createLink(sourceEvidID, targetEvidID, investigatorID);
        if (!saved) return false;

        auditLog.logEvent(investigatorID,
                "Cross-case link created: EV-" + sourceEvidID
                        + " (Case #" + sourceCaseID + ")"
                        + " ⇄ EV-" + targetEvidID
                        + " (Case #" + targetCaseID + ")",
                AuditCategory.EVIDENCE, sourceCaseID);
        return true;
    }

    // ── Delete link ──────────────────────────────────────────────
    public boolean removeLink(int investigatorID, int linkID, int caseID) {
        boolean deleted = corrRepo.deleteLink(linkID);
        if (deleted) {
            auditLog.logEvent(investigatorID,
                    "Cross-case link #" + linkID + " removed",
                    AuditCategory.EVIDENCE, caseID);
        }
        return deleted;
    }

    // ── Fetch all links ──────────────────────────────────────────
    public List<CorrelationLink> getLinks(String search,
                                          String caseFilter,
                                          String typeFilter) {
        return corrRepo.fetchLinks(search, caseFilter, typeFilter);
    }

    public List<String> getDistinctCaseTitles() {
        return corrRepo.fetchDistinctCaseTitles();
    }
}
