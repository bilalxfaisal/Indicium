package com.indicium.models;

import java.time.LocalDateTime;

public class TimeLineEvent {

    private int           eventID;
    private int           caseID;
    private String        title;
    private String        description;
    private LocalDateTime timestamp;
    private int           linkedEvidenceID;
    private String        addedBy;

    public TimeLineEvent() {}

    public TimeLineEvent(int caseID, String title, String description,
                         LocalDateTime timestamp, int linkedEvidenceID, String addedBy) {
        this.caseID           = caseID;
        this.title            = title;
        this.description      = description;
        this.timestamp        = timestamp;
        this.linkedEvidenceID = linkedEvidenceID;
        this.addedBy          = addedBy;
    }

    // ── Getters ──
    public int           getEventID()         { return eventID; }
    public int           getCaseID()          { return caseID; }
    public String        getTitle()           { return title; }
    public String        getDescription()     { return description; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public int           getLinkedEvidenceID(){ return linkedEvidenceID; }
    public String        getAddedBy()         { return addedBy; }

    // ── Setters ──
    public void setEventID(int eventID)                   { this.eventID = eventID; }
    public void setCaseID(int caseID)                     { this.caseID = caseID; }
    public void setTitle(String title)                    { this.title = title; }
    public void setDescription(String description)        { this.description = description; }
    public void setTimestamp(LocalDateTime timestamp)     { this.timestamp = timestamp; }
    public void setLinkedEvidenceID(int linkedEvidenceID) { this.linkedEvidenceID = linkedEvidenceID; }
    public void setAddedBy(String addedBy)                { this.addedBy = addedBy; }
}
