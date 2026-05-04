-- INDICIUM DATABASE SETUP SCRIPT
-- Purpose: Initialize local forensic database schema

-- 1. Create and switch to the unified project database
CREATE DATABASE IF NOT EXISTS indicium;
USE indicium;

-- 2. Create Cases Table
-- NOTE: CaseID is NOT Auto-Increment because Java generates it via hashCode()
CREATE TABLE IF NOT EXISTS Cases (
                                     CaseID INT PRIMARY KEY,
                                     Title VARCHAR(255) NOT NULL,
    IncidentDate DATETIME NOT NULL,
    Status VARCHAR(50) DEFAULT 'OPEN'
    );

-- 3. Create Case Assignments Table (Bridging Table)
-- Manages access control for Investigators (UC-2 / Privileges)
CREATE TABLE IF NOT EXISTS CaseAssignments (
                                               AssignmentID INT AUTO_INCREMENT PRIMARY KEY,
                                               CaseID INT NOT NULL,
                                               InvestigatorID INT NOT NULL,
                                               AssignedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
                                               FOREIGN KEY (CaseID) REFERENCES Cases(CaseID) ON DELETE CASCADE
    );

-- 4. Create Evidence Table
CREATE TABLE IF NOT EXISTS Evidence (
                                        EvidenceID INT AUTO_INCREMENT PRIMARY KEY,
                                        CaseID INT NOT NULL,
                                        FileName VARCHAR(255) NOT NULL,
    FileHash VARCHAR(128) NOT NULL,
    StoragePath VARCHAR(512) NOT NULL,
    UploadTimestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    UploaderID INT,
    Status VARCHAR(50) DEFAULT 'ACTIVE',
    FOREIGN KEY (CaseID) REFERENCES Cases(CaseID) ON DELETE CASCADE
    );

-- 5. Create Timeline Events Table (For UC-9: Manage Case Timeline)
CREATE TABLE IF NOT EXISTS timeline_events (
                                               event_id INT AUTO_INCREMENT PRIMARY KEY,
                                               case_id INT NOT NULL,
                                               description TEXT NOT NULL,
                                               event_timestamp DATETIME NOT NULL,
                                               linked_evidence_id INT DEFAULT NULL,
                                               FOREIGN KEY (case_id) REFERENCES Cases(CaseID) ON DELETE CASCADE
    );

-- 6. Create Forensic Audit Log Table (For System Integrity)
CREATE TABLE IF NOT EXISTS ForensicAuditLog (
                                                LogID INT AUTO_INCREMENT PRIMARY KEY,
                                                LogTimestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                                                Category VARCHAR(50) NOT NULL,
    Description TEXT NOT NULL,
    InvestigatorID INT NULL,
    LinkedCaseID INT NULL,
    LinkedEvidenceID INT NULL
    );