package com.indicium.controllers;
import com.indicium.models.Evidence;
import com.indicium.models.Case;
import com.indicium.services.AuditLog;
import com.indicium.services.HashGenerator;
import com.indicium.repository.EvidenceRepo;
import java.io.File;

enum EvidenceStatus
{
    COLLECTED,
    VERIFIED,
    LINKED,
    ARCHIVED,
    DISCARDED
}

public class EvidenceManager
{
    public Evidence ingestEvidence(int caseID, File file)
    {
        if (file == null || !file.exists())
        {
            System.out.println("[EvidenceManager] ERROR: File does not exist or is null.");
            return null;
        }

        // Step 1: Generate hash for the file (UC5 - HashGenerator)
       String fileHash = generateHash(file);
        if (fileHash == null)
        {
            System.out.println("[EvidenceManager] ERROR: Hash generation failed for: " + file.getName());
            return null;
        }

        // Step 2: Check for duplicate (UC5 - checkDuplicate)
       if (isDuplicate(fileHash, caseID))
       {
           System.out.println("[EvidenceManager] DUPLICATE DETECTED: " + file.getName() + " already exists in case " + caseID);
           return null;
       }

        // Step 3: Create evidence object (UC5 - <<create>> evidence:Evidence)
        Evidence evidence = new Evidence(file, fileHash);
        evidence.setStatus(EvidenceStatus.COLLECTED.ordinal());

        // Step 4: Link evidence to the case
        evidence.linkWithCase(caseID);

        // Step 5: Lock evidence after ingestion (UC5 - evidence is locked, cannot be edited)
        evidence.lock();

//        // Step 6: Save to repository (UC5 - AddEvidence)
        EvidenceRepo.add(evidence);
//
//        // Step 7: Log the event (UC5 - AddEvidenceLog(fileHash, Case))
        logEvidenceEvent("INGEST", evidence.getEvidenceID(), caseID, fileHash);

        System.out.println("[EvidenceManager] Evidence ingested successfully: ID=" + evidence.getEvidenceID());
        return evidence;
    }
}
