package com.indicium.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

enum EvidenceStatus
{
    COLLECTED,
    VERIFIED,
    LINKED,
    ARCHIVED,
    DISCARDED
}

public class Evidence {

    private static int idsCount = 1;
    private String name;
    private int evidenceID;
    private String filePath;
    private String digitalFingerprint;
    List<Integer> caseIDs;
    private boolean isLocked;
    private long size;
    private String type;
    File evidenceFile;
    EvidenceStatus status; // verified, discarded, archived, collected
    boolean fileExists;

    // --- Constructors ---
    public Evidence()
    {
        this.setDefaultValues();
        this.evidenceID = idsCount++;
    }
    public Evidence(File file)
    {
        this.setDefaultValues();
        this.evidenceID = idsCount++;
        this.isLocked = false;
        this.digitalFingerprint = null;
        this.setDataFromFile(file);
    }
    public Evidence(File file, String fingerprint)
    {
        this.setDefaultValues();
        this.evidenceFile = file;
        this.digitalFingerprint = fingerprint;
        this.evidenceID = idsCount++;
        this.isLocked = false;
        this.setDataFromFile(file);
    }

    public void setDataFromFile(File file)
    {
        if (file == null || !file.exists())
        {
            System.out.println("[Evidence] ERROR: File does not exist or is null.");
            fileExists = false;
            return;
        }
        fileExists = true;
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        this.filePath = file.getAbsolutePath();
        this.size = file.length();
        this.name = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
        this.type = (dotIndex > 0) ? fileName.substring(dotIndex + 1) : "";
    }

    private void setDefaultValues()
    {
        this.name = null;
        this.evidenceID = 0;
        this.filePath = null;
        this.digitalFingerprint = null;
        this.caseIDs = null;
        this.isLocked = false;
        this.size = 0;
        this.type = null;
        this.evidenceFile = null;
        this.status = null;
        fileExists = false;
    }

    // used by admin to lock evidence
    public void lockEvidence()
    {
        this.isLocked = true;
    }

    /**
     * UC5, UC7, UC10, UC12
     * Recomputes the hash of the file at filePath and compares
     * it against the stored digitalFingerprint.
     * Returns true if the file is intact, false if tampered.
     */
    public boolean verifyIntegrity(String hash) {
        if (this.digitalFingerprint == null || hash == null) {
            return false;
        }
        return this.digitalFingerprint.equals(hash);
    }

    // --- Getters ---

    // UC 7
    public File getFile() { return evidenceFile; }

    public int getEvidenceID()
    {
        return evidenceID;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getDigitalFingerprint() {
        return digitalFingerprint;
    }

    public boolean isLocked() { return isLocked; }

    public long getSize() { return size; }

    public String getType() { return type; }

    public List<Integer> linkedCaseIDs() { return caseIDs; }

    // --- Setters ---

    public void setEvidenceID(int evidenceID) {
        this.evidenceID = evidenceID;
    }

    public void setStatus(int statusNum)
    {
        this.status = EvidenceStatus.values()[statusNum];
    }
    /**
     * UC5: Lock the evidence record after successful ingestion.
     * Once locked, the file cannot be edited.
     */
    public void lock() {
        this.isLocked = true;
    }
    public void unlock() { this.isLocked = false; }

    // checks if evidence belongs to the case given
    public boolean verifyBelongsToCase(int caseID)
    {
        if (this.caseIDs == null) return false;
        int i = 0;
        while (i < caseIDs.size())
        {
            if (i == caseID) return true;
        }
        return false;
    }

    public void linkWithCase(int caseID)
    {
        if (this.caseIDs == null) caseIDs = new ArrayList<>();
        this.caseIDs.add(caseID);
    }

    public void displayEvidence()
    {
        System.out.println(
                "========== Evidence Details ==========\n" +
                        "Evidence ID   : " + evidenceID + "\n" +
                        "Name          : " + name + "\n" +
                        "Type          : " + type + "\n" +
                        "Size (bytes)  : " + size + "\n" +
                        "File Path     : " + filePath + "\n" +
                        "Fingerprint   : " + digitalFingerprint + "\n" +
                        "Locked        : " + isLocked + "\n" +
                        "Status        : " + status + "\n" +
                        "Case IDs      : " + caseIDs + "\n" +
                        "======================================"
        );
    }
}