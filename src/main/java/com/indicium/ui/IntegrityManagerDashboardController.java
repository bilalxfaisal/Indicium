package com.indicium.ui;

import com.indicium.services.HashGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class IntegrityManagerDashboardController extends StackPane {

    // ── Header ──
    @FXML private Label     lblSystemStatus;
    @FXML private HBox      systemStatusChip;
    @FXML private StackPane statusDot;

    // ── Lockdown ──
    @FXML private Button btnLockdown;

    private boolean verifyAdminPassword(int adminId, String enteredPassword) {
        String hashed = HashGenerator.generateSHA256FromString(enteredPassword);
        return com.indicium.repository.UserDirectory.verifyPassword(adminId, hashed);
    }

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
        boolean locked = com.indicium.services.AccessManager.isLockdownActive();
        updateStatusChip(locked);
        updateLockdownButton(locked);
    }

    // ══════════════════════════════════════════════════════════
    //  LOCKDOWN
    // ══════════════════════════════════════════════════════════

    @FXML
    private void onLockdownTriggered() {

        // ── RBAC CHECK ──────────────────────────────────────────────────────
        if (!com.indicium.services.SessionManager.getInstance().isAdminLoggedIn()) {
            System.err.println("[SECURITY] Unauthorized lockdown operation blocked.");
            com.indicium.services.AuditLog breach = new com.indicium.services.AuditLog();
            breach.logEvent(-1, "UNAUTHORIZED LOCKDOWN OPERATION BLOCKED",
                    com.indicium.services.AuditCategory.SECURITY);
            new Alert(Alert.AlertType.ERROR,
                    "Admin privileges required for this operation.",
                    ButtonType.OK).showAndWait();
            return;
        }

        boolean currentlyLocked = com.indicium.services.AccessManager.isLockdownActive();
        int adminId = com.indicium.services.SessionManager.getInstance()
                .getCurrentUser().getUserID();

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

            // Build custom dialog with PasswordField
            Dialog<String> codeDialog = new Dialog<>();
            codeDialog.setTitle("Emergency Lockdown");
            codeDialog.setHeaderText("\u26A0  This action is irreversible until manually lifted.");

            PasswordField pf = new PasswordField();
            pf.setPromptText("Enter your admin password...");
            pf.setPrefWidth(300);

            VBox content = new VBox(6,
                    new Label("Enter emergency security code:"),
                    pf
            );
            content.setPadding(new Insets(10, 0, 0, 0));
            codeDialog.getDialogPane().setContent(content);
            codeDialog.getDialogPane().getButtonTypes()
                    .addAll(ButtonType.OK, ButtonType.CANCEL);

            // Auto-focus the password field when dialog opens
            codeDialog.setOnShown(e -> pf.requestFocus());

            // Return typed text only on OK
            codeDialog.setResultConverter(btn ->
                    btn == ButtonType.OK ? pf.getText() : null
            );

            Optional<String> input = codeDialog.showAndWait();
            if (input.isEmpty() || input.get().isBlank()) return;

            if (!verifyAdminPassword(adminId, input.get())) {
                com.indicium.services.AuditLog fail = new com.indicium.services.AuditLog();
                fail.logEvent(adminId, "LOCKDOWN REJECTED — Wrong security code entered",
                        com.indicium.services.AuditCategory.SECURITY);

                Alert wrong = new Alert(Alert.AlertType.ERROR);
                wrong.setTitle("Lockdown Failed");
                wrong.setHeaderText("Incorrect Security Code");
                wrong.setContentText(
                        "The emergency code you entered is incorrect. This event has been logged."
                );
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

    // ══════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════

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
        statusDot.getStyleClass().removeAll("status-dot-online", "status-dot-locked");
        statusDot.getStyleClass().add(locked ? "status-dot-locked" : "status-dot-online");
        systemStatusChip.getStyleClass().removeAll("system-status-chip-locked");
        if (locked) systemStatusChip.getStyleClass().add("system-status-chip-locked");
    }
}
