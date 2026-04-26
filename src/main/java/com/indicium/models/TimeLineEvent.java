package com.indicium.models;

import com.indicium.repository.EvidenceRepo;
import java.time.LocalDateTime;

public class TimeLineEvent {
    private LocalDateTime timestamp;
    private String description; // Small 'd' is standard Java naming
    private int linkedEvidenceID;

    public TimeLineEvent(LocalDateTime timestamp, String description, int linkedEvidenceID) {
        this.timestamp = timestamp;
        this.description = description;
        this.linkedEvidenceID = linkedEvidenceID;
    }

    // Fixed Public Constructor
    public TimeLineEvent(int linkedEvidenceID, String description, int caseID) {
        if (!verify_case_belonging(linkedEvidenceID, caseID)) {
            throw new IllegalArgumentException("Evidence ID " + linkedEvidenceID + " does not belong to Case ID " + caseID);
        }

        this.timestamp = LocalDateTime.now();
        this.description = description;
        this.linkedEvidenceID = linkedEvidenceID;
    }

    // Logic to check if the evidence actually belongs to the case
    private boolean verify_case_belonging(int evidenceID, int caseID) {
        Evidence evidence = EvidenceRepo.getEvidence(evidenceID);
        if (evidence == null) {
            System.out.println("[TimeLineEvent] ERROR: Evidence ID " + evidenceID + " not found.");
            return false;
        }
        return evidence.verifyBelongsToCase(caseID);
    }
}