CREATE DATABASE IF NOT EXISTS timeline_db;
USE timeline_db;

CREATE TABLE IF NOT EXISTS timeline_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    case_id INT NOT NULL,
    description TEXT NOT NULL,
    event_timestamp DATETIME NOT NULL,
    linked_evidence_id INT DEFAULT NULL
);