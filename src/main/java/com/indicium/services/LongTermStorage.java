package com.indicium.services;
import com.indicium.services.IStorageService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


public class LongTermStorage implements IStorageService
{
    private String rootDirectory = "C:/Indicium/";
    private String archiveFolder = "Archive/";
    private String dataFolder = "SavedData/";

    public LongTermStorage()
    {
        // Ensure the archive directory exists
        File dir = new File(rootDirectory);
        if (!dir.exists()) dir.mkdirs();
    }

    @Override
    public String saveFile(String filePath)
    {
        File source = new File(filePath);
        File dest = new File(rootDirectory + dataFolder + source.getName());

        // Make the file's copy and save it into Data folder
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean moveToArchive(String filePath)
    {
        File activeFile = new File(filePath);
        File archivedFile = new File(rootDirectory + archiveFolder + activeFile.getName());

        if(!activeFile.exists()) return false;

        // Move the file to the Archive folder removing it from the prev stored location
        return activeFile.renameTo(archivedFile);
    }

    @Override
    public File retrieveFromArchive(String filePath)
    {
        // filePath = rootDirectory + ArchiveFolder + fileName
        // Archived file is restored into Data folder

        File archivedFile = new File(filePath);
        File restoredFile = new File(rootDirectory + dataFolder + archivedFile.getName());

        if (!archivedFile.exists()) return null;

        try {
            new File(dataFolder).mkdirs();
            boolean moved = archivedFile.renameTo(restoredFile);

            if (moved) return restoredFile;
        }
        catch (Exception e) {
            return null;
        }
        return null;
    }
}
