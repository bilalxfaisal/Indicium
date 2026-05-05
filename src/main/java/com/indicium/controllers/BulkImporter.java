package com.indicium.controllers;

import com.indicium.models.Evidence;
import com.indicium.repository.EvidenceRepo;
import com.indicium.services.AuditLog;
import com.indicium.services.AuditCategory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BulkImporter
{
    private List<Evidence> pendingImportBuffer;

    public BulkImporter()
    {
        this.pendingImportBuffer = new ArrayList<>();
    }

    public void importAssets(String sourcePath, int targetCaseId, int userID)
    {
        this.selectSourceFolder(sourcePath, targetCaseId);

//        this.confirmImport(userID);
    }

    public void selectImportBulkData()
    {
        System.out.println("Displaying Bulk Import Dialog...");
    }

    public void selectSourceFolder(String path, int caseID)
    {
        File folder = new File(path);
        File[] files = folder.listFiles();

        if (files == null) return;

        this.pendingImportBuffer.clear();

        for (File file : files)
        {
            if (file.isFile())
            {
                Evidence evidence = new Evidence(file);

                boolean isDuplicate = EvidenceRepo.checkDuplicate(evidence.getHash());

                if (!isDuplicate)
                {
                    EvidenceRepo.add(evidence, caseID);
                    this.pendingImportBuffer.add(evidence);
                }
            }
        }

        this.generateFilePreview();
    }

    private void generateFilePreview()
    {
        for (Evidence e : this.pendingImportBuffer)
        {
            System.out.printf("Preview: %s | %d bytes | %s%n", e.getName(), e.getSize(), e.getType());
        }
    }

    public String confirmImport(int userID)
    {
        int count = this.pendingImportBuffer.size();

        AuditLog audit = new AuditLog();
        String report = audit.logEvent(userID, "BulkImportEvent", AuditCategory.USER);

        return report;
    }
}