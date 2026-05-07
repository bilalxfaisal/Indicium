package com.indicium.services;

import com.indicium.models.Case;
import com.indicium.repository.CaseRepository;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;
import com.indicium.services.SystemStateManager;

public class AccessManager
{
    private final AuditLog auditLog;

    /**
     * Static lockdown flag — initialised from the persisted system.state file
     * so that a lockdown survives application restarts.
     */
    private static boolean isLockdownActive =
            SystemStateManager.getInstance().isLockdownPersistedActive();

    static {
        if (isLockdownActive) {
            System.out.println("[AccessManager] LOCKDOWN state restored from system.state — system is LOCKED.");
        }
    }

    public AccessManager() {
        this.auditLog = new AuditLog();
    }

    /**
     * Corresponds to Message #1 & #3 in the Authorization Sequence Diagram.
     * Verifies if an investigator has standard reading/viewing privileges for a case.
     */
    public boolean verifyPrivileges(int investigatorID, int caseID) {
        if (isLockdownActive) {
            System.out.println("[AccessManager] SYSTEM LOCKDOWN ACTIVE. All access denied.");
            auditLog.logEvent(investigatorID, "Lockdown Block: Access Denied", AuditCategory.SECURITY, caseID);
            return false;
        }

        // 1. Fetch the Case to check its internal status (e.g., is it ARCHIVED?)
        CaseRepository repo = new CaseRepository();
        Case targetCase = repo.findById(caseID);

        if (targetCase == null) {
            auditLog.logEvent(investigatorID, "Access Attempt on Non-Existent Case", AuditCategory.SECURITY, caseID);
            return false;
        }

        // 2. Delegate to the Case model to verify standard privileges (Message #3)
        boolean hasAccess = targetCase.verifyStandardViewingPrivileges(investigatorID);

        // 3. Log the outcome (Message #5)
        if (hasAccess) {
            auditLog.logEvent(investigatorID, "Read Access Granted", AuditCategory.SECURITY, caseID);
        } else {
            auditLog.logEvent(investigatorID, "Read Access Denied", AuditCategory.SECURITY, caseID);
        }

        return hasAccess;
    }

    /**
     * Often used before allowing modifications (e.g., UC#6: Modify Case Specifications).
     * Modification might require stricter checks than standard viewing.
     */
    public boolean authorizeEdit(int investigatorID, int caseID) {
        if (isLockdownActive) {
            auditLog.logEvent(investigatorID, "Lockdown Block: Edit Denied", AuditCategory.SECURITY, caseID);
            return false;
        }

        // For now, edit privileges mirror viewing privileges.
        // In a more complex system, you might check a specific "CanEdit" flag in the DB.
        boolean canEdit = verifyPrivileges(investigatorID, caseID);

        if (canEdit) {
            auditLog.logEvent(investigatorID, "Edit Access Granted", AuditCategory.SECURITY, caseID);
        } else {
            auditLog.logEvent(investigatorID, "Edit Access Denied", AuditCategory.SECURITY, caseID);
        }

        return canEdit;
    }

    // --- System Administrator Functions ---

    public static void activateLockdown(int adminID, AuditLog log) {
        isLockdownActive = true;
        // Persist to disk so lockdown survives app restarts
        SystemStateManager.getInstance().persistLockdownActive(adminID);
        log.logEvent(adminID, "SYSTEM LOCKDOWN INITIATED", AuditCategory.SYSTEM);
        System.out.println("[CRITICAL] System is now in Lockdown Mode.");
    }

    public static void liftLockdown(int adminID, AuditLog log) {
        isLockdownActive = false;
        // Clear persisted lockdown flag
        SystemStateManager.getInstance().persistLockdownLifted(adminID);
        log.logEvent(adminID, "SYSTEM LOCKDOWN LIFTED", AuditCategory.SYSTEM);
        System.out.println("[INFO] System Lockdown has been lifted.");
    }

    public static boolean isLockdownActive() {
        return isLockdownActive;
    }
}