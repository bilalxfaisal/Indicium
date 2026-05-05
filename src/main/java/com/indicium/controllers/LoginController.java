package com.indicium.controllers;

import com.indicium.models.SystemUser;
import com.indicium.repository.UserDirectory;
import com.indicium.services.SessionManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginController {

    // Note: Changed from usernameField to emailField to match your repository
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void onLoginButtonClicked(ActionEvent event) {
        String email = emailField.getText();
        String rawPassword = passwordField.getText();

        if (email.isEmpty() || rawPassword.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        // 1. Hash the password to match your database schema
        String hashedInput = hashPassword(rawPassword);

        // 2. Call your static authenticate method directly
        SystemUser loggedInUser = UserDirectory.authenticate(email, hashedInput);

        // 3. Handle the Result
        if (loggedInUser != null) {
            // SUCCESS: Save to global session
            SessionManager.getInstance().loginUser(loggedInUser);

            // Switch to the main dashboard
            loadDashboard(event);
        } else {
            // FAILURE: Show error
            errorLabel.setText("Invalid email, password, or inactive account.");
        }
    }

    /**
     * Converts the plain text password into an SHA-256 hash.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("CRITICAL: Hashing algorithm not found.");
            return null;
        }
    }

    private void loadDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/indicium/ui/Dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Indicium - Investigator Dashboard");
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load dashboard: " + e.getMessage());
        }
    }
}