package com.indicium.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;

public class IdentityManagerDashboardController extends BorderPane {

    // ── Top bar ──
    @FXML private TextField  searchField;
    @FXML private ComboBox<String> roleFilter;

    // ── Stats ──
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statInvestigators;
    @FXML private Label statAdmins;

    // ── Table ──
    @FXML private TableView<UserRow>       userTable;
    @FXML private TableColumn<UserRow, Integer> colId;
    @FXML private TableColumn<UserRow, String>  colName;
    @FXML private TableColumn<UserRow, String>  colEmail;
    @FXML private TableColumn<UserRow, String>  colRole;
    @FXML private TableColumn<UserRow, String>  colStatus;
    @FXML private TableColumn<UserRow, String>  colLast;

    // ── Detail panel ──
    @FXML private Label    detailTitle;
    @FXML private Label    avatarLabel;
    @FXML private Label    detailName;
    @FXML private Label    detailRole;
    @FXML private TextField fieldName;
    @FXML private TextField fieldEmail;
    @FXML private ComboBox<String> fieldRole;
    @FXML private ComboBox<String> fieldClearance;
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
        setupRoleFilter();
        setupTable();
        loadDummyData();       // TODO: replace with DB call
        setupSearch();
        updateStats();
        clearDetailPanel();
    }

    // ── Role filter ComboBox ──
    private void setupRoleFilter() {
        roleFilter.setItems(FXCollections.observableArrayList(
                "All Roles", "Investigator", "Admin", "Analyst", "Read-Only"
        ));
        roleFilter.setValue("All Roles");

        fieldRole.setItems(FXCollections.observableArrayList(
                "Investigator", "Admin", "Analyst", "Read-Only"
        ));
        fieldClearance.setItems(FXCollections.observableArrayList(
                "Level 1", "Level 2", "Level 3", "Top Secret"
        ));
        fieldStatus.setItems(FXCollections.observableArrayList(
                "Active", "Suspended", "Revoked"
        ));
    }

    // ── Table columns ──
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colLast.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));

        // Status column with colored badge
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("id-status-badge");
                    switch (status) {
                        case "Active"    -> badge.setStyle("-fx-background-color:#1A3A2A; -fx-text-fill:#4CAF50; -fx-background-radius:10; -fx-padding:2 10 2 10; -fx-font-size:11px; -fx-font-weight:bold;");
                        case "Suspended" -> badge.setStyle("-fx-background-color:#3A2A1A; -fx-text-fill:#FF9800; -fx-background-radius:10; -fx-padding:2 10 2 10; -fx-font-size:11px; -fx-font-weight:bold;");
                        case "Revoked"   -> badge.setStyle("-fx-background-color:#3A1A1A; -fx-text-fill:#FF5252; -fx-background-radius:10; -fx-padding:2 10 2 10; -fx-font-size:11px; -fx-font-weight:bold;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    // ── Dummy seed data ──
    private void loadDummyData() {
        masterList = FXCollections.observableArrayList(
                new UserRow(1, "Alice Rahman",   "alice@indicium.pk",  "Admin",        "Active",    "2026-05-04"),
                new UserRow(2, "Bilal Chaudhry", "bilal@indicium.pk",  "Investigator", "Active",    "2026-05-03"),
                new UserRow(3, "Sara Khan",      "sara@indicium.pk",   "Analyst",      "Active",    "2026-05-01"),
                new UserRow(4, "Omar Farooq",    "omar@indicium.pk",   "Investigator", "Suspended", "2026-04-28"),
                new UserRow(5, "Nadia Hussain",  "nadia@indicium.pk",  "Read-Only",    "Active",    "2026-04-20"),
                new UserRow(6, "Zain Malik",     "zain@indicium.pk",   "Investigator", "Revoked",   "2026-03-15")
        );
        filteredList = new FilteredList<>(masterList, p -> true);
        userTable.setItems(filteredList);
    }

    // ── Live search ──
    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
    }

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

    // ── Stats ──
    private void updateStats() {
        long total   = masterList.size();
        long active  = masterList.stream().filter(u -> u.getStatus().equals("Active")).count();
        long inv     = masterList.stream().filter(u -> u.getRole().equals("Investigator")).count();
        long admins  = masterList.stream().filter(u -> u.getRole().equals("Admin")).count();

        statTotal.setText(String.valueOf(total));
        statActive.setText(String.valueOf(active));
        statInvestigators.setText(String.valueOf(inv));
        statAdmins.setText(String.valueOf(admins));
    }

    // ══════════════════════════════════════════
    //  HANDLERS
    // ══════════════════════════════════════════

    @FXML
    private void handleRoleFilter() {
        applyFilters();
    }

    @FXML
    private void handleTableClick() {
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null) return;
        selectedUser = row;
        populateDetailPanel(row);
    }

    @FXML
    private void handleNewUser() {
        // Clear panel and enter edit mode for a blank user
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
        // TODO: validate + write to DB
        String name   = fieldName.getText().trim();
        String email  = fieldEmail.getText().trim();
        String role   = fieldRole.getValue();
        String status = fieldStatus.getValue();

        if (name.isEmpty() || email.isEmpty() || role == null) {
            showAlert("Validation Error", "Name, email and role are required.");
            return;
        }

        if (selectedUser == null) {
            // New user
            int newId = masterList.size() + 1;
            UserRow newUser = new UserRow(newId, name, email, role, status != null ? status : "Active", "—");
            masterList.add(newUser);
            selectedUser = newUser;
        } else {
            // Update existing
            selectedUser.setName(name);
            selectedUser.setEmail(email);
            selectedUser.setRole(role);
            selectedUser.setStatus(status);
            userTable.refresh();
        }

        updateStats();
        populateDetailPanel(selectedUser);
        exitEditMode();
    }

    @FXML
    private void handleRevoke() {
        if (selectedUser == null) return;
        selectedUser.setStatus("Revoked");
        userTable.refresh();
        updateStats();
        populateDetailPanel(selectedUser);
        // TODO: write to DB + audit log
    }

    @FXML
    private void handleReset() {
        if (selectedUser == null) return;
        // TODO: trigger password reset flow
        showAlert("Password Reset", "A reset link has been sent to " + selectedUser.getEmail());
    }

    // ══════════════════════════════════════════
    //  DETAIL PANEL HELPERS
    // ══════════════════════════════════════════

    private void populateDetailPanel(UserRow user) {
        detailTitle.setText("User Details");
        avatarLabel.setText(String.valueOf(user.getName().charAt(0)).toUpperCase());
        detailName.setText(user.getName());
        detailRole.setText(user.getRole());

        fieldName.setText(user.getName());
        fieldEmail.setText(user.getEmail());
        fieldRole.setValue(user.getRole());
        fieldStatus.setValue(user.getStatus());

        btnEdit.setVisible(true);
        btnRevoke.setVisible(!user.getStatus().equals("Revoked"));
        btnReset.setVisible(true);

        // Role badge colour
        String colour = switch (user.getRole()) {
            case "Admin"        -> "#FF9800";
            case "Investigator" -> "#00BCD4";
            case "Analyst"      -> "#9C27B0";
            default             -> "#607D8B";
        };
        detailRole.setStyle("-fx-background-color:" + colour + "33; -fx-text-fill:" + colour +
                "; -fx-background-radius:10; -fx-padding:2 10 2 10; -fx-font-size:11px; -fx-font-weight:bold;");
    }

    private void clearDetailPanel() {
        detailTitle.setText("User Details");
        avatarLabel.setText("?");
        detailName.setText("Select a user");
        detailRole.setText("");
        detailRole.setStyle("");
        fieldName.clear();
        fieldEmail.clear();
        fieldRole.setValue(null);
        fieldClearance.setValue(null);
        fieldStatus.setValue(null);
        btnEdit.setVisible(false);
        btnRevoke.setVisible(false);
        btnReset.setVisible(false);
        btnSave.setVisible(false);
        btnSave.setManaged(false);
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
    }

    private void enterEditMode() {
        editMode = true;
        fieldName.setEditable(true);
        fieldEmail.setEditable(true);
        fieldRole.setDisable(false);
        fieldClearance.setDisable(false);
        fieldStatus.setDisable(false);

        btnEdit.setVisible(false);
        btnCancel.setVisible(true);
        btnCancel.setManaged(true);
        btnSave.setVisible(true);
        btnSave.setManaged(true);
        btnRevoke.setVisible(false);
        btnReset.setVisible(false);
    }

    private void exitEditMode() {
        editMode = false;
        fieldName.setEditable(false);
        fieldEmail.setEditable(false);
        fieldRole.setDisable(true);
        fieldClearance.setDisable(true);
        fieldStatus.setDisable(true);

        btnEdit.setVisible(true);
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
        btnSave.setVisible(false);
        btnSave.setManaged(false);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // ══════════════════════════════════════════
    //  UserRow model (inner class)
    // ══════════════════════════════════════════

    public static class UserRow {
        private int    id;
        private String name, email, role, status, lastLogin;

        public UserRow(int id, String name, String email, String role, String status, String lastLogin) {
            this.id = id; this.name = name; this.email = email;
            this.role = role; this.status = status; this.lastLogin = lastLogin;
        }

        public int    getId()        { return id; }
        public String getName()      { return name; }
        public String getEmail()     { return email; }
        public String getRole()      { return role; }
        public String getStatus()    { return status; }
        public String getLastLogin() { return lastLogin; }

        public void setName(String v)   { name = v; }
        public void setEmail(String v)  { email = v; }
        public void setRole(String v)   { role = v; }
        public void setStatus(String v) { status = v; }
    }
}
