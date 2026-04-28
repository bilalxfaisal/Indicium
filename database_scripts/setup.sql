-- INDICIUM DATABASE SETUP SCRIPT
-- Purpose: Initialize local forensic database schema

-- 1. Create and switch to the unified project database
CREATE DATABASE IF NOT EXISTS indicium;
USE indicium;

-- 2. Create Timeline Events Table (For UC-9: Manage Case Timeline)
-- Tracks the narrative of the incident and links events to evidence
CREATE TABLE IF NOT EXISTS timeline_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    case_id INT NOT NULL,
    description TEXT NOT NULL,
    event_timestamp DATETIME NOT NULL,
    linked_evidence_id INT DEFAULT NULL
);

-- 3. Create Forensic Audit Log Table (For System Integrity)
-- Tracks all user actions, security events, and modifications
CREATE TABLE IF NOT EXISTS ForensicAuditLog (
    LogID INT AUTO_INCREMENT PRIMARY KEY,
    LogTimestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    Category VARCHAR(50) NOT NULL,
    Description TEXT NOT NULL,
    InvestigatorID INT NULL,
    LinkedCaseID INT NULL,         -- Links to the Case
    LinkedEvidenceID INT NULL      -- NEW: Links to the specific Evidence
    );