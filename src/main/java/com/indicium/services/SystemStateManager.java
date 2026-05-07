package com.indicium.services;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Persists critical system state (e.g., lockdown status) to a flat properties
 * file so the application can restore its state correctly after a restart.
 *
 * File location: system.state in the application working directory.
 */
public class SystemStateManager {

    private static final String STATE_FILE = "system.state";
    private static final String KEY_LOCKDOWN = "lockdown.active";
    private static final String KEY_LOCKDOWN_BY = "lockdown.by";

    // ── Singleton ──────────────────────────────────────────────────────────
    private static SystemStateManager instance;

    private SystemStateManager() { }

    public static SystemStateManager getInstance() {
        if (instance == null) {
            instance = new SystemStateManager();
        }
        return instance;
    }

    // ── Read ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the persisted state file records an active lockdown.
     * Returns false when the file is missing (first run / clean state).
     */
    public boolean isLockdownPersistedActive() {
        Properties props = load();
        return Boolean.parseBoolean(props.getProperty(KEY_LOCKDOWN, "false"));
    }

    public int getPersistedLockdownAdminId() {
        Properties props = load();
        try {
            return Integer.parseInt(props.getProperty(KEY_LOCKDOWN_BY, "-1"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ── Write ──────────────────────────────────────────────────────────────

    public void persistLockdownActive(int adminId) {
        Properties props = load();
        props.setProperty(KEY_LOCKDOWN, "true");
        props.setProperty(KEY_LOCKDOWN_BY, String.valueOf(adminId));
        save(props, "Lockdown activated by admin " + adminId);
        System.out.println("[SystemState] Lockdown state written to " + STATE_FILE);
    }

    public void persistLockdownLifted(int adminId) {
        Properties props = load();
        props.setProperty(KEY_LOCKDOWN, "false");
        props.setProperty(KEY_LOCKDOWN_BY, String.valueOf(adminId));
        save(props, "Lockdown lifted by admin " + adminId);
        System.out.println("[SystemState] Lockdown lifted — state written to " + STATE_FILE);
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private Properties load() {
        Properties props = new Properties();
        Path path = Paths.get(STATE_FILE);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("[SystemState] Could not read state file: " + e.getMessage());
            }
        }
        return props;
    }

    private void save(Properties props, String comment) {
        Path path = Paths.get(STATE_FILE);
        try (OutputStream out = Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(out, "Indicium System State — " + comment);
        } catch (IOException e) {
            System.err.println("[SystemState] Could not write state file: " + e.getMessage());
        }
    }
}
