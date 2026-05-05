package com.indicium.controllers;
import com.indicium.models.Evidence;
import com.indicium.models.Case;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;
import com.indicium.services.HashGenerator;
import com.indicium.repository.EvidenceRepo;
import com.indicium.ui.EvidenceDashBoardController;

import java.io.File;
import java.time.LocalDateTime;

import com.indicium.models.EvidenceStatus;

public class EvidenceManager
{
    public Evidence ingestEvidence(int userID, int caseID, String caseTitle, File file)
    {
        if (file == null || !file.exists())
        {
            System.out.println("[EvidenceManager] ERROR: File does not exist or is null.");
            return null;
        }

        // Step 1: Generate hash for the file (UC5 - HashGenerator)
        String fileHash = HashGenerator.generateSHA256(file.getPath());
        if (fileHash == null)
        {
            System.out.println("[EvidenceManager] ERROR: Hash generation failed for: " + file.getName());
            return null;
        }

        // Step 2: Check for duplicate (UC5 - checkDuplicate)
        Case case_ = new Case(caseID, caseTitle, LocalDateTime.now());
        if (case_.checkDuplicate(fileHash))
        {
            System.out.println("[EvidenceManager] DUPLICATE DETECTED: " + file.getName() + " already exists in case " + caseID);
            return null;
        }

        // Step 3: Create evidence object (UC5 - <<create>> evidence:Evidence)
        Evidence evidence = new Evidence(file, fileHash);
        evidence.setStatus(EvidenceStatus.COLLECTED.ordinal());

        // Step 4: Link evidence to the case
        evidence.linkWithCase(caseID);
        case_.addEvidence(evidence);

        // Step 5: Lock evidence after ingestion (UC5 - evidence is locked, cannot be edited)
        evidence.lock();

        // Step 6: Save to repository (UC5 - AddEvidence)
        EvidenceRepo.add(evidence, caseID);

        AuditLog audit = new AuditLog();
        // Step 7: Log the event (UC5 - AddEvidenceLog(fileHash, Case))
        audit.logEvent(userID, "INGEST", AuditCategory.EVIDENCE, caseID, evidence.getEvidenceID());

        System.out.println("[EvidenceManager] Evidence ingested successfully: ID=" + evidence.getEvidenceID());
        return evidence;
    }

    public void requestMedia(String actionType, int evidenceID, int caseID, int userID)
    {
        // Step # 1: Get Evidence Details
        Evidence evidence = EvidenceRepo.getEvidence(evidenceID);
        if (evidence == null)
        {
            System.out.println("[EvidenceManager] ERROR: No evidence with ID=" + evidenceID + " exists.");
            return;
        }

        File file = evidence.getFile();
        String hash = evidence.getDigitalFingerprint();

        if (HashGenerator.verifyHash(evidence.getFile(), hash) == false)
        {
            System.out.println("[EvidenceManager] ERROR: Hash not verified.");
            return;
        }

        AuditLog audit = new AuditLog();
        if (actionType.equals("View"))
        {
            audit.logEvent(userID, "View", AuditCategory.EVIDENCE, caseID, evidenceID);
            //EvidenceDashBoardController.openOnlinePlayer(file);
        }
        else if (actionType.equals("Download"))
        {
            audit.logEvent(userID, "Download", AuditCategory.EVIDENCE, caseID, evidenceID);
            //EvidenceDashBoardController.askForConfirmation();
        }
    }
}
