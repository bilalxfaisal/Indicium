package com.indicium.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class IntegrityManagerDashboardController extends StackPane {

    // ── Header ──
    @FXML private Label  lblSystemStatus;
    @FXML private HBox   systemStatusChip;
    @FXML private StackPane statusDot;

    // ── Stats ──
    @FXML private Label statActiveCases;
    @FXML private Label statArchivedCases;
    @FXML private Label statStorageUsed;
    @FXML private Label statIntegrityFails;

    // ── Archive section ──
    @FXML private TextField archiveSearchField;
    @FXML private VBox      archiveCaseList;
    @FXML private VBox      archiveEmptyHint;
    @FXML private HBox      archiveActionBar;
    @FXML private Label     lblArchiveSelected;
    @FXML private Button    btnArchiveSelected;

    // ── Restore section ──
    @FXML private TextField restoreSearchField;
    @FXML private VBox      restoreCaseList;
    @FXML private VBox      restoreEmptyHint;
    @FXML private HBox      restoreActionBar;
    @FXML private Label     lblRestoreSelected;
    @FXML private Button    btnRestoreSelected;

    // ── Lockdown ──
    @FXML private Button btnLockdown;

    // ── Archive modal ──
    @FXML private StackPane archiveModal;
    @FXML private Label     archiveModalSummary;
    @FXML private Button    btnConfirmArchive;

    // ── Restore modal ──
    @FXML private StackPane restoreModal;
    @FXML private Label     restoreModalSummary;
    @FXML private Button    btnConfirmRestore;

    // ── Lockdown modal ──
    @FXML private StackPane lockdownModal;
    @FXML private TextField lockdownCodeField;
    @FXML private Label     lockdownError;
    @FXML private Button    btnConfirmLockdown;

    // ── Processing overlay ──
    @FXML private StackPane processingOverlay;
    @FXML private Label     lblProcessingTitle;
    @FXML private Label     lblProcessingPct;
    @FXML private Label     lblProcessingStep;
    @FXML private StackPane processingProgressFill;

    // ── Selection state ──
    private final List<String> selectedArchiveCaseIds = new ArrayList<>();
    private final List<String> selectedRestoreCaseIds = new ArrayList<>();

    // ── Constructor ──
    public IntegrityManagerDashboardController() {
        URL fxmlUrl = getClass().getResource(
                "/com/indicium/ui/IntegrityManagerDashboard.fxml"
        );
        if (fxmlUrl == null)
            throw new RuntimeException("IntegrityManagerDashboard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load IntegrityManagerDashboard.fxml: " + e.getMessage(), e
            );
        }
    }

    @FXML
    public void initialize() {
        loadStats();
    }

    // ══════════════════════════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════════════════════════

    private void loadStats() {
        // TODO: SELECT COUNT(*) FROM cases WHERE status = 'ACTIVE'
        statActiveCases.setText("—");

        // TODO: SELECT COUNT(*) FROM cases WHERE status = 'ARCHIVED'
        statArchivedCases.setText("—");

        // TODO: SELECT SUM(file_size) FROM evidence
        //       Format result as "4.2 GB" etc.
        statStorageUsed.setText("—");

        // TODO: SELECT COUNT(*) FROM evidence
        //       WHERE current_hash != original_hash
        statIntegrityFails.setText("—");
    }

    // ══════════════════════════════════════════════════════════
    //  ARCHIVE — Search & Select
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleArchiveSearch() {
        String query = archiveSearchField.getText().trim().toLowerCase();
        archiveCaseList.getChildren().clear();
        archiveCaseList.getChildren().add(archiveEmptyHint);
        selectedArchiveCaseIds.clear();
        updateArchiveActionBar();

        if (query.isEmpty()) return;

        // TODO: SELECT case_id, title, closed_date, evidence_count
        //       FROM cases
        //       WHERE status = 'CLOSED'
        //       AND (LOWER(case_id) LIKE ? OR LOWER(title) LIKE ?)
        //       ORDER BY closed_date ASC
        // TODO: For each result call buildIntegrityRow(
        //           archiveCaseList, caseId, title, closedDate,
        //           evidenceCount, "VERIFIED", true)

        archiveEmptyHint.setVisible(false);
        archiveEmptyHint.setManaged(false);
    }

    @FXML
    private void handleClearArchiveSelection() {
        selectedArchiveCaseIds.clear();
        refreshRowSelectionUI(archiveCaseList, selectedArchiveCaseIds);
        updateArchiveActionBar();
    }

    private void updateArchiveActionBar() {
        int count = selectedArchiveCaseIds.size();
        boolean show = count > 0;
        archiveActionBar.setVisible(show);
        archiveActionBar.setManaged(show);
        if (show)
            lblArchiveSelected.setText(count + " case" + (count > 1 ? "s" : "") + " selected");
    }

    // ══════════════════════════════════════════════════════════
    //  RESTORE — Search & Select
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleRestoreSearch() {
        String query = restoreSearchField.getText().trim().toLowerCase();
        restoreCaseList.getChildren().clear();
        restoreCaseList.getChildren().add(restoreEmptyHint);
        selectedRestoreCaseIds.clear();
        updateRestoreActionBar();

        if (query.isEmpty()) return;

        // TODO: SELECT case_id, title, archived_date, evidence_count
        //       FROM cases
        //       WHERE status = 'ARCHIVED'
        //       AND (LOWER(case_id) LIKE ? OR LOWER(title) LIKE ?)
        //       ORDER BY archived_date DESC
        // TODO: For each result call buildIntegrityRow(
        //           restoreCaseList, caseId, title, archivedDate,
        //           evidenceCount, "VERIFIED", false)

        restoreEmptyHint.setVisible(false);
        restoreEmptyHint.setManaged(false);
    }

    @FXML
    private void handleClearRestoreSelection() {
        selectedRestoreCaseIds.clear();
        refreshRowSelectionUI(restoreCaseList, selectedRestoreCaseIds);
        updateRestoreActionBar();
    }

    private void updateRestoreActionBar() {
        int count = selectedRestoreCaseIds.size();
        boolean show = count > 0;
        restoreActionBar.setVisible(show);
        restoreActionBar.setManaged(show);
        if (show)
            lblRestoreSelected.setText(count + " case" + (count > 1 ? "s" : "") + " selected");
    }

    // ══════════════════════════════════════════════════════════
    //  ROW BUILDER
    // ══════════════════════════════════════════════════════════

    /**
     * Builds a single selectable case row for archive or restore list.
     *
     * @param container    archiveCaseList or restoreCaseList
     * @param caseId       case primary key string
     * @param title        case title
     * @param date         closed/archived date string
     * @param evCount      number of evidence files
     * @param hashStatus   "VERIFIED" | "PENDING" | "FAILED"
     * @param isArchiveRow true = adds to archive selection, false = restore
     */
    private void buildIntegrityRow(VBox container, String caseId, String title,
                                   String date, int evCount,
                                   String hashStatus, boolean isArchiveRow) {
        HBox row = new HBox(12);
        row.getStyleClass().add("integrity-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(caseId);

        // Checkbox
        StackPane checkBox = new StackPane();
        checkBox.getStyleClass().add("row-select-box");
        Label checkMark = new Label("");
        checkMark.getStyleClass().add("row-check-label");
        checkBox.getChildren().add(checkMark);

        // Case info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblId    = new Label(caseId);
        lblId.getStyleClass().add("integrity-case-id");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("integrity-case-title");
        Label lblMeta  = new Label(evCount + " evidence files  ·  " + date);
        lblMeta.getStyleClass().add("integrity-case-meta");
        info.getChildren().addAll(lblId, lblTitle, lblMeta);

        // Hash status chip
        Label hashChip = new Label(hashStatus);
        hashChip.getStyleClass().addAll("hash-chip", getHashChipStyle(hashStatus));

        row.getChildren().addAll(checkBox, info, hashChip);

        // Click to toggle selection
        row.setOnMouseClicked(e -> {
            List<String> selection = isArchiveRow
                    ? selectedArchiveCaseIds : selectedRestoreCaseIds;

            if (selection.contains(caseId)) {
                selection.remove(caseId);
                row.getStyleClass().remove("integrity-row-selected");
                row.getStyleClass().add("integrity-row");
                checkBox.getStyleClass().remove("row-select-box-checked");
                checkMark.setText("");
            } else {
                selection.add(caseId);
                row.getStyleClass().remove("integrity-row");
                row.getStyleClass().add("integrity-row-selected");
                checkBox.getStyleClass().add("row-select-box-checked");
                checkMark.setText("✓");
            }

            if (isArchiveRow) updateArchiveActionBar();
            else              updateRestoreActionBar();
        });

        container.getChildren().add(row);
    }

    private void refreshRowSelectionUI(VBox list, List<String> selection) {
        list.getChildren().forEach(node -> {
            if (node instanceof HBox row) {
                String id = (String) row.getUserData();
                if (id == null) return;

                boolean selected = selection.contains(id);
                row.getStyleClass().removeAll(
                        "integrity-row", "integrity-row-selected"
                );
                row.getStyleClass().add(
                        selected ? "integrity-row-selected" : "integrity-row"
                );

                // Reset checkbox
                row.getChildren().stream()
                        .filter(c -> c instanceof StackPane sp &&
                                sp.getStyleClass().contains("row-select-box"))
                        .forEach(c -> {
                            StackPane sp = (StackPane) c;
                            sp.getStyleClass().remove("row-select-box-checked");
                            if (sp.getChildren().get(0) instanceof Label lbl)
                                lbl.setText("");
                        });
            }
        });
    }

    private String getHashChipStyle(String status) {
        return switch (status) {
            case "VERIFIED" -> "hash-verified";
            case "PENDING"  -> "hash-pending";
            case "FAILED"   -> "hash-failed";
            default         -> "hash-pending";
        };
    }

    // ══════════════════════════════════════════════════════════
    //  ARCHIVE — Confirm & Execute
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleArchiveCases() {
        if (selectedArchiveCaseIds.isEmpty()) return;
        int count = selectedArchiveCaseIds.size();
        archiveModalSummary.setText(
                count + " case" + (count > 1 ? "s" : "") + " will be moved to long-term storage."
        );
        archiveModal.setVisible(true);
        archiveModal.setManaged(true);
    }

    @FXML
    private void handleConfirmArchive() {
        handleCloseArchiveModal();
        showProcessingOverlay("Archiving Cases",
                List.of(
                        "Verifying integrity hashes...",
                        "Moving files to long-term storage...",
                        "Updating case records...",
                        "Writing audit log entry..."
                ),
                () -> {
                    // TODO: For each caseId in selectedArchiveCaseIds:
                    //   1. SELECT evidence files, verify SHA-256 hash matches stored hash
                    //   2. Move files to archive storage path
                    //   3. UPDATE cases SET status = 'ARCHIVED',
                    //                       archived_at = NOW()
                    //      WHERE case_id = ?
                    //   4. INSERT INTO audit_log (action, type, case_id, user_id, timestamp)
                    //      VALUES ('ARCHIVE', 'Case', ?, currentUserId, NOW())
                    selectedArchiveCaseIds.clear();
                    updateArchiveActionBar();
                    loadStats();
                }
        );
    }

    @FXML private void handleCloseArchiveModal() {
        archiveModal.setVisible(false);
        archiveModal.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  RESTORE — Confirm & Execute
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleRestoreCases() {
        if (selectedRestoreCaseIds.isEmpty()) return;
        int count = selectedRestoreCaseIds.size();
        restoreModalSummary.setText(
                count + " case" + (count > 1 ? "s" : "") + " will be restored to active storage."
        );
        restoreModal.setVisible(true);
        restoreModal.setManaged(true);
    }

    @FXML
    private void handleConfirmRestore() {
        handleCloseRestoreModal();
        showProcessingOverlay("Restoring Cases",
                List.of(
                        "Retrieving files from archive...",
                        "Comparing integrity hashes...",
                        "Restoring to active storage...",
                        "Writing audit log entry..."
                ),
                () -> {
                    // TODO: For each caseId in selectedRestoreCaseIds:
                    //   1. Retrieve files from archive storage
                    //   2. Compare current SHA-256 hash with original_hash in DB
                    //   3. If hash mismatch: flag case, notify admin, abort restore
                    //   4. UPDATE cases SET status = 'ACTIVE',
                    //                       restored_at = NOW()
                    //      WHERE case_id = ?
                    //   5. INSERT INTO audit_log (action, type, case_id, user_id, timestamp)
                    //      VALUES ('RESTORE', 'Case', ?, currentUserId, NOW())
                    selectedRestoreCaseIds.clear();
                    updateRestoreActionBar();
                    loadStats();
                }
        );
    }

    @FXML private void handleCloseRestoreModal() {
        restoreModal.setVisible(false);
        restoreModal.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  LOCKDOWN
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleLockdownPrompt() {
        lockdownCodeField.clear();
        lockdownError.setVisible(false);
        lockdownError.setManaged(false);
        lockdownModal.setVisible(true);
        lockdownModal.setManaged(true);
    }

    @FXML
    private void handleConfirmLockdown() {
        String code = lockdownCodeField.getText().trim();

        if (code.isEmpty()) {
            showLockdownError("Security code cannot be empty.");
            return;
        }

        // TODO: Verify code against stored admin security code in DB
        //   SELECT security_code FROM admin_settings WHERE admin_id = currentUserId
        //   If code doesn't match → showLockdownError("Invalid security code.")
        //   If matches → proceed

        boolean codeValid = true; // placeholder — replace with DB check

        if (!codeValid) {
            showLockdownError("Invalid security code.");
            return;
        }

        handleCloseLockdownModal();
        executeLockdown();
    }

    private void showLockdownError(String msg) {
        lockdownError.setText("⚠  " + msg);
        lockdownError.setVisible(true);
        lockdownError.setManaged(true);
    }

    @FXML private void handleCloseLockdownModal() {
        lockdownModal.setVisible(false);
        lockdownModal.setManaged(false);
        lockdownCodeField.clear();
    }

    private void executeLockdown() {
        showProcessingOverlay("Activating Lockdown",
                List.of(
                        "Terminating all active sessions...",
                        "Halting write operations...",
                        "Alerting security team...",
                        "Lockdown active."
                ),
                () -> {
                    // TODO: 1. UPDATE user_sessions SET active = false
                    //          WHERE user_id != currentUserId
                    // TODO: 2. UPDATE system_settings SET lockdown = true,
                    //                                     lockdown_by = currentUserId,
                    //                                     lockdown_at = NOW()
                    // TODO: 3. Send alert email/notification to security team
                    // TODO: 4. INSERT INTO audit_log (action='LOCKDOWN', ...)
                    // TODO: 5. Update status chip to red "System Locked"
                    updateStatusChip(true);
                }
        );
    }

    private void updateStatusChip(boolean locked) {
        lblSystemStatus.setText(locked ? "System Locked" : "System Online");
        lblSystemStatus.getStyleClass().removeAll(
                "system-status-label", "system-status-label-locked"
        );
        lblSystemStatus.getStyleClass().add(
                locked ? "system-status-label-locked" : "system-status-label"
        );
        statusDot.getStyleClass().removeAll(
                "status-dot-online", "status-dot-locked"
        );
        statusDot.getStyleClass().add(
                locked ? "status-dot-locked" : "status-dot-online"
        );
        systemStatusChip.getStyleClass().removeAll("system-status-chip-locked");
        if (locked) systemStatusChip.getStyleClass().add("system-status-chip-locked");
    }

    // ══════════════════════════════════════════════════════════
    //  PROCESSING OVERLAY
    // ══════════════════════════════════════════════════════════

    /**
     * Shows the animated processing overlay, cycles through step labels,
     * then runs the onComplete callback when done.
     */
    private void showProcessingOverlay(String title,
                                       List<String> steps,
                                       Runnable onComplete) {
        lblProcessingTitle.setText(title);
        lblProcessingStep.setText(steps.get(0));
        lblProcessingPct.setText("0%");
        processingProgressFill.setPrefWidth(0);

        processingOverlay.setVisible(true);
        processingOverlay.setManaged(true);

        double totalMs   = 2800.0;
        double trackWidth = 240.0;
        int    stepCount  = steps.size();

        Timeline anim = new Timeline();

        for (int i = 0; i < stepCount; i++) {
            final String stepLabel = steps.get(i);
            double t = (totalMs / stepCount) * (i + 1);
            anim.getKeyFrames().add(
                    new KeyFrame(Duration.millis(t),
                            e -> lblProcessingStep.setText(stepLabel),
                            new KeyValue(processingProgressFill.prefWidthProperty(),
                                    trackWidth * ((double)(steps.indexOf(stepLabel) + 1) / stepCount))
                    )
            );
        }

        anim.currentTimeProperty().addListener((o, ov, nv) -> {
            int pct = (int) Math.min(100, (nv.toMillis() / totalMs) * 100);
            lblProcessingPct.setText(pct + "%");
        });

        anim.setOnFinished(e -> {
            lblProcessingPct.setText("100%");
            processingProgressFill.setPrefWidth(trackWidth);

            PauseTransition pause = new PauseTransition(Duration.millis(600));
            pause.setOnFinished(ev -> {
                processingOverlay.setVisible(false);
                processingOverlay.setManaged(false);
                processingProgressFill.setPrefWidth(0);
                if (onComplete != null) onComplete.run();
            });
            pause.play();
        });

        anim.play();
    }
}
