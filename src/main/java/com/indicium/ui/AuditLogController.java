package com.indicium.ui;

import com.indicium.models.AuditLogEntry;
import com.indicium.repository.LogsRepo;
import com.indicium.services.AuditCategory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuditLogController extends StackPane {

    // ── Header ──
    @FXML private Label countTotal;

    // ── Filters ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterAction;
    @FXML private ComboBox<String> filterRole;
    @FXML private ComboBox<String> filterType;

    // ── Log list ──
    @FXML private VBox logListContainer;
    @FXML private VBox emptyState;

    // ── Pagination ──
    @FXML private Label             lblTotalItems;
    @FXML private Label             lblCurrentPage;
    @FXML private Label             lblTotalPages;
    @FXML private Button            btnPrevPage;
    @FXML private Button            btnNextPage;
    @FXML private ComboBox<Integer> pageSizeCombo;

    // ── State ──
    private int    currentPage = 1;
    private int    pageSize    = 25;
    private int    totalItems  = 0;

    // ── Active filters (resolved before each query) ──
    private String        activeSearch   = "";
    private AuditCategory activeCategory = null;
    private String        activeRole     = null;

    private LogsRepo logsRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Constructor ──────────────────────────────────────────────

    public AuditLogController() {
        URL fxmlUrl = getClass().getResource("/com/indicium/ui/AuditLogDashboard.fxml");
        if (fxmlUrl == null)
            throw new RuntimeException("AuditLogDashboard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load AuditLogDashboard.fxml: " + e.getMessage(), e);
        }
    }

    // ── Initialize ───────────────────────────────────────────────

    @FXML
    public void initialize() {
        logsRepo = new LogsRepo();
        setupFilters();
        setupPagination();
        loadLogs();
    }

    // ═══════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════

    private void setupFilters() {
        filterAction.getItems().addAll("All", "EVIDENCE", "CASE", "TIMELINE", "AUTH", "SYSTEM");
        filterRole.getItems().addAll("All", "ADMIN", "INVESTIGATOR");
        filterType.getItems().addAll("All", "EVIDENCE", "CASE", "TIMELINE");
        filterAction.setValue("All");
        filterRole.setValue("All");
        filterType.setValue("All");
    }

    private void setupPagination() {
        pageSizeCombo.getItems().addAll(10, 25, 50, 100);
        pageSizeCombo.setValue(25);
        pageSizeCombo.setOnAction(e -> {
            pageSize    = pageSizeCombo.getValue();
            currentPage = 1;
            loadLogs();
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  LOAD & RENDER
    // ═══════════════════════════════════════════════════════════

    private void loadLogs() {
        // Resolve active filters
        activeSearch = searchField.getText().trim();

        String actionVal = filterAction.getValue();
        activeCategory = (actionVal == null || actionVal.equals("All"))
                ? null : resolveCategory(actionVal);

        activeRole = filterRole.getValue();
        if (activeRole != null && activeRole.equals("All")) activeRole = null;

        int offset = (currentPage - 1) * pageSize;

        // Count total for pagination
        totalItems = logsRepo.countLogs(activeSearch, activeCategory, activeRole);

        // Fetch page
        List<AuditLogEntry> entries = logsRepo.fetchLogs(
                activeSearch, activeCategory, activeRole, pageSize, offset);

        // Render
        logListContainer.getChildren().clear();
        logListContainer.getChildren().add(emptyState);

        if (entries.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            for (AuditLogEntry e : entries) {
                buildLogRow(
                        e.getFullName(),
                        "USR-" + String.format("%04d", e.getInvestigatorID()),
                        e.getDescription(),
                        mapEntityId(e),
                        e.getCategory(),
                        e.getRole(),
                        e.getTimestamp() != null ? e.getTimestamp().format(DATE_FMT) : "—",
                        e.getTimestamp() != null ? e.getTimestamp().format(TIME_FMT) : "—"
                );
            }
        }

        updatePaginationControls();
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    // ── Map entity ID string from entry ─────────────────────────

    private String mapEntityId(AuditLogEntry e) {
        if (e.getLinkedEvidenceID() > 0)
            return "EV-" + String.format("%04d", e.getLinkedEvidenceID());
        if (e.getLinkedCaseID() > 0)
            return "CASE-" + String.format("%04d", e.getLinkedCaseID());
        return "—";
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILD LOG ROW
    // ═══════════════════════════════════════════════════════════

    private void buildLogRow(String username, String userId, String description,
                             String entityId, String category, String role,
                             String date, String time) {

        HBox row = new HBox();
        row.getStyleClass().add("log-row");
        row.setSpacing(0);
        row.setAlignment(Pos.CENTER_LEFT);

        // ── User column ──
        HBox userCell = new HBox(10);
        userCell.setAlignment(Pos.CENTER_LEFT);
        userCell.setPrefWidth(200);

        StackPane avatar = buildAvatar(username);

        VBox nameBox = new VBox(2);
        Label lblName = new Label(username);
        lblName.getStyleClass().add("log-username");
        Label lblId = new Label(userId);
        lblId.getStyleClass().add("log-userid");
        nameBox.getChildren().addAll(lblName, lblId);
        userCell.getChildren().addAll(avatar, nameBox);

        // ── Entity ID column ──
        HBox entityCell = new HBox();
        entityCell.setAlignment(Pos.CENTER_LEFT);
        entityCell.setPrefWidth(150);
        Label lblEntity = new Label(entityId);
        lblEntity.getStyleClass().add("entity-id-chip");
        entityCell.getChildren().add(lblEntity);

        // ── Action / Description column ──
        HBox actionCell = new HBox();
        actionCell.setAlignment(Pos.CENTER_LEFT);
        actionCell.setPrefWidth(140);
        HBox actionChip = buildActionChip(mapActionFromCategory(category));
        actionCell.getChildren().add(actionChip);

        // ── Role chip ──
        HBox roleCell = new HBox();
        roleCell.setAlignment(Pos.CENTER_LEFT);
        roleCell.setPrefWidth(120);
        Label roleChip = new Label(role != null ? role : "—");
        roleChip.getStyleClass().addAll("role-chip", getRoleStyle(role));
        roleCell.getChildren().add(roleChip);

        // ── Type chip ──
        HBox typeCell = new HBox();
        typeCell.setAlignment(Pos.CENTER_LEFT);
        typeCell.setPrefWidth(120);
        Label typeChip = new Label(category != null ? category : "—");
        typeChip.getStyleClass().addAll("type-chip", getTypeStyle(category));
        typeCell.getChildren().add(typeChip);

        // ── Timestamp ──
        VBox tsCell = new VBox(2);
        tsCell.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tsCell, Priority.ALWAYS);
        Label lblDate = new Label(date);
        lblDate.getStyleClass().add("log-timestamp");
        Label lblTime = new Label(time);
        lblTime.getStyleClass().add("log-timestamp-time");
        tsCell.getChildren().addAll(lblDate, lblTime);

        // ── Tooltip with full description ──
        Tooltip tip = new Tooltip(description != null ? description : "");
        tip.setWrapText(true);
        tip.setMaxWidth(320);
        Tooltip.install(row, tip);

        row.getChildren().addAll(userCell, entityCell, actionCell, roleCell, typeCell, tsCell);
        logListContainer.getChildren().add(row);
    }

    // ── Avatar ───────────────────────────────────────────────────

    private StackPane buildAvatar(String username) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar-circle");

        String[] parts    = (username != null ? username : "?").trim().split(" ");
        String   initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
                : String.valueOf(parts[0].charAt(0));

        Label lblInitials = new Label(initials.toUpperCase());
        lblInitials.getStyleClass().add("avatar-initials");
        avatar.getChildren().add(lblInitials);

        double r = 17;
        Circle clip = new Circle(r, r, r);
        avatar.setClip(clip);
        return avatar;
    }

    // ── Action chip ──────────────────────────────────────────────

    private HBox buildActionChip(String action) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().addAll("action-chip", getActionStyle(action));

        StackPane dot = new StackPane();
        dot.setMinSize(7, 7);
        dot.setMaxSize(7, 7);
        dot.setStyle("-fx-background-radius: 50; -fx-background-color: "
                + getActionDotColor(action) + ";");

        Label lblAction = new Label(action.toUpperCase());
        lblAction.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        chip.getChildren().addAll(dot, lblAction);
        return chip;
    }

    // ═══════════════════════════════════════════════════════════
    //  FILTERS
    // ═══════════════════════════════════════════════════════════

    @FXML private void handleSearch() { currentPage = 1; loadLogs(); }
    @FXML private void handleFilter() { currentPage = 1; loadLogs(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterAction.setValue("All");
        filterRole.setValue("All");
        filterType.setValue("All");
        currentPage = 1;
        loadLogs();
    }

    // ═══════════════════════════════════════════════════════════
    //  PAGINATION
    // ═══════════════════════════════════════════════════════════

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) { currentPage--; loadLogs(); }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (currentPage < totalPages) { currentPage++; loadLogs(); }
    }

    private void updatePaginationControls() {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));

        lblTotalItems.setText(totalItems + " items");
        lblCurrentPage.setText(String.valueOf(currentPage));
        lblTotalPages.setText(String.valueOf(totalPages));
        countTotal.setText(totalItems + " Entries");

        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= totalPages);
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPORT CSV
    // ═══════════════════════════════════════════════════════════

    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Audit Log");
        chooser.setInitialFileName("audit_log_export.csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        List<AuditLogEntry> all = logsRepo.fetchAllForExport(
                activeSearch, activeCategory, activeRole);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("LogID,Timestamp,UserID,FullName,Role,Category,Description,CaseID,EvidenceID\n");
            for (AuditLogEntry e : all) {
                fw.write(String.format("%d,%s,%d,%s,%s,%s,\"%s\",%d,%d\n",
                        e.getLogID(),
                        e.getTimestamp() != null
                                ? e.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                : "",
                        e.getInvestigatorID(),
                        csvEscape(e.getFullName()),
                        csvEscape(e.getRole()),
                        csvEscape(e.getCategory()),
                        csvEscape(e.getDescription()),
                        e.getLinkedCaseID(),
                        e.getLinkedEvidenceID()
                ));
            }
            showAlert("Export Complete", "Saved to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showAlert("Export Failed", "Could not write file: " + ex.getMessage());
        }
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    // ═══════════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ═══════════════════════════════════════════════════════════

    private String mapActionFromCategory(String category) {
        if (category == null) return "View";
        return switch (category.toUpperCase()) {
            case "EVIDENCE" -> "Add";
            case "CASE"     -> "Edit";
            case "TIMELINE" -> "View";
            case "AUTH"     -> "View";
            default         -> "View";
        };
    }

    private AuditCategory resolveCategory(String val) {
        try { return AuditCategory.valueOf(val.toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private String getActionStyle(String action) {
        return switch (action) {
            case "Add"     -> "action-add";
            case "Edit"    -> "action-edit";
            case "Delete"  -> "action-delete";
            case "Archive" -> "action-archive";
            default        -> "action-view";
        };
    }

    private String getActionDotColor(String action) {
        return switch (action) {
            case "Add"     -> "#2E7D32";
            case "Edit"    -> "#E65100";
            case "Delete"  -> "#C62828";
            case "Archive" -> "#4527A0";
            default        -> "#1565C0";
        };
    }

    private String getRoleStyle(String role) {
        if (role == null) return "role-investigator";
        return switch (role.toUpperCase()) {
            case "ADMIN" -> "role-admin";
            default      -> "role-investigator";
        };
    }

    private String getTypeStyle(String type) {
        if (type == null) return "type-case";
        return switch (type.toUpperCase()) {
            case "EVIDENCE" -> "type-evidence";
            case "TIMELINE" -> "type-timeline";
            default         -> "type-case";
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
