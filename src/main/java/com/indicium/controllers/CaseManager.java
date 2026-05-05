package com.indicium.controllers;

import com.indicium.models.Case;
import com.indicium.models.CaseStatus;
import com.indicium.models.Evidence;
import com.indicium.models.UserAuth;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.EvidenceRepo;
import com.indicium.services.IStorageService;
import com.indicium.services.LongTermStorage;
import com.indicium.services.*;

import java.time.LocalDateTime;
import java.util.List;

public class CaseManager {
    private final CaseRepository caseRepo;
    private final EvidenceRepo evidenceRepo;
    private final AccessManager accessManager;
    private final AuditLog auditLog;
    private final UserAuth userAuth;
    private final IStorageService storageService;
    private final HashGenerator hashGenerator;

    public CaseManager(UserAuth userAuth, EvidenceRepo evidenceRepo) {
        this.userAuth = userAuth;
        this.evidenceRepo = evidenceRepo;
        this.caseRepo = new CaseRepository();
        this.accessManager = new AccessManager();
        this.auditLog = new AuditLog();
        this.storageService = new LongTermStorage();
        this.hashGenerator = new HashGenerator();
    }

    public int initializeCase(String title, LocalDateTime datetime , int investigatorID) {
        Case newCase = new Case(title, datetime);
        caseRepo.save(newCase);
        auditLog.logEvent(investigatorID,"Case Initialized", AuditCategory.CASE );
        return newCase.getCaseID();
    }

    public Case getCase(int caseID, int investigatorID) {
        if (!accessManager.verifyPrivileges(investigatorID, caseID)) {
            auditLog.logEvent(investigatorID, "Unauthorized Access Attempt", AuditCategory.SECURITY, caseID);
            throw new SecurityException("Access Denied");
        }
        return caseRepo.findById(caseID);
    }

    public boolean startNewInvestigation(int investigatorID) {
        if (!userAuth.verifyUserPermissions(investigatorID)) {
            auditLog.logEvent(investigatorID, "Unauthorized Investigation Start Attempt", AuditCategory.SECURITY);
            return false;
        }
        return true;
    }

    public List<Case> getClosedCases() {
        return caseRepo.findByFilter(CaseStatus.CLOSED);
    }

    /**
     * UC-12: Integrity Verification
     * Compares the DB hash (original) vs the Disk hash (current).
     */
    public boolean verifyDigitalFootprints(int investigatorID, int[] caseIDs) {
        boolean allIntact = true;

        for (int id : caseIDs) {
            List<Evidence> evidenceList = EvidenceRepo.findByCase(id);

            for (Evidence e : evidenceList) {
                String currentPhysicalHash = HashGenerator.generateSHA256(e.getFilePath());

                if (!e.verifyIntegrity(currentPhysicalHash)) {
                    auditLog.logEvent(investigatorID, "FORENSIC ALERT: Integrity Violation for Evidence " + e.getEvidenceID(), AuditCategory.EVIDENCE, id);
                    allIntact = false;
                }
            }
        }
        return allIntact;
    }

    /**
     * UC-12: Archiving Logic
     */
    public int removeFromActiveSpace(int investigatorID, int[] caseIDs) {
        int count = 0;
        for (int id : caseIDs) {
            try {
                if (!verifyDigitalFootprints(investigatorID, new int[]{id})) {
                    auditLog.logEvent(investigatorID, "Archive Aborted: Integrity check failed for Case " + id, AuditCategory.CASE, id);
                    continue;
                }

                Case c = caseRepo.findById(id);
                if (c != null) {
                    c.setStatus(CaseStatus.ARCHIVED);
                    caseRepo.update(c, "status = 'ARCHIVED'");

                    boolean moved = storageService.moveToArchive("data/cases/" + id);

                    if (moved) {
                        auditLog.logEvent(investigatorID, "Case Archived and Moved to Long Term Storage", AuditCategory.CASE, id);
                        count++;
                    }
                }
            } catch (Exception e) {
                auditLog.logEvent(investigatorID, "Archiving Failed for Case: " + id, AuditCategory.CASE, id);
            }
        }
        return count;
    }

    // UC 2 stuff

    /**
     * 1. Triggered by UI to fetch filter options (dropdowns, lists)
     */
    public List<String> openCaseSearchPanel() {
        // Returns the filter options to populate the JavaFX UI
        // khud change kar sakte agar kara, sirf dropdown hi ha
        return List.of("Status", "Investigator", "Date Range", "Priority");
    }

    /**
     * 2. Validates the search criteria entered by the user
     */
    public boolean selectFilter(int investigatorID, String criteria) {
        CaseFilter filter = new CaseFilter();
        boolean isValid = filter.validate(criteria);

        if (!isValid) {
            auditLog.logEvent(investigatorID, "Invalid Filter Criteria Attempted", AuditCategory.SECURITY);
        }
        return isValid;
    }

    /**
     * 3. Builds the query and fetches the data from the repository
     */
    public List<Case> applyFilter(int investigatorID, String criteria) {
        CaseFilter filter = new CaseFilter();

        // Fixed: Passed the 'criteria' into buildQuery so it knows what to search
        String query = filter.buildQuery(criteria);

        List<Case> matchingCases = caseRepo.findByFilter(query);
        auditLog.logEvent(investigatorID, "Filtered Case Search Performed", AuditCategory.CASE);

        return matchingCases;
    }

    /**
     * 4. Formats or verifies the final results before sending them back to the UI
     */
    public List<Case> reviewResults(List<Case> matchingCases) {
        // the controller might do final sorting or formatting here
        // before passing 'filteredCasesView' back to the JavaFX Dashboard.
        return matchingCases;
    }
}
