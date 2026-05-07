package com.indicium.ui;

import com.indicium.models.SystemUser;
import com.indicium.repository.UserDirectory;
import com.indicium.services.AccessManager;
import com.indicium.services.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    public void onLoginButtonClicked(ActionEvent event) {
        String email       = emailField.getText().trim();
        String rawPassword = passwordField.getText();

        if (email.isEmpty() || rawPassword.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        String hashedInput = SessionManager.hashSHA256(rawPassword);
        SystemUser loggedInUser = UserDirectory.authenticate(email, hashedInput);

        if (loggedInUser != null) {
            SessionManager.getInstance().loginUser(loggedInUser);

            boolean isAdmin        = SessionManager.getInstance().isAdminLoggedIn();
            boolean isInvestigator = SessionManager.getInstance().isInvestigatorLoggedIn();

            // ── LOCKDOWN GATE ──────────────────────────────────────────────
            // Only admins pass through during an active lockdown.
            // Non-admins get a popup and their session is immediately cleared.
            if (AccessManager.isLockdownActive() && !isAdmin) {
                SessionManager.getInstance().logoutUser();
                Alert locked = new Alert(Alert.AlertType.WARNING, "", ButtonType.OK);
                locked.setTitle("System Lockdown");
                locked.setHeaderText("\uD83D\uDD12  SYSTEM LOCKDOWN ACTIVE");
                locked.setContentText(
                    "The system is currently under an emergency lockdown.\n" +
                    "All non-admin access has been suspended.\n\n" +
                    "Please contact your system administrator."
                );
                locked.showAndWait();
                return;
            }
            // ──────────────────────────────────────────────────────────────

            if (isAdmin) {
                loadAdminDashboard(event);
            } else if (isInvestigator) {
                loadInvestigatorDashboard(event);
            } else {
                errorLabel.setText("Login succeeded but role is unrecognised. Contact your admin.");
                SessionManager.getInstance().logoutUser();
            }

        } else {
            errorLabel.setText("Invalid email, password, or inactive account.");
        }
    }

    private void loadInvestigatorDashboard(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            InvestigationDashBoard.launchDashboard(stage);
        } catch (Exception e) {
            System.err.println("[Login] Failed to load investigator dashboard: " + e.getMessage());
            errorLabel.setText("Failed to load dashboard. Contact support.");
        }
    }

    private void loadAdminDashboard(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            InvestigationDashBoard.launchDashboard(stage);
        } catch (Exception e) {
            System.err.println("[Login] Failed to load admin dashboard: " + e.getMessage());
            errorLabel.setText("Failed to load dashboard. Contact support.");
        }
    }
}
