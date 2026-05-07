package com.indicium.ui;

import com.indicium.models.SystemUser;
import com.indicium.repository.UserDirectory;
import com.indicium.services.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
        System.out.println("Attempting login for: " + email);
        System.out.println("Hashed password: " + hashedInput);

        SystemUser loggedInUser = UserDirectory.authenticate(email, hashedInput);

        if (loggedInUser != null) {
            SessionManager.getInstance().loginUser(loggedInUser);

            if (SessionManager.getInstance().isAdminLoggedIn()) {
                loadAdminDashboard(event);
            } else if (SessionManager.getInstance().isInvestigatorLoggedIn()) {
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

    //  Admin now routes to the same dashboard — no separate FXML needed
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
