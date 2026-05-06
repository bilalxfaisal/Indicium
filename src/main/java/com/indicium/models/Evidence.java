package com.indicium.models;

import com.indicium.services.HashGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import com.indicium.models.EvidenceStatus;

public class Evidence
{

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
    String hash;

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
        this.hash = this.computeHash();
    }
    public Evidence(File file, String fingerprint)
    {
        this.setDefaultValues();
        this.evidenceFile = file;
        this.digitalFingerprint = fingerprint;
        this.evidenceID = idsCount++;
        this.isLocked = false;
        this.setDataFromFile(file);
        this.hash = this.computeHash();
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
        this.hash = null;
    }

    // used by admin to lock evidence
    public void lockEvidence()
    {
        this.isLocked = true;
    }

    // --- Verifications ---
    /**
     * UC5, UC7, UC10, UC12
     * Recomputes the hash of the file at filePath and compares
     * it against the stored digitalFingerprint.
     * Returns true if the file is intact, false if tampered.
     */

    private String computeHash()
    {
        return HashGenerator.generateSHA256(this.filePath);
    }
    public boolean verifyIntegrity(String hash)
    {
        if (this.digitalFingerprint == null || hash == null) {
            return false;
        }
        return this.digitalFingerprint.equals(hash);
    }

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

    // --- Getters ---

    // UC 7
    public File getFile() { return this.evidenceFile; }

    public int getEvidenceID()
    {
        return this.evidenceID;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getDigitalFingerprint() {
        return this.digitalFingerprint;
    }

    public boolean isLocked() { return this.isLocked; }

    public long getSize() { return this.size; }

    public String getType() { return this.type; }

    public List<Integer> linkedCaseIDs() { return this.caseIDs; }

    public String getHash() { return this.hash; }

    public String getName() { return this.name; }
     public EvidenceStatus getStatus() { return this.status; }

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

    public void setName(String name)           { this.name = name; }
    public void setFilePath(String path)       { this.filePath = path; }
    public void setDigitalFingerprint(String h){ this.digitalFingerprint = h; }
    public void setStatus(EvidenceStatus s)    { this.status = s; }
    public java.time.LocalDateTime getDateSeized() { return null; } // wire to DB column when added

}
