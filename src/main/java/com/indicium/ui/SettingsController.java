package com.indicium.ui;

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
    private int    currentUserId = -1;
    private String discardTarget = ""; // "PROFILE" or "PASSWORD"

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
        setupStrengthBarVisibility();
        setupModalDefaults();
        setupToastDefaults();
    }

    // ══════════════════════════════════════════════════════════
    //  Scroll
    // ══════════════════════════════════════════════════════════
    private void setupScroll() {
        mainScrollPane.setFocusTraversable(false);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        //  Attach to the ScrollPane itself, not .getContent()
        mainScrollPane.setOnScroll(event -> {
            double deltaY   = event.getDeltaY() * 0.003;
            double newValue = mainScrollPane.getVvalue() - deltaY;
            mainScrollPane.setVvalue(
                    Math.max(0.0, Math.min(1.0, newValue))
            );
            event.consume();
        });
    }


    // ══════════════════════════════════════════════════════════
    //  Strength bar — hidden until user types in new password
    // ══════════════════════════════════════════════════════════

    private void setupStrengthBarVisibility() {
        // Start hidden
        strengthBarBox.setVisible(false);
        strengthBarBox.setManaged(false);

        // Show as soon as user starts typing
        fieldNewPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = !newVal.isEmpty();
            strengthBarBox.setVisible(hasText);
            strengthBarBox.setManaged(hasText);

            // Reset to neutral state when cleared
            if (!hasText) {
                applyStrengthStyle("weak");
            }
            // TODO: plug in real scoring logic here later
        });
    }

    // ══════════════════════════════════════════════════════════
    //  Strength bar styling helper
    // ══════════════════════════════════════════════════════════

    private void applyStrengthStyle(String level) {
        // level = "weak" | "medium" | "strong"

        strengthFill.getStyleClass().removeAll(
                "strength-fill-weak",
                "strength-fill-medium",
                "strength-fill-strong"
        );
        lblStrength.getStyleClass().removeAll(
                "strength-label-weak",
                "strength-label-medium",
                "strength-label-strong"
        );

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
    //  Modal defaults
    // ══════════════════════════════════════════════════════════

    private void setupModalDefaults() {
        discardModal.setVisible(false);
        discardModal.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  Toast defaults
    // ══════════════════════════════════════════════════════════

    private void setupToastDefaults() {
        toastBox.setVisible(false);
        toastBox.setManaged(false);
        toastBox.setOpacity(0);
    }

    // ══════════════════════════════════════════════════════════
    //  Toast animation (styling only — message wired later)
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
    //  Field error styling helpers (no logic — just style)
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

    // ══════════════════════════════════════════════════════════
    //  Modal open / close styling
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleCloseDiscardModal() {
        discardModal.setVisible(false);
        discardModal.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  Stubs — logic added later
    // ══════════════════════════════════════════════════════════

    public void loadUser(int userId) {
        this.currentUserId = userId;
        // TODO: load user from DB
    }

    @FXML
    private void handleSaveProfile() {
        // TODO: validate + save profile
    }

    @FXML
    private void handleDiscardProfile() {
        discardTarget = "PROFILE";
        discardModal.setVisible(true);
        discardModal.setManaged(true);
    }

    @FXML
    private void handleChangePassword() {
        // TODO: validate + update password
    }

    @FXML
    private void handleDiscardPassword() {
        discardTarget = "PASSWORD";
        discardModal.setVisible(true);
        discardModal.setManaged(true);
    }

    @FXML
    private void handleConfirmDiscard() {
        handleCloseDiscardModal();
        // TODO: reload profile or clear password fields based on discardTarget
    }
}
