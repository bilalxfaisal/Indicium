package com.indicium.ui;

import com.indicium.models.Admin;
import com.indicium.models.SystemUser;
import com.indicium.models.UserRole;
import com.indicium.repository.UserDirectory;
import com.indicium.services.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class IdentityManagerDashboardController extends BorderPane {

    // ── Top bar ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilter;

    // ── Stats ──
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statInvestigators;
    @FXML private Label statAdmins;

    // ── Table ──
    @FXML private TableView<UserRow>            userTable;
    @FXML private TableColumn<UserRow, Integer> colId;
    @FXML private TableColumn<UserRow, String>  colName;
    @FXML private TableColumn<UserRow, String>  colEmail;
    @FXML private TableColumn<UserRow, String>  colRole;
    @FXML private TableColumn<UserRow, String>  colStatus;
    @FXML private TableColumn<UserRow, String>  colLast;

    // ── Detail panel ──
    @FXML private Label            detailTitle;
    @FXML private Label            avatarLabel;
    @FXML private Label            detailName;
    @FXML private Label            detailRole;
    @FXML private TextField        fieldName;
    @FXML private TextField        fieldEmail;
    @FXML private ComboBox<String> fieldRole;
    @FXML private ComboBox<String> fieldStatus;

    // ── Buttons ──
    @FXML private Button btnEdit;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    @FXML private Button btnRevoke;
    @FXML private Button btnReset;

    // ── Data ──
    private ObservableList<UserRow> masterList;
    private FilteredList<UserRow>   filteredList;
    private UserRow                 selectedUser;
    private boolean                 editMode = false;

    // ── Admin actor (from session) ──
    private Admin currentAdmin;

    // ══════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════

    public IdentityManagerDashboardController() {
        URL url = getClass().getResource("/com/indicium/ui/IdentityManager.fxml");
        if (url == null)
            throw new RuntimeException("IdentityManager.fxml not found!");

        FXMLLoader loader = new FXMLLoader(url);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load IdentityManager.fxml: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════

    @FXML
    public void initialize() {
        loadAdminSession();
        setupCombos();
        setupTable();
        setupSearch();
        refreshFromDB();
        clearDetailPanel();
    }

    // ══════════════════════════════════════════
    //  Session
    // ══════════════════════════════════════════

    private void loadAdminSession() {
        SystemUser user = SessionManager.getInstance().getCurrentUser();
        if (user != null && user.getRole() == UserRole.ADMIN) {
            currentAdmin = new Admin(
                    user.getUserID(),
                    user.getName(),
                    user.getEmail(),
                    user.getCredentials()
            );
        }
    }

    public void refresh() {
        loadAdminSession();
        refreshFromDB();
    }

    // ══════════════════════════════════════════
    //  Setup
    // ══════════════════════════════════════════

    private void setupCombos() {
        roleFilter.setItems(FXCollections.observableArrayList(
                "All Roles", "ADMIN", "INVESTIGATOR"
        ));
        roleFilter.setValue("All Roles");

        fieldRole.setItems(FXCollections.observableArrayList(
                "ADMIN", "INVESTIGATOR"
        ));

        fieldStatus.setItems(FXCollections.observableArrayList(
                "Active", "Revoked"
        ));
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colLast.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(status);
                String style = switch (status) {
                    case "Active"  -> "-fx-background-color:#1A3A2A; -fx-text-fill:#4CAF50;";
                    case "Revoked" -> "-fx-background-color:#3A1A1A; -fx-text-fill:#FF5252;";
                    default        -> "-fx-background-color:#2A2A2A; -fx-text-fill:#B0BEC5;";
                };
                badge.setStyle(style +
                        "-fx-background-radius:10; -fx-padding:2 10 2 10;" +
                        "-fx-font-size:11px; -fx-font-weight:bold;");
                setGraphic(badge);
                setText(null);
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
    }

    // ══════════════════════════════════════════
    //  DB Load
    // ══════════════════════════════════════════

    private void refreshFromDB() {
        List<SystemUser> dbUsers = UserDirectory.getAllActiveUsers();

        masterList = FXCollections.observableArrayList();
        for (SystemUser u : dbUsers) {
            masterList.add(new UserRow(
                    u.getUserID(),
                    u.getName(),
                    u.getEmail(),
                    u.getRole().name(),
                    u.isActive() ? "Active" : "Revoked",
                    "—"
            ));
        }

        filteredList = new FilteredList<>(masterList, p -> true);
        userTable.setItems(filteredList);
        updateStats();
    }

    // ══════════════════════════════════════════
    //  Filters & Stats
    // ══════════════════════════════════════════

    private void applyFilters() {
        String query = searchField.getText().toLowerCase().trim();
        String role  = roleFilter.getValue();

        filteredList.setPredicate(user -> {
            boolean matchesSearch = query.isEmpty()
                    || user.getName().toLowerCase().contains(query)
                    || user.getEmail().toLowerCase().contains(query);
            boolean matchesRole = role == null || role.equals("All Roles")
                    || user.getRole().equals(role);
            return matchesSearch && matchesRole;
        });
        updateStats();
    }

    private void updateStats() {
        long total  = masterList.size();
        long active = masterList.stream().filter(u -> u.getStatus().equals("Active")).count();
        long inv    = masterList.stream().filter(u -> u.getRole().equals("INVESTIGATOR")).count();
        long admins = masterList.stream().filter(u -> u.getRole().equals("ADMIN")).count();

        statTotal.setText(String.valueOf(total));
        statActive.setText(String.valueOf(active));
        statInvestigators.setText(String.valueOf(inv));
        statAdmins.setText(String.valueOf(admins));
    }

    // ══════════════════════════════════════════
    //  HANDLERS
    // ══════════════════════════════════════════

    @FXML
    private void handleRoleFilter() { applyFilters(); }

    @FXML
    private void handleTableClick() {
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null) return;
        selectedUser = row;
        populateDetailPanel(row);
    }

    @FXML
    private void handleNewUser() {
        selectedUser = null;
        clearDetailPanel();
        detailTitle.setText("New User");
        enterEditMode();
    }

    @FXML
    private void handleEdit() {
        enterEditMode();
    }

    @FXML
    private void handleCancel() {
        if (selectedUser != null) populateDetailPanel(selectedUser);
        else clearDetailPanel();
        exitEditMode();
    }

    @FXML
    private void handleSave() {
        if (currentAdmin == null) {
            showAlert("Access Denied", "You must be logged in as an Admin to perform this action.");
            return;
        }

        String name  = fieldName.getText().trim();
        String email = fieldEmail.getText().trim();
        String role  = fieldRole.getValue();

        // ── Validate fields ──
        if (name.isEmpty()) {
            showAlert("Validation Error", "Name is required.");
            return;
        }
        if (email.isEmpty() || !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showAlert("Validation Error", "A valid email address is required.");
            return;
        }
        if (role == null) {
            showAlert("Validation Error", "A role must be selected.");
            return;
        }

        UserRole userRole = UserRole.valueOf(role);

        if (selectedUser == null) {
            // ══ NEW USER ══

            if (!SystemUser.validateUniqueEmail(email)) {
                showAlert("Duplicate Email", "This email is already registered in the system.");
                return;
            }
            if (!SystemUser.checkValidRole(userRole)) {
                showAlert("Invalid Role", "Selected role is not valid.");
                return;
            }

            // Admin sets the password explicitly via dialog
            String tempPassword = showPasswordDialog(name, "Set Password");
            if (tempPassword == null) return; // cancelled

            String  hashedPassword = SessionManager.hashSHA256(tempPassword);
            boolean saved          = currentAdmin.registerNewUser(name, email, hashedPassword, userRole);

            if (saved) {
                refreshFromDB();
                clearDetailPanel();
                exitEditMode();
                showSuccess(
                        "User '" + name + "' created successfully.\n\n" +
                                "Temporary password has been set.\n" +
                                "Instruct the user to change it on first login."
                );
            } else {
                showAlert("Save Failed", "Could not create user. Check console for details.");
            }

        } else {
            // ══ MODIFY EXISTING USER ══

            String status = fieldStatus.getValue();

            boolean roleUpdated    = currentAdmin.modifyUserRole(selectedUser.getId(), userRole);
            boolean profileUpdated = UserDirectory.updateProfile(selectedUser.getId(), name, email);

            // Revoke if status changed to Revoked
            if ("Revoked".equals(status) && "Active".equals(selectedUser.getStatus())) {
                currentAdmin.deactivateUser(selectedUser.getId());
            }

            if (roleUpdated || profileUpdated) {
                refreshFromDB();

                // Re-select the updated row
                userTable.getItems().stream()
                        .filter(u -> u.getId() == selectedUser.getId())
                        .findFirst()
                        .ifPresent(u -> {
                            selectedUser = u;
                            populateDetailPanel(u);
                        });

                exitEditMode();
                showSuccess("User '" + name + "' updated successfully.");
            } else {
                showAlert("Update Failed", "Could not update user. Check console for details.");
            }
        }
    }

    @FXML
    private void handleRevoke() {
        if (selectedUser == null || currentAdmin == null) return;

        // Store name before clearing
        String userName = selectedUser.getName();

        boolean revoked = currentAdmin.deactivateUser(selectedUser.getId());
        if (revoked) {
            refreshFromDB();
            clearDetailPanel();
            selectedUser = null;
            showSuccess("User '" + userName + "' has been revoked.");
        } else {
            showAlert("Revoke Failed", "Could not revoke user. Check console for details.");
        }
    }

    @FXML
    private void handleReset() {
        if (selectedUser == null || currentAdmin == null) return;

        // Admin sets the new password via dialog
        String tempPassword = showPasswordDialog(selectedUser.getName(), "Reset Password");
        if (tempPassword == null) return; // cancelled

        String  hashedPassword = SessionManager.hashSHA256(tempPassword);
        boolean reset          = UserDirectory.updatePassword(selectedUser.getId(), hashedPassword);

        if (reset) {
            showSuccess(
                    "Password for '" + selectedUser.getName() + "' has been reset.\n\n" +
                            "Instruct the user to change it on first login."
            );
        } else {
            showAlert("Reset Failed", "Could not reset password. Check console for details.");
        }
    }

    // ══════════════════════════════════════════
    //  Password Dialog
    // ══════════════════════════════════════════

    /**
     * Shows a dialog for the admin to set a temporary password.
     * confirmLabel = "Set Password" for new user, "Reset Password" for reset.
     * Returns plain-text password, or null if cancelled.
     */
    private String showPasswordDialog(String userName, String confirmLabel) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Set Temporary Password");
        dialog.setHeaderText("Set a temporary password for: " + userName);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        PasswordField passField    = new PasswordField();
        PasswordField confirmField = new PasswordField();
        passField.setPromptText("Temporary password");
        confirmField.setPromptText("Confirm password");

        Label strengthLabel = new Label("");
        strengthLabel.setStyle("-fx-font-size:11px;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill:#FF5252; -fx-font-size:11px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Live strength feedback
        passField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) { strengthLabel.setText(""); return; }

            int score = 0;
            if (val.length() >= 8)                      score++;
            if (val.length() >= 12)                      score++;
            if (val.matches(".*[A-Z].*"))               score++;
            if (val.matches(".*[0-9].*"))               score++;
            if (val.matches(".*[!@#$%^&*()_+\\-=].*")) score++;

            if (score >= 4) {
                strengthLabel.setText("Strength: Strong ✓");
                strengthLabel.setStyle("-fx-text-fill:#4CAF50; -fx-font-size:11px;");
            } else if (score >= 2) {
                strengthLabel.setText("Strength: Medium");
                strengthLabel.setStyle("-fx-text-fill:#FF9800; -fx-font-size:11px;");
            } else {
                strengthLabel.setText("Strength: Weak");
                strengthLabel.setStyle("-fx-text-fill:#FF5252; -fx-font-size:11px;");
            }
        });

        content.getChildren().addAll(
                new Label("Min 8 characters, must include a number."),
                passField,
                strengthLabel,
                confirmField,
                errorLabel
        );
        dialog.getDialogPane().setContent(content);

        ButtonType confirmBtn = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn  = new ButtonType("Cancel",     ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, cancelBtn);

        // Block close if validation fails
        Node confirmNode = dialog.getDialogPane().lookupButton(confirmBtn);
        confirmNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String pass    = passField.getText();
            String confirm = confirmField.getText();

            if (pass.length() < 8) {
                errorLabel.setText("Password must be at least 8 characters.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                event.consume();
                return;
            }
            if (!pass.matches(".*\\d.*")) {
                errorLabel.setText("Password must contain at least one number.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                event.consume();
                return;
            }
            if (!pass.equals(confirm)) {
                errorLabel.setText("Passwords do not match.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                event.consume();
                return;
            }
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });

        dialog.setResultConverter(btn ->
                btn == confirmBtn ? passField.getText() : null
        );

        return dialog.showAndWait().orElse(null);
    }

    // ══════════════════════════════════════════
    //  Detail Panel
    // ══════════════════════════════════════════

    private void populateDetailPanel(UserRow row) {
        detailTitle.setText("User Details");

        String[] parts   = row.getName().trim().split(" ", 2);
        String   initials = parts[0].isEmpty() ? "?"
                : String.valueOf(parts[0].charAt(0)).toUpperCase()
                + (parts.length > 1 && !parts[1].isEmpty()
                ? String.valueOf(parts[1].charAt(0)).toUpperCase() : "");
        avatarLabel.setText(initials);
        detailName.setText(row.getName());
        detailRole.setText(row.getRole());

        fieldName.setText(row.getName());
        fieldEmail.setText(row.getEmail());
        fieldRole.setValue(row.getRole());
        fieldStatus.setValue(row.getStatus());

        btnEdit.setVisible(true);
        btnEdit.setManaged(true);

        btnReset.setVisible(true);
        btnReset.setManaged(true);

        boolean isActive = "Active".equals(row.getStatus());
        btnRevoke.setVisible(isActive);
        btnRevoke.setManaged(isActive);
    }

    private void clearDetailPanel() {
        detailTitle.setText("Select a User");
        avatarLabel.setText("?");
        detailName.setText("—");
        detailRole.setText("—");
        fieldName.clear();
        fieldEmail.clear();
        fieldRole.setValue(null);
        fieldStatus.setValue(null);

        btnEdit.setVisible(false);
        btnEdit.setManaged(false);
        btnRevoke.setVisible(false);
        btnRevoke.setManaged(false);
        btnReset.setVisible(false);
        btnReset.setManaged(false);
    }

    private void enterEditMode() {
        editMode = true;
        fieldName.setEditable(true);
        fieldEmail.setEditable(true);
        fieldRole.setDisable(false);
        fieldStatus.setDisable(false);
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnCancel.setVisible(true);
        btnCancel.setManaged(true);
        btnEdit.setVisible(false);
        btnEdit.setManaged(false);
    }

    private void exitEditMode() {
        editMode = false;
        fieldName.setEditable(false);
        fieldEmail.setEditable(false);
        fieldRole.setDisable(true);
        fieldStatus.setDisable(true);
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
    }

    // ══════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Indicium — Identity Manager");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
