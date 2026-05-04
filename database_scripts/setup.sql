-- INDICIUM DATABASE SETUP SCRIPT
-- Purpose: Initialize local forensic database schema

-- 1. Create and switch to the unified project database
CREATE DATABASE IF NOT EXISTS indicium;
USE indicium;

-- 2. Create Users Table (NEW)
-- Stores all system investigators and administrators
CREATE TABLE IF NOT EXISTS Users (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    FullName VARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Role VARCHAR(50) DEFAULT 'INVESTIGATOR',
    IsActive BOOLEAN DEFAULT TRUE
    );

-- Insert a default Admin account so you can log in immediately
INSERT IGNORE INTO Users (FullName, Email, PasswordHash, Role)
VALUES ('System Admin', 'admin@indicium.com', 'hashed_password_here', 'ADMIN');

-- 3. Create Cases Table
CREATE TABLE IF NOT EXISTS Cases (
    CaseID INT PRIMARY KEY,
    Title VARCHAR(255) NOT NULL,
    IncidentDate DATETIME NOT NULL,
    Status VARCHAR(50) DEFAULT 'OPEN'
    );

-- 4. Create Case Assignments Table (Bridging Table)
CREATE TABLE IF NOT EXISTS CaseAssignments (
    AssignmentID INT AUTO_INCREMENT PRIMARY KEY,
    CaseID INT NOT NULL,
    InvestigatorID INT NOT NULL,
    AssignedDate DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CaseID) REFERENCES Cases(CaseID) ON DELETE CASCADE,
    FOREIGN KEY (InvestigatorID) REFERENCES Users(UserID) ON DELETE CASCADE -- NEW: Links to Users
    );

-- 5. Create Evidence Table
CREATE TABLE IF NOT EXISTS Evidence (
    EvidenceID INT AUTO_INCREMENT PRIMARY KEY,
    CaseID INT NOT NULL,
    FileName VARCHAR(255) NOT NULL,
    FileHash VARCHAR(128) NOT NULL,
    StoragePath VARCHAR(512) NOT NULL,
    UploadTimestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    UploaderID INT,
    Status VARCHAR(50) DEFAULT 'ACTIVE',
    FOREIGN KEY (CaseID) REFERENCES Cases(CaseID) ON DELETE CASCADE,
    FOREIGN KEY (UploaderID) REFERENCES Users(UserID) ON DELETE SET NULL -- NEW: Links to Users
    );

-- 6. Create Timeline Events Table
CREATE TABLE IF NOT EXISTS timeline_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    case_id INT NOT NULL,
    description TEXT NOT NULL,
    event_timestamp DATETIME NOT NULL,
    linked_evidence_id INT DEFAULT NULL,
    FOREIGN KEY (case_id) REFERENCES Cases(CaseID) ON DELETE CASCADE
    );

-- 7. Create Forensic Audit Log Table
CREATE TABLE IF NOT EXISTS ForensicAuditLog (
    LogID INT AUTO_INCREMENT PRIMARY KEY,
    LogTimestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    Category VARCHAR(50) NOT NULL,
    Description TEXT NOT NULL,
    InvestigatorID INT NULL,
    LinkedCaseID INT NULL,
    LinkedEvidenceID INT NULL,
    FOREIGN KEY (InvestigatorID) REFERENCES Users(UserID) ON DELETE SET NULL -- NEW: Links to Users
    );