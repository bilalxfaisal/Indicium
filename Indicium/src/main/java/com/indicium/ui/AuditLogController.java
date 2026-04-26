package com.indicium.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.net.URL;
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

    // ── Pagination state ──
    private int currentPage = 1;
    private int pageSize    = 25;
    private int totalItems  = 0;

    // ── Constructor ──
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

    @FXML
    public void initialize() {
        setupFilters();
        setupPagination();
        loadLogs();
    }

    // ── Setup ──────────────────────────────────────────────────

    private void setupFilters() {
        filterAction.getItems().addAll("All", "View", "Add", "Edit", "Delete", "Archive");
        filterRole.getItems().addAll("All", "Admin", "Investigator");
        filterType.getItems().addAll("All", "Evidence", "Case", "Timeline");
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

    // ── Load Logs ──────────────────────────────────────────────

    private void loadLogs() {
        logListContainer.getChildren().clear();
        logListContainer.getChildren().add(emptyState);

        // TODO: Query DB for audit log entries with pagination
        //   SELECT * FROM audit_log
        //   ORDER BY timestamp DESC
        //   LIMIT pageSize OFFSET (currentPage - 1) * pageSize
        // TODO: Also run COUNT(*) query for totalItems
        // TODO: Call buildLogRow() for each result
        // TODO: Update countTotal, lblTotalItems, lblCurrentPage, lblTotalPages

        showEmptyState(true); // remove once DB is wired
        updatePaginationControls();
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    /**
     * Builds a single audit log row.
     *
     * @param username   Display name of the user
     * @param userId     Short user ID string e.g. "USR-0012"
     * @param action     "View" | "Add" | "Edit" | "Delete" | "Archive"
     * @param role       "Admin" | "Investigator"
     * @param type       "Evidence" | "Case" | "Timeline"
     * @param date       e.g. "24 Apr 2026"
     * @param time       e.g. "11:03"
     */
    private void buildLogRow(String username, String userId, String action,
                             String role, String type, String date, String time) {

        HBox row = new HBox();
        row.getStyleClass().add("log-row");
        row.setSpacing(0);
        row.setAlignment(Pos.CENTER_LEFT);

        // ── User column (avatar + name) ──
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
        Label lblEntity = new Label(userId); // TODO: replace with actual entity ID
        lblEntity.getStyleClass().add("entity-id-chip");
        entityCell.getChildren().add(lblEntity);

        // ── Action chip ──
        HBox actionCell = new HBox();
        actionCell.setAlignment(Pos.CENTER_LEFT);
        actionCell.setPrefWidth(140);
        HBox actionChip = buildActionChip(action);
        actionCell.getChildren().add(actionChip);

        // ── Role chip ──
        HBox roleCell = new HBox();
        roleCell.setAlignment(Pos.CENTER_LEFT);
        roleCell.setPrefWidth(120);
        Label roleChip = new Label(role);
        roleChip.getStyleClass().addAll("role-chip", getRoleStyle(role));
        roleCell.getChildren().add(roleChip);

        // ── Type chip ──
        HBox typeCell = new HBox();
        typeCell.setAlignment(Pos.CENTER_LEFT);
        typeCell.setPrefWidth(120);
        Label typeChip = new Label(type);
        typeChip.getStyleClass().addAll("type-chip", getTypeStyle(type));
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

        row.getChildren().addAll(userCell, entityCell, actionCell, roleCell, typeCell, tsCell);
        logListContainer.getChildren().add(row);
    }

    // ── Avatar builder ─────────────────────────────────────────

    private StackPane buildAvatar(String username) {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("avatar-circle");

        // Get initials — first letter of first + last word
        String[] parts    = username.trim().split(" ");
        String   initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
                : String.valueOf(parts[0].charAt(0));

        Label lblInitials = new Label(initials.toUpperCase());
        lblInitials.getStyleClass().add("avatar-initials");
        avatar.getChildren().add(lblInitials);

        // Clip to circle
        double r = 17;
        Circle clip = new Circle(r, r, r);
        avatar.setClip(clip);

        return avatar;
    }

    // ── Action chip builder ────────────────────────────────────

    /**
     * Builds the colored rounded action chip with a dot indicator,
     * matching the inspo image style.
     */
    private HBox buildActionChip(String action) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().addAll("action-chip", getActionStyle(action));

        // Dot indicator
        StackPane dot = new StackPane();
        double dotSize = 7;
        dot.setMinSize(dotSize, dotSize);
        dot.setMaxSize(dotSize, dotSize);
        dot.setStyle("-fx-background-radius: 50; -fx-background-color: " + getActionDotColor(action) + ";");

        Label lblAction = new Label(action.toUpperCase());
        lblAction.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        chip.getChildren().addAll(dot, lblAction);
        return chip;
    }

    // ── Style helpers ──────────────────────────────────────────

    private String getActionStyle(String action) {
        return switch (action) {
            case "View"    -> "action-view";
            case "Add"     -> "action-add";
            case "Edit"    -> "action-edit";
            case "Delete"  -> "action-delete";
            case "Archive" -> "action-archive";
            default        -> "action-view";
        };
    }

    private String getActionDotColor(String action) {
        return switch (action) {
            case "View"    -> "#1565C0";
            case "Add"     -> "#2E7D32";
            case "Edit"    -> "#E65100";
            case "Delete"  -> "#C62828";
            case "Archive" -> "#4527A0";
            default        -> "#90A4AE";
        };
    }

    private String getRoleStyle(String role) {
        return switch (role) {
            case "Admin"        -> "role-admin";
            case "Investigator" -> "role-investigator";
            default             -> "role-investigator";
        };
    }

    private String getTypeStyle(String type) {
        return switch (type) {
            case "Evidence" -> "type-evidence";
            case "Case"     -> "type-case";
            case "Timeline" -> "type-timeline";
            default         -> "type-case";
        };
    }

    // ── Filter Handlers ────────────────────────────────────────

    @FXML private void handleSearch() { currentPage = 1; applyFilters(); }
    @FXML private void handleFilter() { currentPage = 1; applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterAction.setValue("All");
        filterRole.setValue("All");
        filterType.setValue("All");
        currentPage = 1;
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        String action = filterAction.getValue();
        String role   = filterRole.getValue();
        String type   = filterType.getValue();
        // TODO: Re-query DB with WHERE clauses for each active filter
        // TODO: Reset pagination and reload rows
        loadLogs();
    }

    // ── Pagination ─────────────────────────────────────────────

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadLogs();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (currentPage < totalPages) {
            currentPage++;
            loadLogs();
        }
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

    // ── Export ─────────────────────────────────────────────────

    @FXML
    private void handleExport() {
        // TODO: Open FileChooser for .csv save path
        // TODO: Write all filtered audit entries to CSV
        //   columns: timestamp, username, userId, action, role, type
        // TODO: Show success toast on completion
    }
}
