package com.indicium.ui;

import com.indicium.models.SystemUser;
import com.indicium.repository.UserDirectory;
import com.indicium.services.SessionManager;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class SettingsController extends StackPane {

    // ── Header ──
    @FXML private Label     lblUserFullName;
    @FXML private Label     lblUserRole;
    @FXML private Label     lblUserId;
    @FXML private Label     avatarInitials;

    // ── Profile fields ──
    @FXML private TextField fieldFirstName;
    @FXML private TextField fieldLastName;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldRole;
    @FXML private TextField fieldCreatedAt;

    // ── Profile errors ──
    @FXML private Label errFirstName;
    @FXML private Label errLastName;
    @FXML private Label errEmail;

    // ── Password fields ──
    @FXML private PasswordField fieldCurrentPassword;
    @FXML private PasswordField fieldNewPassword;
    @FXML private PasswordField fieldConfirmPassword;

    // ── Password errors ──
    @FXML private Label errCurrentPassword;
    @FXML private Label errNewPassword;
    @FXML private Label errConfirmPassword;

    // ── Strength bar ──
    @FXML private VBox      strengthBarBox;
    @FXML private Label     lblStrength;
    @FXML private StackPane strengthFill;

    // ── Toast ──
    @FXML private VBox  toastBox;
    @FXML private Label toastMessage;

    // ── Discard modal ──
    @FXML private StackPane discardModal;
    @FXML private Button    btnConfirmDiscard;

    // ── Scroll ──
    @FXML private ScrollPane mainScrollPane;

    // ── State ──
    private int        currentUserId = -1;
    private String     discardTarget = "";
    private SystemUser currentUser   = null;

    // Snapshots for discard — stored as split parts for the two text fields
    private String originalFirstName;
    private String originalLastName;
    private String originalEmail;

    // ══════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════

    public SettingsController() {
        URL fxmlUrl = getClass().getResource(
                "/com/indicium/ui/SettingsDashboard.fxml"
        );
        if (fxmlUrl == null)
            throw new RuntimeException("SettingsDashboard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load SettingsDashboard.fxml: " + e.getMessage(), e
            );
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        setupScroll();
        setupStrengthBar();
        setupModalDefaults();
        setupToastDefaults();
        loadCurrentUser();
    }

    // ══════════════════════════════════════════════════════════
    //  Load user from session
    // ══════════════════════════════════════════════════════════

    private void loadCurrentUser() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) return;
        currentUserId = currentUser.getUserID();
        populateFields(currentUser);
    }

    public void loadUser(int userId) {
        // Uses UserDirectory.findUser() — consistent with existing static pattern
        SystemUser user = UserDirectory.findUser(userId);
        if (user != null) {
            currentUser   = user;
            currentUserId = userId;
            populateFields(user);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Populate fields
    //  UserDirectory stores a single FullName — we split on the
    //  first space to fill the two separate text fields.
    // ══════════════════════════════════════════════════════════

    private void populateFields(SystemUser user) {
        String fullName = user.getName() != null ? user.getName().trim() : "";

        // Split FullName → first / last for the two editable fields
        int    spaceIdx = fullName.indexOf(' ');
        String first    = spaceIdx > 0 ? fullName.substring(0, spaceIdx) : fullName;
        String last     = spaceIdx > 0 ? fullName.substring(spaceIdx + 1) : "";

        // Avatar initials
        String initials = (first.isEmpty() ? "?" : String.valueOf(first.charAt(0)).toUpperCase())
                + (last.isEmpty()  ? ""  : String.valueOf(last.charAt(0)).toUpperCase());
        avatarInitials.setText(initials);

        // Header labels
        lblUserFullName.setText(fullName.isEmpty() ? "—" : fullName);
        lblUserRole.setText(user.getRole() != null ? user.getRole().name() : "—");
        lblUserId.setText("ID #" + user.getUserID());

        // Editable fields
        fieldFirstName.setText(first);
        fieldLastName.setText(last);
        fieldEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // Read-only fields
        fieldRole.setText(user.getRole() != null ? user.getRole().name() : "—");
        // SystemUser has no createdAt — show a placeholder
        fieldCreatedAt.setText("—");

        // Save snapshots for discard
        originalFirstName = first;
        originalLastName  = last;
        originalEmail     = fieldEmail.getText();
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 1 — Save Profile
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleSaveProfile()
    {

        clearProfileErrors();

        String first = fieldFirstName.getText().trim();
        String last  = fieldLastName.getText().trim();
        String email = fieldEmail.getText().trim();

        // ── TEMP DEBUG — remove after confirming ──
        System.out.println("[Settings] Saving: '" + first + " " + last + "' / " + email
                + " for UserID=" + currentUserId);

        boolean valid = true;

        clearProfileErrors();


        if (first.isEmpty()) {
            showFieldError(errFirstName, fieldFirstName, "First name is required.");
            valid = false;
        }
        if (last.isEmpty()) {
            showFieldError(errLastName, fieldLastName, "Last name is required.");
            valid = false;
        }
        if (email.isEmpty()) {
            showFieldError(errEmail, fieldEmail, "Email is required.");
            valid = false;
        } else if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showFieldError(errEmail, fieldEmail, "Enter a valid email address.");
            valid = false;
        }

        if (!valid) return;

        // Rejoin into FullName for DB — consistent with UserDirectory schema
        String fullName = (first + " " + last).trim();
        boolean saved   = UserDirectory.updateProfile(currentUserId, fullName, email);

        if (saved) {
            // Keep in-memory session user in sync
            currentUser.SetName(fullName);
            currentUser.SetEmail(email);

            populateFields(currentUser);
            showToast("✓  Profile updated successfully.");
        } else {
            showFieldError(errEmail, fieldEmail, "Save failed — email may already be in use.");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 1 — Discard Profile
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleDiscardProfile() {
        boolean changed = !fieldFirstName.getText().trim().equals(originalFirstName)
                || !fieldLastName.getText().trim().equals(originalLastName)
                || !fieldEmail.getText().trim().equals(originalEmail);

        if (changed) {
            discardTarget = "PROFILE";
            discardModal.setVisible(true);
            discardModal.setManaged(true);
        } else {
            clearProfileErrors();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 2 — Change Password
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleChangePassword() {
        clearPasswordErrors();

        String current = fieldCurrentPassword.getText();
        String newPass = fieldNewPassword.getText();
        String confirm = fieldConfirmPassword.getText();

        boolean valid = true;

        // ── Validate current password ──
        if (current.isEmpty()) {
            showFieldError(errCurrentPassword, fieldCurrentPassword,
                    "Enter your current password.");
            valid = false;
        } else {
            String hashedCurrent = SessionManager.hashSHA256(current);
            // Uses UserDirectory.verifyPassword() — checks against PasswordHash column
            if (!UserDirectory.verifyPassword(currentUserId, hashedCurrent)) {
                showFieldError(errCurrentPassword, fieldCurrentPassword,
                        "Current password is incorrect.");
                valid = false;
            }
        }

        // ── Validate new password ──
        if (newPass.isEmpty()) {
            showFieldError(errNewPassword, fieldNewPassword, "Enter a new password.");
            valid = false;
        } else if (newPass.length() < 8) {
            showFieldError(errNewPassword, fieldNewPassword,
                    "Password must be at least 8 characters.");
            valid = false;
        } else if (!newPass.matches(".*\\d.*")) {
            showFieldError(errNewPassword, fieldNewPassword,
                    "Password must contain at least one number.");
            valid = false;
        }

        // ── Validate confirm ──
        if (confirm.isEmpty()) {
            showFieldError(errConfirmPassword, fieldConfirmPassword,
                    "Please confirm your new password.");
            valid = false;
        } else if (!newPass.equals(confirm)) {
            showFieldError(errConfirmPassword, fieldConfirmPassword,
                    "Passwords do not match.");
            valid = false;
        }

        if (!valid) return;

        // Uses UserDirectory.updatePassword() — writes to PasswordHash column
        String  hashedNew = SessionManager.hashSHA256(newPass);
        boolean saved     = UserDirectory.updatePassword(currentUserId, hashedNew);

        if (saved) {
            fieldCurrentPassword.clear();
            fieldNewPassword.clear();
            fieldConfirmPassword.clear();
            strengthBarBox.setVisible(false);
            strengthBarBox.setManaged(false);
            showToast("✓  Password updated successfully.");
        } else {
            showFieldError(errNewPassword, fieldNewPassword,
                    "Password update failed. Try again.");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 2 — Discard Password
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleDiscardPassword() {
        boolean changed = !fieldCurrentPassword.getText().isEmpty()
                || !fieldNewPassword.getText().isEmpty()
                || !fieldConfirmPassword.getText().isEmpty();

        if (changed) {
            discardTarget = "PASSWORD";
            discardModal.setVisible(true);
            discardModal.setManaged(true);
        } else {
            clearPasswordErrors();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Discard modal — confirm
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleConfirmDiscard() {
        handleCloseDiscardModal();

        if ("PROFILE".equals(discardTarget)) {
            fieldFirstName.setText(originalFirstName);
            fieldLastName.setText(originalLastName);
            fieldEmail.setText(originalEmail);
            clearProfileErrors();

        } else if ("PASSWORD".equals(discardTarget)) {
            fieldCurrentPassword.clear();
            fieldNewPassword.clear();
            fieldConfirmPassword.clear();
            strengthBarBox.setVisible(false);
            strengthBarBox.setManaged(false);
            clearPasswordErrors();
        }

        discardTarget = "";
    }

    @FXML
    private void handleCloseDiscardModal() {
        discardModal.setVisible(false);
        discardModal.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  Scroll
    // ══════════════════════════════════════════════════════════

    private void setupScroll() {
        mainScrollPane.setFocusTraversable(false);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mainScrollPane.setOnScroll(event -> {
            double deltaY   = event.getDeltaY() * 0.003;
            double newValue = mainScrollPane.getVvalue() - deltaY;
            mainScrollPane.setVvalue(Math.max(0.0, Math.min(1.0, newValue)));
            event.consume();
        });
    }

    // ══════════════════════════════════════════════════════════
    //  Strength bar
    // ══════════════════════════════════════════════════════════

    private void setupStrengthBar() {
        strengthBarBox.setVisible(false);
        strengthBarBox.setManaged(false);

        fieldNewPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = !newVal.isEmpty();
            strengthBarBox.setVisible(hasText);
            strengthBarBox.setManaged(hasText);
            applyStrengthStyle(hasText ? scorePassword(newVal) : "weak");
        });
    }

    private String scorePassword(String pw) {
        int score = 0;
        if (pw.length() >= 8)                      score++;
        if (pw.length() >= 12)                      score++;
        if (pw.matches(".*[A-Z].*"))               score++;
        if (pw.matches(".*[0-9].*"))               score++;
        if (pw.matches(".*[!@#$%^&*()_+\\-=].*")) score++;

        if (score >= 4) return "strong";
        if (score >= 2) return "medium";
        return "weak";
    }

    private void applyStrengthStyle(String level) {
        strengthFill.getStyleClass().removeAll(
                "strength-fill-weak", "strength-fill-medium", "strength-fill-strong");
        lblStrength.getStyleClass().removeAll(
                "strength-label-weak", "strength-label-medium", "strength-label-strong");

        switch (level) {
            case "medium" -> {
                strengthFill.getStyleClass().add("strength-fill-medium");
                lblStrength.getStyleClass().add("strength-label-medium");
                lblStrength.setText("Medium");
            }
            case "strong" -> {
                strengthFill.getStyleClass().add("strength-fill-strong");
                lblStrength.getStyleClass().add("strength-label-strong");
                lblStrength.setText("Strong");
            }
            default -> {
                strengthFill.getStyleClass().add("strength-fill-weak");
                lblStrength.getStyleClass().add("strength-label-weak");
                lblStrength.setText("Weak");
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Modal + Toast defaults
    // ══════════════════════════════════════════════════════════

    private void setupModalDefaults() {
        discardModal.setVisible(false);
        discardModal.setManaged(false);
    }

    private void setupToastDefaults() {
        toastBox.setVisible(false);
        toastBox.setManaged(false);
        toastBox.setOpacity(0);
    }

    // ══════════════════════════════════════════════════════════
    //  Toast
    // ══════════════════════════════════════════════════════════

    private void showToast(String message) {
        toastMessage.setText(message);
        toastBox.setVisible(true);
        toastBox.setManaged(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(2500));
        hold.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toastBox);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                toastBox.setVisible(false);
                toastBox.setManaged(false);
            });
            fadeOut.play();
        });

        fadeIn.setOnFinished(e -> hold.play());
        fadeIn.play();
    }

    // ══════════════════════════════════════════════════════════
    //  Field error helpers
    // ══════════════════════════════════════════════════════════

    private void showFieldError(Label errLabel, Control field, String msg) {
        errLabel.setText(msg);
        errLabel.setVisible(true);
        errLabel.setManaged(true);
        if (!field.getStyleClass().contains("field-error-border"))
            field.getStyleClass().add("field-error-border");
    }

    private void hideError(Label errLabel, Control field) {
        errLabel.setVisible(false);
        errLabel.setManaged(false);
        field.getStyleClass().remove("field-error-border");
    }

    private void clearProfileErrors() {
        hideError(errFirstName, fieldFirstName);
        hideError(errLastName,  fieldLastName);
        hideError(errEmail,     fieldEmail);
    }

    private void clearPasswordErrors() {
        hideError(errCurrentPassword, fieldCurrentPassword);
        hideError(errNewPassword,     fieldNewPassword);
        hideError(errConfirmPassword, fieldConfirmPassword);
    }
}
