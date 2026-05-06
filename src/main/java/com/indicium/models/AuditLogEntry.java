package com.indicium.models;

import java.time.LocalDateTime;

public class AuditLogEntry {

    private int           logID;
    private String        category;
    private String        description;
    private int           investigatorID;
    private int           linkedCaseID;
    private int           linkedEvidenceID;
    private LocalDateTime timestamp;
    private String        fullName;
    private String        role;

    public AuditLogEntry() {}

    // ── Getters ──
    public int           getLogID()           { return logID; }
    public String        getCategory()        { return category; }
    public String        getDescription()     { return description; }
    public int           getInvestigatorID()  { return investigatorID; }
    public int           getLinkedCaseID()    { return linkedCaseID; }
    public int           getLinkedEvidenceID(){ return linkedEvidenceID; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public String        getFullName()        { return fullName; }
    public String        getRole()            { return role; }

    // ── Setters ──
    public void setLogID(int logID)                       { this.logID = logID; }
    public void setCategory(String category)              { this.category = category; }
    public void setDescription(String description)        { this.description = description; }
    public void setInvestigatorID(int investigatorID)     { this.investigatorID = investigatorID; }
    public void setLinkedCaseID(int linkedCaseID)         { this.linkedCaseID = linkedCaseID; }
    public void setLinkedEvidenceID(int linkedEvidenceID) { this.linkedEvidenceID = linkedEvidenceID; }
    public void setTimestamp(LocalDateTime timestamp)     { this.timestamp = timestamp; }
    public void setFullName(String fullName)              { this.fullName = fullName; }
    public void setRole(String role)                      { this.role = role; }
}
