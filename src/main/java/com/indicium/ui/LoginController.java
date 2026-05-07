package com.indicium.ui;

import com.indicium.models.SystemUser;
import com.indicium.repository.UserDirectory;
import com.indicium.services.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

        // Hash using the shared utility — no duplicate logic
        String hashedInput = SessionManager.hashSHA256(rawPassword);
        System.out.println("Attempting login for: " + email);
        System.out.println("Hashed password: " + hashedInput);

        // Authenticate against DB
        SystemUser loggedInUser = UserDirectory.authenticate(email, hashedInput);

        if (loggedInUser != null) {
            // Store in session — sets currentUser + currentUserAuth
            SessionManager.getInstance().loginUser(loggedInUser);

            // Role-based routing — redirect to the correct dashboard
            if (SessionManager.getInstance().isAdminLoggedIn()) {
                loadAdminDashboard(event);
            } else if (SessionManager.getInstance().isInvestigatorLoggedIn()) {
                loadInvestigatorDashboard(event);
            } else {
                // Defensive fallback: should never happen with valid enum values
                errorLabel.setText("Login succeeded but role is unrecognised. Contact your admin.");
                SessionManager.getInstance().logoutUser();
            }
        } else {
            errorLabel.setText("Invalid email, password, or inactive account.");
        }
    }

    /**
     * Loads the Investigator Dashboard using the InvestigationDashBoard launcher.
     */
    private void loadInvestigatorDashboard(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            InvestigationDashBoard.launchDashboard(stage);
        } catch (Exception e) {
            System.err.println("[Login] Failed to load investigator dashboard: " + e.getMessage());
            errorLabel.setText("Failed to load dashboard. Contact support.");
        }
    }

    /**
     * Loads the Admin Dashboard from AdminDashboard.fxml.
     * If AdminDashboard.fxml doesn't exist yet, it falls back gracefully.
     */
    private void loadAdminDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/indicium/ui/AdminDashboard.fxml")
            );
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Indicium - Admin Dashboard");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            System.err.println("[Login] Failed to load admin dashboard: " + e.getMessage());
            errorLabel.setText("Admin dashboard not yet available. Contact support.");
        }
    }
}

