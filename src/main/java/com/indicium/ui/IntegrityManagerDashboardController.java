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
import java.util.Optional;

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
        // Restore status chip and button state from persisted lockdown state on every load
        boolean locked = com.indicium.services.AccessManager.isLockdownActive();
        updateStatusChip(locked);
        updateLockdownButton(locked);
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

    private void buildIntegrityRow(VBox container, String caseId, String title,
                                   String date, int evCount,
                                   String hashStatus, boolean isArchiveRow) {
        HBox row = new HBox(12);
        row.getStyleClass().add("integrity-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setUserData(caseId);

        StackPane checkBox = new StackPane();
        checkBox.getStyleClass().add("row-select-box");
        Label checkMark = new Label("");
        checkMark.getStyleClass().add("row-check-label");
        checkBox.getChildren().add(checkMark);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblId    = new Label(caseId);
        lblId.getStyleClass().add("integrity-case-id");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("integrity-case-title");
        Label lblMeta  = new Label(evCount + " evidence files  ·  " + date);
        lblMeta.getStyleClass().add("integrity-case-meta");
        info.getChildren().addAll(lblId, lblTitle, lblMeta);

        Label hashChip = new Label(hashStatus);
        hashChip.getStyleClass().addAll("hash-chip", getHashChipStyle(hashStatus));

        row.getChildren().addAll(checkBox, info, hashChip);

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
                row.getStyleClass().removeAll("integrity-row", "integrity-row-selected");
                row.getStyleClass().add(selected ? "integrity-row-selected" : "integrity-row");

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

    // Hardcoded emergency security code. In production, store this as a hashed value in DB.
    private static final String EMERGENCY_CODE = "LOCKDOWN-911";

    /**
     * Toggles the system lockdown state.
     * Currently locked   → confirms with admin and lifts the lockdown.
     * Currently unlocked → asks for emergency code and activates the lockdown.
     */
    @FXML
    private void onLockdownTriggered() {
        // ── RBAC CHECK ───────────────────────────────────────────────────────
        if (!com.indicium.services.SessionManager.getInstance().isAdminLoggedIn()) {
            System.err.println("[SECURITY] Unauthorized lockdown operation blocked.");
            com.indicium.services.AuditLog breach = new com.indicium.services.AuditLog();
            breach.logEvent(-1, "UNAUTHORIZED LOCKDOWN OPERATION BLOCKED", com.indicium.services.AuditCategory.SECURITY);
            new Alert(Alert.AlertType.ERROR,
                    "Admin privileges required for this operation.",
                    ButtonType.OK).showAndWait();
            return;
        }

        boolean currentlyLocked = com.indicium.services.AccessManager.isLockdownActive();
        int adminId = com.indicium.services.SessionManager.getInstance().getCurrentUser().getUserID();

        if (currentlyLocked) {
            // ── LIFT LOCKDOWN ───────────────────────────────────────────────
            Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Are you sure you want to lift the system lockdown?\nAll users will regain access.",
                ButtonType.YES, ButtonType.NO
            );
            confirm.setTitle("Lift System Lockdown");
            confirm.setHeaderText("Confirm Lockdown Removal");
            Optional<ButtonType> choice = confirm.showAndWait();
            if (choice.isEmpty() || choice.get() != ButtonType.YES) return;

            com.indicium.services.AuditLog auditLog = new com.indicium.services.AuditLog();
            com.indicium.services.AccessManager.liftLockdown(adminId, auditLog);
            updateStatusChip(false);
            updateLockdownButton(false);

            Alert lifted = new Alert(Alert.AlertType.INFORMATION);
            lifted.setTitle("Lockdown Lifted");
            lifted.setHeaderText("\uD83D\uDFE2  System Lockdown Lifted");
            lifted.setContentText("The system is now back online. All users can log in again.");
            lifted.showAndWait();

        } else {
            // ── ACTIVATE LOCKDOWN ───────────────────────────────────────────
            TextInputDialog codeDialog = new TextInputDialog();
            codeDialog.setTitle("Emergency Lockdown");
            codeDialog.setHeaderText("\u26A0  This action is irreversible until manually lifted.");
            codeDialog.setContentText("Enter emergency security code:");

            Optional<String> input = codeDialog.showAndWait();
            if (input.isEmpty()) return;

            if (!EMERGENCY_CODE.equals(input.get().trim())) {
                com.indicium.services.AuditLog fail = new com.indicium.services.AuditLog();
                fail.logEvent(adminId, "LOCKDOWN REJECTED — Wrong security code entered", com.indicium.services.AuditCategory.SECURITY);

                Alert wrong = new Alert(Alert.AlertType.ERROR);
                wrong.setTitle("Lockdown Failed");
                wrong.setHeaderText("Incorrect Security Code");
                wrong.setContentText("The emergency code you entered is incorrect. This event has been logged.");
                wrong.showAndWait();
                return;
            }

            com.indicium.services.AuditLog auditLog = new com.indicium.services.AuditLog();
            com.indicium.services.AccessManager.activateLockdown(adminId, auditLog);
            updateStatusChip(true);
            updateLockdownButton(true);

            Alert done = new Alert(Alert.AlertType.INFORMATION);
            done.setTitle("Emergency Lockdown Active");
            done.setHeaderText("\uD83D\uDD12  System Lockdown Activated");
            done.setContentText(
                "All active non-admin sessions have been terminated.\n" +
                "The system is now in lockdown mode.\n\n" +
                "Only administrators can log in until you lift the lockdown."
            );
            done.showAndWait();
        }
    }

    /**
     * Updates the Kill Switch button label and CSS style to reflect current lockdown state.
     * Locked   → green "Lift Lockdown" button (recovery action)
     * Unlocked → red   "Kill Switch"   button (danger action)
     */
    private void updateLockdownButton(boolean locked) {
        if (locked) {
            btnLockdown.setText("Lift Lockdown");
            btnLockdown.getStyleClass().remove("btn-lockdown");
            if (!btnLockdown.getStyleClass().contains("btn-lift-lockdown"))
                btnLockdown.getStyleClass().add("btn-lift-lockdown");
        } else {
            btnLockdown.setText("Kill Switch");
            btnLockdown.getStyleClass().remove("btn-lift-lockdown");
            if (!btnLockdown.getStyleClass().contains("btn-lockdown"))
                btnLockdown.getStyleClass().add("btn-lockdown");
        }
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

    private void showProcessingOverlay(String title, List<String> steps, Runnable onComplete) {
        lblProcessingTitle.setText(title);
        lblProcessingStep.setText(steps.get(0));
        lblProcessingPct.setText("0%");
        processingProgressFill.setPrefWidth(0);

        processingOverlay.setVisible(true);
        processingOverlay.setManaged(true);

        double totalMs    = 2800.0;
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
