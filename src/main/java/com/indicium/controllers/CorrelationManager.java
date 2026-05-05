package com.indicium.services;

import com.indicium.models.Evidence;
import com.indicium.repository.CaseRepository;
import com.indicium.services.AuditCategory;
import com.indicium.services.AuditLog;

public class CorrelationManager
{
    private AuditLog auditLog;

    public CorrelationManager()
    {
        this.auditLog = new AuditLog();
    }

    public String initiateLink(int investigatorID, int sourceEvidenceID, int targetCaseID)
    {
        // Trigger "Authorize Case Access" event for target case
        boolean isAuthorized = CaseRepository.isUserAssignedToCase(investigatorID, targetCaseID);

        if (!isAuthorized)
        {
            // Extension 3a: Authorization Failure for Target Case
            auditLog.logEvent(investigatorID, "Unauthorized link attempt to Case " + targetCaseID, AuditCategory.SECURITY);
            return "Access Denied: You are ineligible for access to Case ID " + targetCaseID;
        }

        // Log the initiation to audit trail
        auditLog.logEvent(investigatorID, "Initiated search for correlation from Evidence " + sourceEvidenceID, AuditCategory.EVIDENCE);

        return "Authorized: Connection established to Case " + targetCaseID + ". Please select target evidence.";
    }

    /**
     * UC8: Map Cross-Case Correlations (Step 2: Linking)
     * Finalizes the link between two evidence objects and logs the record.
     */
    public String createCrossCaseLink(int investigatorID, Evidence sourceEvidence, int targetCaseID, Evidence targetEvidence) {

        // 1. Double-check authorization before final submission
        if (!CaseRepository.isUserAssignedToCase(investigatorID, targetCaseID))
        {
            return "Security Error: Session expired or authorization revoked for target case.";
        }

        // 2. Extension 5a: Existing Link Conflict
        // Prevents linking the same item or items that already share a connection
        if (sourceEvidence.getEvidenceID() == targetEvidence.getEvidenceID())
        {
            return "Conflict: Redundant link. This evidence is already in your workspace.";
        }

        // 3. Link the two evidence objects together
        // Updates the objects without modifying the original evidence files
        sourceEvidence.linkWithCase(targetCaseID);
        sourceEvidence.linkWithCase(targetEvidence.getEvidenceID());

        // 4. Record timestamped entry to audit log
        auditLog.logEvent(
                investigatorID,
                "Established cross-case link: Evid " + sourceEvidence.getEvidenceID() + " <-> Evid " + targetEvidence.getEvidenceID(),
                AuditCategory.EVIDENCE,
                targetCaseID,
                sourceEvidence.getEvidenceID()
        );

        return "Success: Secure connection record established between evidence items.";
    }
}