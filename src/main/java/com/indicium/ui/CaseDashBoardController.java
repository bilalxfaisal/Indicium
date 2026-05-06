package com.indicium.ui;

import com.indicium.controllers.CaseManager;
import com.indicium.controllers.FilterCriteria;
import com.indicium.controllers.FilterType;
import com.indicium.models.Case;
import com.indicium.models.CaseStatus;
import com.indicium.models.UserAuth;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.EvidenceRepo;
import com.indicium.services.SessionManager;


import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    // ── Backend ──
    private CaseManager    caseManager;
    private CaseRepository caseRepo;
    private int            currentUserID;

    // ── In-memory cache of loaded cases (for filtering without re-querying) ──
    private List<Case> allCases;

    // ══════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════

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

    // ══════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════

    @FXML
    public void initialize() {
        // 1. Pull logged-in user from session
        currentUserID = SessionManager.getInstance()
                .getCurrentUser()
                .getUserID();

        // 2. Wire up CaseManager
        UserAuth userAuth = new UserAuth(
                SessionManager.getInstance().getCurrentUser().getCredentials(),
                null
        );
        caseManager = new CaseManager(userAuth, new EvidenceRepo());
        caseRepo    = new CaseRepository();

        // 3. Setup UI options
        setupFilterOptions();

        // 4. Load cases from DB
        loadCases();
    }

    // ══════════════════════════════════════════
    //  Setup
    // ══════════════════════════════════════════

    private void setupFilterOptions() {
        filterPriority.getItems().addAll("All", "High", "Medium", "Low");
        filterStatus.getItems().addAll("All", "Active", "Archived", "Locked");
        filterPriority.setValue("All");
        filterStatus.setValue("All");
        inputPriority.getItems().addAll("High", "Medium", "Low");
    }

    // ══════════════════════════════════════════
    //  Load Cases from DB
    // ══════════════════════════════════════════

    private void loadCases() {
        try {
            // Pull all cases from DB
            allCases = caseRepo.findAll();

            // Render them
            renderCases(allCases);
            updateCountBadges(allCases);

        } catch (Exception e) {
            System.err.println("[CaseDashBoard] Failed to load cases: " + e.getMessage());
            showEmptyState(true);
        }
    }

    private void renderCases(List<Case> cases) {
        // Clear existing rows (keep the header row at index 0 if you have one)
        caseListContainer.getChildren().clear();

        if (cases == null || cases.isEmpty()) {
            showEmptyState(true);
            return;
        }

        showEmptyState(false);

        for (Case c : cases) {
            // Map CaseStatus to display string
            String statusDisplay = mapStatusToDisplay(c.getStatus());

            // Priority — your Case model doesn't have priority yet,
            // default to "Medium" until you add it to the DB schema
            String priority = "Medium";

            // Format date
            String date = c.getIncidentDate() != null
                    ? c.getIncidentDate().toLocalDate().toString()
                    : "N/A";

            buildCaseRow(
                    String.valueOf(c.getCaseID()),
                    c.getTitle(),
                    priority,
                    date,
                    statusDisplay
            );
        }
    }

    private void updateCountBadges(List<Case> cases) {
        long active   = cases.stream()
                .filter(c -> c.getStatus() == CaseStatus.OPEN)
                .count();
        long archived = cases.stream()
                .filter(c -> c.getStatus() == CaseStatus.ARCHIVED)
                .count();
        long locked   = cases.stream()
                .filter(c -> c.getStatus() == CaseStatus.CLOSED)
                .count();

        countActive.setText(String.valueOf(active));
        countArchived.setText(String.valueOf(archived));
        countLocked.setText(String.valueOf(locked));
    }

    private String mapStatusToDisplay(CaseStatus status) {
        return switch (status) {
            case OPEN     -> "Active";
            case ARCHIVED -> "Archived";
            case CLOSED   -> "Locked";
            default       -> "Active";
        };
    }

    private CaseStatus mapDisplayToStatus(String display) {
        return switch (display) {
            case "Active"   -> CaseStatus.OPEN;
            case "Archived" -> CaseStatus.ARCHIVED;
            case "Locked"   -> CaseStatus.CLOSED;
            default         -> CaseStatus.OPEN;
        };
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    // ══════════════════════════════════════════
    //  Build Case Row
    // ══════════════════════════════════════════

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

    // ══════════════════════════════════════════
    //  Overflow Menu
    // ══════════════════════════════════════════

    private MenuItem makeMenuItem(String label, String iconFile) {
        ImageView icon = new ImageView(
                new Image(getClass().getResourceAsStream("/com/indicium/ui/Assets/" + iconFile))
        );
        icon.setFitWidth(14);
        icon.setFitHeight(14);
        icon.setPreserveRatio(true);

        MenuItem item = new MenuItem(label, icon);
        return item;
    }


    private void showOverflowMenu(Button anchor, String caseId, String status) {
        ContextMenu menu = new ContextMenu();

        MenuItem open    = makeMenuItem("Open Case",     "icons8-open-100.png");
        MenuItem edit    = makeMenuItem("Edit Details",  "icons8-edit-100.png");
        MenuItem lock    = makeMenuItem("Lock Case",     "icons8-lock-100.png");
        MenuItem archive = makeMenuItem("Archive Case",  "icons8-archive-100.png");
        MenuItem delete  = makeMenuItem("Delete",        "icons8-delete-100.png");

        open.setOnAction(e    -> handleOpenCase(caseId));
        edit.setOnAction(e    -> handleEditCase(caseId));
        lock.setOnAction(e    -> handleLockCase(caseId));
        archive.setOnAction(e -> handleArchiveCase(caseId));
        delete.setOnAction(e  -> handleDeleteCase(caseId));

        String role = SessionManager.getInstance()
                .getCurrentUser()
                .getRole()
                .toString();
        delete.setDisable(!role.equalsIgnoreCase("ADMIN"));

        menu.getItems().addAll(open, edit, new SeparatorMenuItem(),
                lock, archive, new SeparatorMenuItem(), delete);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }


    // ══════════════════════════════════════════
    //  Filter Handlers
    // ══════════════════════════════════════════

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterPriority.setValue("All");
        filterStatus.setValue("All");
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        renderCases(allCases);
        updateCountBadges(allCases);
    }

    private void applyFilters() {
        if (allCases == null) return;

        String    search   = searchField.getText().toLowerCase().trim();
        String    priority = filterPriority.getValue();
        String    status   = filterStatus.getValue();
        LocalDate from     = filterDateFrom.getValue();
        LocalDate to       = filterDateTo.getValue();

        List<Case> filtered = allCases.stream()
                // Search by title or ID
                .filter(c -> search.isEmpty()
                        || c.getTitle().toLowerCase().contains(search)
                        || String.valueOf(c.getCaseID()).contains(search))

                // Status filter
                .filter(c -> status.equals("All")
                        || c.getStatus() == mapDisplayToStatus(status))

                // Date range filter
                .filter(c -> {
                    if (c.getIncidentDate() == null) return true;
                    LocalDate caseDate = c.getIncidentDate().toLocalDate();
                    if (from != null && caseDate.isBefore(from)) return false;
                    if (to   != null && caseDate.isAfter(to))   return false;
                    return true;
                })
                .collect(Collectors.toList());

        renderCases(filtered);
        updateCountBadges(filtered);
    }

    // ══════════════════════════════════════════
    //  Modal Handlers
    // ══════════════════════════════════════════

    @FXML
    private void handleNewCase() {
        // Check permission before even opening the modal
        if (!caseManager.startNewInvestigation(currentUserID)) {
            showAlert("Permission Denied",
                    "You don't have permission to create cases.");
            return;
        }
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

        String    title    = inputTitle.getText().trim();
        LocalDate date     = inputDate.getValue();
        String    priority = inputPriority.getValue();

        // 1. Check for duplicate title
        boolean isDuplicate = allCases != null && allCases.stream()
                .anyMatch(c -> c.getTitle().equalsIgnoreCase(title));

        if (isDuplicate) {
            errorDuplicate.setText("A case with this title already exists.");
            errorDuplicate.setVisible(true);
            errorDuplicate.setManaged(true);
            return;
        }

        // 2. Create via CaseManager (handles DB save + audit log)
        LocalDateTime dateTime = date.atStartOfDay();
        int newCaseID = caseManager.initializeCase(title, dateTime, currentUserID);

        System.out.println("[CaseDashBoard] New case created with ID: " + newCaseID);

        // 3. Refresh the list
        loadCases();

        // 4. Close modal
        handleCloseModal();
    }

    // ══════════════════════════════════════════
    //  Case Row Actions
    // ══════════════════════════════════════════

    private void handleOpenCase(String caseId) {
        // TODO: Navigate to case detail view, passing caseId
        System.out.println("[CaseDashBoard] Opening case: " + caseId);
    }

    private void handleEditCase(String caseId) {
        // TODO: Populate modal with existing case data for editing
        System.out.println("[CaseDashBoard] Editing case: " + caseId);
    }

    private void handleLockCase(String caseId) {
        int id = Integer.parseInt(caseId);
        Case c = caseRepo.findById(id);
        if (c == null) return;

        c.setStatus(CaseStatus.CLOSED);
        caseRepo.update(c, "status = 'CLOSED'");

        System.out.println("[CaseDashBoard] Case locked: " + caseId);
        loadCases(); // refresh
    }

    private void handleArchiveCase(String caseId) {
        int id = Integer.parseInt(caseId);

        // Uses CaseManager which also verifies integrity before archiving
        int archived = caseManager.removeFromActiveSpace(
                currentUserID, new int[]{id}
        );

        if (archived > 0) {
            System.out.println("[CaseDashBoard] Case archived: " + caseId);
            loadCases();
        } else {
            showAlert("Archive Failed",
                    "Case could not be archived. Integrity check may have failed.");
        }
    }

    private void handleDeleteCase(String caseId) {
        // Confirm dialog before delete
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Case");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Add CaseRepository.delete(int caseID) method
                System.out.println("[CaseDashBoard] Delete case: " + caseId);
                loadCases();
            }
        });
    }

    // ══════════════════════════════════════════
    //  Validation
    // ══════════════════════════════════════════

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

    // ══════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
