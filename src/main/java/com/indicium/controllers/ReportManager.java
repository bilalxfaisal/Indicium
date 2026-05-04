package com.indicium.controllers;

import com.indicium.models.Case;
import com.indicium.models.Report;
import com.indicium.repository.CaseRepository;
import com.indicium.services.AccessManager;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;

// iText PDF Imports
import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;

public class ReportManager {

    private final CaseRepository caseRepo;
    private final AccessManager accessManager;
    private final AuditLog auditLog;

    public ReportManager() {
        this.caseRepo = new CaseRepository();
        this.accessManager = new AccessManager();
        this.auditLog = new AuditLog();
    }

    public Report generateReport(int caseID, int investigatorID, String reportType, String format) {

        // 1. Verify Privileges
        if (!accessManager.verifyPrivileges(investigatorID, caseID)) {
            // UPDATED: Using your specific AuditLog signature
            auditLog.logEvent(investigatorID, "Unauthorized Report Generation Attempt", AuditCategory.SECURITY, caseID);
            System.out.println("[ReportManager] Exception 2a: Permission Denied.");
            return null;
        }

        // 2. Fetch Case
        Case targetCase = caseRepo.findById(caseID);
        if (targetCase == null) {
            System.out.println("[ReportManager] ERROR: Case not found.");
            return null;
        }

        // 3. Verify Dataset isn't empty
        if (targetCase.getEvidenceList().isEmpty() && targetCase.getTimeLineEvents().isEmpty()) {
            System.out.println("[ReportManager] Exception 3a: Empty Dataset. Halting process.");
            return null;
        }

        // 4. Gather Data and Generate Model
        String caseData = targetCase.getDetails();
        Report newReport = new Report(caseID, reportType, "PDF"); // Forcing PDF format for export
        newReport.formatAndHashReport(caseData);

        return newReport;
    }

    /**
     * Generates a physical PDF on the disk and returns the absolute file path.
     */
    public String confirmSave(Report report, String targetDirectoryPath, int investigatorID) {
        if (report == null) return null;

        File directory = new File(targetDirectoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "Forensic_Report_Case_" + report.getCaseID() + "_" + report.getReportID() + ".pdf";
        File file = new File(directory, fileName);

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Setup fonts
            Font standardFont = FontFactory.getFont(FontFactory.COURIER, 11);
            Font boldFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 12);

            // Write content
            document.add(new Paragraph(report.getContent(), standardFont));
            document.add(new Paragraph("\n\n"));

            // Write Digital Seal
            document.add(new Paragraph("=== END OF REPORT ===", boldFont));
            document.add(new Paragraph("DIGITAL SEAL (SHA-256): " + report.getReportHash(), boldFont));

            document.close();

            // UPDATED: Using your specific AuditLog signature for successful export
            auditLog.logEvent(investigatorID, "Generated Forensic PDF Report (ID: " + report.getReportID() + ")", AuditCategory.CASE, report.getCaseID());

            System.out.println("[ReportManager] PDF Report securely saved to: " + file.getAbsolutePath());
            return file.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("[ReportManager] CRITICAL ERROR generating PDF: " + e.getMessage());
            // Log the failure
            auditLog.logEvent(investigatorID, "Failed to generate PDF Report", AuditCategory.SYSTEM, report.getCaseID());
            return null;
        }
    }
}