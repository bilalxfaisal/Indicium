package com.indicium.ui;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;

public class CaseDashBoardController extends StackPane {

    // ── Header ──
    @FXML private Label countActive;
    @FXML private Label countArchived;
    @FXML private Label countLocked;

    // ── Filters ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterPriority;
    @FXML private ComboBox<String> filterStatus;
    @FXML private DatePicker       filterDateFrom;
    @FXML private DatePicker       filterDateTo;

    // ── Case list ──
    @FXML private VBox caseListContainer;
    @FXML private VBox emptyState;

    // ── Modal ──
    @FXML private StackPane        modalOverlay;
    @FXML private TextField        inputTitle;
    @FXML private DatePicker       inputDate;
    @FXML private ComboBox<String> inputPriority;
    @FXML private TextArea         inputDescription;
    @FXML private Label            errorTitle;
    @FXML private Label            errorDate;
    @FXML private Label            errorDuplicate;

    public CaseDashBoardController() {
        URL fxmlUrl = getClass().getResource("/com/indicium/ui/CaseDashBoard.fxml");
        if (fxmlUrl == null)
            throw new RuntimeException("CaseDashBoard.fxml not found!");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load CaseDashBoard.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void initialize() {
        setupFilterOptions();
        loadCases();
    }

    // ── Setup ──────────────────────────────────────────────────────

    private void setupFilterOptions() {
        filterPriority.getItems().addAll("All", "High", "Medium", "Low");
        filterStatus.getItems().addAll("All", "Active", "Archived", "Locked");
        filterPriority.setValue("All");
        filterStatus.setValue("All");
        inputPriority.getItems().addAll("High", "Medium", "Low");
    }

    // ── Load Cases ─────────────────────────────────────────────────

    private void loadCases() {
        // TODO: Query DB for all cases assigned to the logged-in user
        // TODO: Call buildCaseRow() for each result
        // TODO: Update countActive, countArchived, countLocked labels
        showEmptyState(true);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    private void buildCaseRow(String caseId, String title,
                              String priority, String date, String status) {
        HBox row = new HBox();
        row.getStyleClass().add("case-row");
        row.setSpacing(0);

        Label lblId = new Label(caseId);
        lblId.getStyleClass().add("case-id");
        lblId.setPrefWidth(110);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("case-title");
        lblTitle.setPrefWidth(220);

        Label lblPriority = new Label(priority);
        lblPriority.getStyleClass().addAll("priority-chip", getPriorityStyle(priority));
        lblPriority.setPrefWidth(90);

        Label lblDate = new Label(date);
        lblDate.getStyleClass().add("case-date");
        lblDate.setPrefWidth(110);

        Label lblStatus = new Label(status);
        lblStatus.getStyleClass().addAll("status-chip", getStatusStyle(status));
        lblStatus.setPrefWidth(100);

        Button btnOpen = new Button("Open");
        btnOpen.getStyleClass().add("btn-open");
        btnOpen.setOnAction(e -> handleOpenCase(caseId));

        Button btnMore = new Button("⋮");
        btnMore.getStyleClass().add("btn-overflow");
        btnMore.setOnAction(e -> showOverflowMenu(btnMore, caseId, status));

        HBox actions = new HBox(8, btnOpen, btnMore);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.ALWAYS);

        row.getChildren().addAll(lblId, lblTitle, lblPriority, lblDate, lblStatus, actions);
        caseListContainer.getChildren().add(row);
    }

    private String getPriorityStyle(String priority) {
        return switch (priority) {
            case "High"   -> "priority-high";
            case "Medium" -> "priority-medium";
            default       -> "priority-low";
        };
    }

    private String getStatusStyle(String status) {
        return switch (status) {
            case "Active"   -> "status-active";
            case "Archived" -> "status-archived";
            case "Locked"   -> "status-locked";
            default         -> "status-active";
        };
    }

    // ── Overflow Menu ──────────────────────────────────────────────

    private void showOverflowMenu(Button anchor, String caseId, String status) {
        ContextMenu menu = new ContextMenu();

        MenuItem open    = new MenuItem("📂  Open Case");
        MenuItem edit    = new MenuItem("✏️  Edit Details");
        MenuItem lock    = new MenuItem("🔒  Lock Case");
        MenuItem archive = new MenuItem("🗄️  Archive Case");
        MenuItem delete  = new MenuItem("🗑️  Delete");

        open.setOnAction(e    -> handleOpenCase(caseId));
        edit.setOnAction(e    -> handleEditCase(caseId));
        lock.setOnAction(e    -> handleLockCase(caseId));
        archive.setOnAction(e -> handleArchiveCase(caseId));
        delete.setOnAction(e  -> handleDeleteCase(caseId));

        delete.setDisable(true); // TODO: enable for admin role only via RBAC

        menu.getItems().addAll(open, edit, new SeparatorMenuItem(),
                lock, archive, new SeparatorMenuItem(), delete);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // ── Filter Handlers ────────────────────────────────────────────

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterPriority.setValue("All");
        filterStatus.setValue("All");
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        applyFilters();
    }

    private void applyFilters() {
        String    search   = searchField.getText().toLowerCase().trim();
        String    priority = filterPriority.getValue();
        String    status   = filterStatus.getValue();
        LocalDate from     = filterDateFrom.getValue();
        LocalDate to       = filterDateTo.getValue();
        // TODO: Filter caseListContainer rows against all criteria
        // TODO: Show emptyState if no rows pass the filter
    }

    // ── Modal Handlers ─────────────────────────────────────────────

    @FXML
    private void handleNewCase() {
        clearModalFields();
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    @FXML
    private void handleCloseModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    @FXML
    private void handleSubmitCase() {
        if (!validateCaseForm()) return;

        String    title       = inputTitle.getText().trim();
        LocalDate date        = inputDate.getValue();
        String    priority    = inputPriority.getValue();
        String    description = inputDescription.getText().trim();

        // TODO: Check for duplicate case title in DB
        // TODO: Generate unique Case ID (e.g. "IND-XXXX")
        // TODO: INSERT new case into DB
        // TODO: Log creation in audit log
        // TODO: Refresh caseListContainer + update count badges
        // TODO: Show success toast

        handleCloseModal();
    }

    private boolean validateCaseForm() {
        boolean valid = true;

        if (inputTitle.getText().trim().isEmpty()) {
            errorTitle.setText("Case title is required.");
            errorTitle.setVisible(true);
            errorTitle.setManaged(true);
            valid = false;
        } else {
            errorTitle.setVisible(false);
            errorTitle.setManaged(false);
        }

        if (inputDate.getValue() == null) {
            errorDate.setText("Incident date is required.");
            errorDate.setVisible(true);
            errorDate.setManaged(true);
            valid = false;
        } else {
            errorDate.setVisible(false);
            errorDate.setManaged(false);
        }

        return valid;
    }

    private void clearModalFields() {
        inputTitle.clear();
        inputDate.setValue(null);
        inputPriority.setValue(null);
        inputDescription.clear();
        errorTitle.setVisible(false);     errorTitle.setManaged(false);
        errorDate.setVisible(false);      errorDate.setManaged(false);
        errorDuplicate.setVisible(false); errorDuplicate.setManaged(false);
    }

    // ── Case Row Actions ───────────────────────────────────────────

    private void handleOpenCase(String caseId)    { /* TODO: Load case detail view */ }
    private void handleEditCase(String caseId)    { /* TODO: Populate and open edit form */ }
    private void handleLockCase(String caseId)    { /* TODO: DB update status = Locked */ }
    private void handleArchiveCase(String caseId) { /* TODO: DB update status = Archived */ }
    private void handleDeleteCase(String caseId)  { /* TODO: RBAC check + confirm + DB delete */ }


}
