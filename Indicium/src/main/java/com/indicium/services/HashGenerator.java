package com.indicium.services;

import com.indicium.models.Evidence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGenerator {

    public String generateSHA256(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int nread;
            while ((nread = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, nread);
            }
            return bytesToHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            // Log this specifically for AuditLog later
            System.err.println("Forensic Hash Failure: " + e.getMessage());
            return null;
        }
    }

    // Helper method to keep the main logic clean
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public boolean verifyHash(File evidenceFile, String originalHash) {
        String computedHash = generateSHA256(evidenceFile.getAbsolutePath());
        return computedHash != null && computedHash.equalsIgnoreCase(originalHash);
    }
}