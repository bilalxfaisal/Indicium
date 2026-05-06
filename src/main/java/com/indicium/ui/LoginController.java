package com.indicium.ui;

import com.indicium.models.SystemUser;
import com.indicium.repository.UserDirectory;
import com.indicium.services.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import static java.lang.IO.print;

public class LoginController
{

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
        print("Attempting login for: " + email);
        print("Hashed password: " + hashedInput);

        // Authenticate against DB
        SystemUser loggedInUser = UserDirectory.authenticate(email, hashedInput);

        if (loggedInUser != null)
        {
            // Store in session — sets currentUser + currentUserAuth
            SessionManager.getInstance().loginUser(loggedInUser);

            // Navigate to dashboard
            loadDashboard(event);
        }
        else {
            errorLabel.setText("Invalid email, password, or inactive account.");
        }
    }

    private void loadDashboard(ActionEvent event) {
        try {
            DashBoardController dashboard = new DashBoardController();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(dashboard));
            stage.setTitle("Indicium - Investigator Dashboard");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            System.err.println("[Login] Failed to load dashboard: " + e.getMessage());
            errorLabel.setText("Failed to load dashboard. Contact support.");
        }
    }
}
