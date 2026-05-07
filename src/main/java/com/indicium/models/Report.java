package com.indicium.models;

import com.indicium.services.HashGenerator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Report
{
    private static int idCounter = 1; // Acts as auto-increment until DB storage is required

    private int reportID;
    private int caseID;
    private String reportType;
    private String format;
    private LocalDateTime generatedDate;
    private String content;
    private String reportHash;
    private String generationDate;

    public Report(int caseID, String reportType, String format) {
        this.reportID = idCounter++;
        this.caseID = caseID;
        this.reportType = reportType;
        this.format = format;
        this.generatedDate = LocalDateTime.now();
    }

    /**
     * Formats the raw case data and seals it with a cryptographic hash.
     */
    public void formatAndHashReport(String caseData) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 1. Format the content for the PDF layout
        this.content =
                "INDICIUM FORENSIC REPORT\n" +
                        "====================================================\n" +
                        "Report ID      : " + this.reportID + "\n" +
                        "Case ID        : " + this.caseID + "\n" +
                        "Report Type    : " + this.reportType + "\n" +
                        "Generated On   : " + dtf.format(this.generatedDate) + "\n" +
                        "====================================================\n\n" +
                        "CASE SUMMARY DATA:\n" +
                        "----------------------------------------------------\n" +
                        caseData;

        // 2. Hash the final string to create a digital seal
        this.reportHash = HashGenerator.generateSHA256FromString(this.content);
    }

    // --- Getters ---
    public int getReportID() { return reportID; }
    public int getCaseID() { return caseID; }
    public String getContent() { return content; }
    public String getFormat() { return format; }
    public String getReportHash() { return reportHash; }
    public String getGenerationDate() { return generationDate; }
}
