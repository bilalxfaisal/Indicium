package com.indicium.models;

import java.time.LocalDateTime;

public class CorrelationLink {

    private int           linkID;
    private int           srcEvidID;
    private String        srcEvidName;
    private String        srcEvidType;
    private int           srcCaseID;
    private String        srcCaseTitle;
    private int           tgtEvidID;
    private String        tgtEvidName;
    private String        tgtEvidType;
    private int           tgtCaseID;
    private String        tgtCaseTitle;
    private String        linkedBy;
    private LocalDateTime createdAt;

    public CorrelationLink() {}

    // ── Getters ──
    public int           getLinkID()      { return linkID; }
    public int           getSrcEvidID()   { return srcEvidID; }
    public String        getSrcEvidName() { return srcEvidName; }
    public String        getSrcEvidType() { return srcEvidType; }
    public int           getSrcCaseID()   { return srcCaseID; }
    public String        getSrcCaseTitle(){ return srcCaseTitle; }
    public int           getTgtEvidID()   { return tgtEvidID; }
    public String        getTgtEvidName() { return tgtEvidName; }
    public String        getTgtEvidType() { return tgtEvidType; }
    public int           getTgtCaseID()   { return tgtCaseID; }
    public String        getTgtCaseTitle(){ return tgtCaseTitle; }
    public String        getLinkedBy()    { return linkedBy; }
    public LocalDateTime getCreatedAt()   { return createdAt; }

    // ── Setters ──
    public void setLinkID(int v)           { this.linkID = v; }
    public void setSrcEvidID(int v)        { this.srcEvidID = v; }
    public void setSrcEvidName(String v)   { this.srcEvidName = v; }
    public void setSrcEvidType(String v)   { this.srcEvidType = v; }
    public void setSrcCaseID(int v)        { this.srcCaseID = v; }
    public void setSrcCaseTitle(String v)  { this.srcCaseTitle = v; }
    public void setTgtEvidID(int v)        { this.tgtEvidID = v; }
    public void setTgtEvidName(String v)   { this.tgtEvidName = v; }
    public void setTgtEvidType(String v)   { this.tgtEvidType = v; }
    public void setTgtCaseID(int v)        { this.tgtCaseID = v; }
    public void setTgtCaseTitle(String v)  { this.tgtCaseTitle = v; }
    public void setLinkedBy(String v)      { this.linkedBy = v; }
    public void setCreatedAt(LocalDateTime v){ this.createdAt = v; }
}
