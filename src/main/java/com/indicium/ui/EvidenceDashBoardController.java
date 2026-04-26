package com.indicium.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

public class EvidenceDashBoardController extends StackPane {

    // ── Header ──
    @FXML private Label countVerified;
    @FXML private Label countTampered;
    @FXML private Label countPending;

    // ── Filters ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;
    @FXML private DatePicker       filterDateFrom;
    @FXML private DatePicker       filterDateTo;

    // ── Evidence list ──
    @FXML private VBox evidenceListContainer;
    @FXML private VBox emptyState;

    // ── Add Evidence Modal ──
    @FXML private StackPane        modalAddEvidence;
    @FXML private VBox             dropZone;
    @FXML private Label            selectedFileName;
    @FXML private ComboBox<String> inputType;
    @FXML private TextField        inputSource;
    @FXML private DatePicker       inputDateSeized;
    @FXML private TextArea         inputDescription;
    @FXML private Label            errorSource;
    @FXML private Label            errorDateSeized;
    @FXML private Label            errorDuplicate;

    // ── Bulk Import Modal ──
    @FXML private StackPane modalBulkImport;
    @FXML private TextField bulkFolderPath;
    @FXML private VBox      bulkPreviewContainer;
    @FXML private VBox      progressArea;
    @FXML private Label     progressLabel;
    @FXML private VBox      summaryArea;
    @FXML private Label     summaryImported;
    @FXML private Label     summarySkipped;
    @FXML private Label     summaryFailed;

    // ── Link Manager Modal ──
    @FXML private StackPane modalLinkManager;
    @FXML private Label     linkManagerSubtitle;
    @FXML private VBox      linksContainer;
    @FXML private VBox      emptyLinks;
    @FXML private TextField linkSearchField;
    @FXML private VBox      linkSearchResults;

    // ── Internal state ──
    private File   selectedFile      = null;
    private String activeLinkEvidenceId = null;

    // ── Constructor ──
    public EvidenceDashBoardController()
    {
        URL fxmlUrl = getClass().getResource("/com/indicium/ui/EvidenceDashBoard.fxml");
        if (fxmlUrl == null)
            throw new RuntimeException("EvidenceDashBoard.fxml not found!");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load EvidenceDashBoard.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void initialize() {
        setupFilterOptions();
        loadEvidence();
    }

    // ── Setup ──────────────────────────────────────────────────────

    private void setupFilterOptions() {
        filterType.getItems().addAll("All", "Image", "Video", "Document",
                "Audio", "Bitstream", "Other");
        filterStatus.getItems().addAll("All", "Verified", "Tampered", "Pending");
        filterType.setValue("All");
        filterStatus.setValue("All");

        inputType.getItems().addAll("Image", "Video", "Document",
                "Audio", "Bitstream", "Other");
    }

    // ── Load Evidence ──────────────────────────────────────────────

    private void loadEvidence() {
        // TODO: Query DB for all evidence linked to the current active case
        // TODO: Call buildEvidenceRow() for each result
        // TODO: Update countVerified, countTampered, countPending labels
        showEmptyState(true);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    /**
     * Builds a single evidence row and appends it to evidenceListContainer.
     *
     * @param evidenceId  e.g. "EV-0012"
     * @param filename    Original filename
     * @param type        "Image" | "Video" | "Document" | "Audio" | "Bitstream" | "Other"
     * @param size        Human-readable size e.g. "2.4 MB"
     * @param dateSeized  e.g. "2026-04-10"
     * @param status      "Verified" | "Tampered" | "Pending"
     */
    private void buildEvidenceRow(String evidenceId, String filename, String type,
                                  String size, String dateSeized, String status) {
        HBox row = new HBox();
        row.getStyleClass().add("evidence-row");
        row.setSpacing(0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblId = new Label(evidenceId);
        lblId.getStyleClass().add("evidence-id");
        lblId.setPrefWidth(120);

        Label lblFile = new Label(filename);
        lblFile.getStyleClass().add("evidence-filename");
        lblFile.setPrefWidth(200);

        Label lblType = new Label(type);
        lblType.getStyleClass().addAll("type-chip", getTypeStyle(type));
        lblType.setPrefWidth(100);

        Label lblSize = new Label(size);
        lblSize.getStyleClass().add("evidence-meta");
        lblSize.setPrefWidth(80);

        Label lblDate = new Label(dateSeized);
        lblDate.getStyleClass().add("evidence-meta");
        lblDate.setPrefWidth(110);

        Label lblStatus = new Label(status);
        lblStatus.getStyleClass().addAll("status-chip", getStatusStyle(status));
        lblStatus.setPrefWidth(110);

        Button btnView = new Button("View");
        btnView.getStyleClass().add("btn-view");
        btnView.setOnAction(e -> handleViewEvidence(evidenceId));

        Button btnMore = new Button("⋮");
        btnMore.getStyleClass().add("btn-overflow");
        btnMore.setOnAction(e -> showOverflowMenu(btnMore, evidenceId, status));

        HBox actions = new HBox(8, btnView, btnMore);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.ALWAYS);

        row.getChildren().addAll(lblId, lblFile, lblType, lblSize, lblDate, lblStatus, actions);
        evidenceListContainer.getChildren().add(row);
    }

    private String getTypeStyle(String type) {
        return switch (type) {
            case "Image"     -> "type-image";
            case "Video"     -> "type-video";
            case "Document"  -> "type-document";
            case "Audio"     -> "type-audio";
            case "Bitstream" -> "type-bitstream";
            default          -> "type-other";
        };
    }

    private String getStatusStyle(String status) {
        return switch (status) {
            case "Verified" -> "status-verified";
            case "Tampered" -> "status-tampered";
            default         -> "status-pending";
        };
    }

    // ── Overflow Menu ──────────────────────────────────────────────

    private void showOverflowMenu(Button anchor, String evidenceId, String status) {
        ContextMenu menu = new ContextMenu();

        MenuItem view     = new MenuItem("👁  View Evidence");
        MenuItem download = new MenuItem("⬇  Download");
        MenuItem links    = new MenuItem("🔗  Manage Links");
        MenuItem delete   = new MenuItem("🗑️  Delete");

        view.setOnAction(e     -> handleViewEvidence(evidenceId));
        download.setOnAction(e -> handleDownloadEvidence(evidenceId));
        links.setOnAction(e    -> handleOpenLinkManager(evidenceId));
        delete.setOnAction(e   -> handleDeleteEvidence(evidenceId));

        delete.setDisable(true); // TODO: enable for admin role only via RBAC

        menu.getItems().addAll(view, download, new SeparatorMenuItem(), links,
                new SeparatorMenuItem(), delete);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // ── Filter Handlers ────────────────────────────────────────────

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterType.setValue("All");
        filterStatus.setValue("All");
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        applyFilters();
    }

    private void applyFilters() {
        String    search = searchField.getText().toLowerCase().trim();
        String    type   = filterType.getValue();
        String    status = filterStatus.getValue();
        LocalDate from   = filterDateFrom.getValue();
        LocalDate to     = filterDateTo.getValue();
        // TODO: Filter evidenceListContainer rows against all criteria
        // TODO: Show emptyState if no rows pass the filter
    }

    // ── Modal Helpers ──────────────────────────────────────────────

    private void closeAllModals() {
        modalAddEvidence.setVisible(false);  modalAddEvidence.setManaged(false);
        modalBulkImport.setVisible(false);   modalBulkImport.setManaged(false);
        modalLinkManager.setVisible(false);  modalLinkManager.setManaged(false);
    }

    // ── Add Evidence Modal ─────────────────────────────────────────

    @FXML
    private void handleAddEvidence() {
        closeAllModals();
        clearAddModal();
        modalAddEvidence.setVisible(true);
        modalAddEvidence.setManaged(true);
    }

    @FXML private void handleCloseAddModal() { closeAllModals(); }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            dropZone.getStyleClass().add("drop-zone-active");
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            selectedFile = db.getFiles().get(0);
            selectedFileName.setText("📎 " + selectedFile.getName());
        }
        dropZone.getStyleClass().remove("drop-zone-active");
        event.setDropCompleted(true);
        event.consume();
    }

    @FXML
    private void handlePickFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Evidence File");
        // TODO: Add extension filters per type selection
        File file = chooser.showOpenDialog(this.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            selectedFileName.setText("📎 " + file.getName());
        }
    }

    @FXML
    private void handleSubmitEvidence() {
        if (!validateAddForm()) return;

        String    source      = inputSource.getText().trim();
        LocalDate dateSeized  = inputDateSeized.getValue();
        String    type        = inputType.getValue();
        String    description = inputDescription.getText().trim();

        // TODO: Compute SHA-256 hash of selectedFile
        // TODO: Check for duplicate hash in current case — show errorDuplicate if found
        // TODO: Upload file to storage
        // TODO: Generate unique Evidence ID (e.g. "EV-XXXX")
        // TODO: INSERT evidence record into DB (filename, type, size, hash, source, dateSeized, description, caseId, uploadedBy)
        // TODO: Lock file as read-only in storage
        // TODO: Log upload event in audit log
        // TODO: Refresh evidenceListContainer + update count badges
        // TODO: Show success toast

        closeAllModals();
    }

    private boolean validateAddForm() {
        boolean valid = true;

        if (selectedFile == null) {
            // TODO: Show a "no file selected" error label in the drop zone
            valid = false;
        }

        if (inputSource.getText().trim().isEmpty()) {
            errorSource.setText("Source of evidence is required.");
            errorSource.setVisible(true);
            errorSource.setManaged(true);
            valid = false;
        } else {
            errorSource.setVisible(false);
            errorSource.setManaged(false);
        }

        if (inputDateSeized.getValue() == null) {
            errorDateSeized.setText("Date seized is required.");
            errorDateSeized.setVisible(true);
            errorDateSeized.setManaged(true);
            valid = false;
        } else {
            errorDateSeized.setVisible(false);
            errorDateSeized.setManaged(false);
        }

        return valid;
    }

    private void clearAddModal() {
        selectedFile = null;
        selectedFileName.setText("");
        inputType.setValue(null);
        inputSource.clear();
        inputDateSeized.setValue(null);
        inputDescription.clear();
        errorSource.setVisible(false);    errorSource.setManaged(false);
        errorDateSeized.setVisible(false); errorDateSeized.setManaged(false);
        errorDuplicate.setVisible(false);  errorDuplicate.setManaged(false);
        dropZone.getStyleClass().remove("drop-zone-active");
    }

    // ── Bulk Import Modal ──────────────────────────────────────────

    @FXML
    private void handleBulkImport() {
        closeAllModals();
        clearBulkModal();
        modalBulkImport.setVisible(true);
        modalBulkImport.setManaged(true);
    }

    @FXML private void handleCloseBulkModal() { closeAllModals(); }

    @FXML
    private void handlePickFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder to Import");
        File folder = chooser.showDialog(this.getScene().getWindow());
        if (folder != null) {
            bulkFolderPath.setText(folder.getAbsolutePath());
            previewBulkFiles(folder);
        }
    }

    private void previewBulkFiles(File folder) {
        bulkPreviewContainer.getChildren().clear();
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            // TODO: Show "folder is empty" message in preview area
            return;
        }
        for (File f : files) {
            if (f.isFile()) {
                buildBulkPreviewRow(f.getName(),
                        formatFileSize(f.length()),
                        getFileExtension(f.getName()));
            }
        }
        // TODO: Check against storage quota — show error if exceeded (extension 5a)
    }

    private void buildBulkPreviewRow(String filename, String size, String type) {
        HBox row = new HBox();
        row.getStyleClass().add("evidence-row");
        row.setSpacing(0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblFile   = new Label(filename); lblFile.getStyleClass().add("evidence-filename"); lblFile.setPrefWidth(220);
        Label lblSize   = new Label(size);     lblSize.getStyleClass().add("evidence-meta");     lblSize.setPrefWidth(80);
        Label lblType   = new Label(type);     lblType.getStyleClass().add("evidence-meta");     lblType.setPrefWidth(100);
        Label lblStatus = new Label("Queued"); lblStatus.getStyleClass().addAll("status-chip", "status-pending");

        row.getChildren().addAll(lblFile, lblSize, lblType, lblStatus);
        bulkPreviewContainer.getChildren().add(row);
    }

    @FXML
    private void handleStartImport() {
        if (bulkFolderPath.getText().isEmpty()) return;

        progressArea.setVisible(true);
        progressArea.setManaged(true);

        // TODO: Run import on a background thread (Task<Void>) to keep UI responsive
        // TODO: For each file:
        //   - Compute SHA-256 hash
        //   - Check for duplicate hash in case (extension 7b) — prompt keep/skip
        //   - If read error or hash failure, skip + flag (extension 7a)
        //   - INSERT evidence record into DB
        //   - Update progress bar on JavaFX thread via Platform.runLater()
        // TODO: On completion:
        //   - Hide progressArea
        //   - Show summaryArea with imported/skipped/failed counts
        //   - Log bulk import event in audit log
        //   - Refresh evidenceListContainer
    }

    private void clearBulkModal() {
        bulkFolderPath.clear();
        bulkPreviewContainer.getChildren().clear();
        progressArea.setVisible(false);  progressArea.setManaged(false);
        summaryArea.setVisible(false);   summaryArea.setManaged(false);
    }

    // ── Link Manager Modal ─────────────────────────────────────────

    private void handleOpenLinkManager(String evidenceId) {
        closeAllModals();
        activeLinkEvidenceId = evidenceId;
        linkManagerSubtitle.setText("Evidence: " + evidenceId);
        loadExistingLinks(evidenceId);
        modalLinkManager.setVisible(true);
        modalLinkManager.setManaged(true);
    }

    @FXML private void handleCloseLinkModal() { closeAllModals(); activeLinkEvidenceId = null; }

    private void loadExistingLinks(String evidenceId) {
        linksContainer.getChildren().clear();
        linksContainer.getChildren().add(emptyLinks);
        // TODO: Query DB for all cross-case links where source_evidence_id = evidenceId
        // TODO: Call buildLinkRow() for each result
        // TODO: Hide emptyLinks if links exist
    }

    private void buildLinkRow(String targetCaseId, String targetEvidenceId,
                              String linkedBy, String timestamp) {
        HBox row = new HBox();
        row.getStyleClass().add("evidence-row");
        row.setSpacing(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblCase     = new Label(targetCaseId);     lblCase.getStyleClass().add("evidence-id");
        Label lblEvidence = new Label(targetEvidenceId); lblEvidence.getStyleClass().add("evidence-filename");
        Label lblBy       = new Label("by " + linkedBy); lblBy.getStyleClass().add("evidence-meta");
        Label lblTime     = new Label(timestamp);        lblTime.getStyleClass().add("evidence-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRemove = new Button("Remove");
        btnRemove.getStyleClass().add("btn-clear-filters");
        btnRemove.setOnAction(e -> handleRemoveLink(targetCaseId, targetEvidenceId));

        row.getChildren().addAll(lblCase, lblEvidence, lblBy, lblTime, spacer, btnRemove);
        linksContainer.getChildren().add(row);
    }

    @FXML
    private void handleSearchLinkCase() {
        String query = linkSearchField.getText().trim();
        if (query.isEmpty()) return;

        linkSearchResults.setVisible(true);
        linkSearchResults.setManaged(true);
        linkSearchResults.getChildren().clear();

        // TODO: Query DB for cases matching query (by ID or title)
        // TODO: Trigger "Authorize Case Access" check for each result (UC-4)
        // TODO: Build result rows — each with a "Link" button
        // TODO: On "Link" click: check for existing link conflict (extension 5a)
        //   - If conflict: show notification, reject
        //   - If clear: INSERT link record into DB, log in audit trail, refresh linksContainer
    }

    private void handleRemoveLink(String targetCaseId, String targetEvidenceId) {
        // TODO: Confirmation dialog
        // TODO: DELETE link record from DB
        // TODO: Log removal in audit trail
        // TODO: Refresh linksContainer
    }

    // ── Evidence Row Actions ───────────────────────────────────────

    private void handleViewEvidence(String evidenceId) {
        // TODO: Re-compute SHA-256 hash of stored file
        // TODO: Compare against stored hash in DB
        //   - Match   → open viewer overlay (read-only)
        //   - Mismatch → block open, show tamper warning, flag for admin, update status chip
        // TODO: Log "View" event in audit log
    }

    private void handleDownloadEvidence(String evidenceId) {
        // TODO: Show legal tracking warning dialog — require confirmation
        // TODO: On confirm: copy file to user-selected path
        // TODO: Verify hash of downloaded copy matches original
        // TODO: Log "Download" event in audit log (distinct from View)
    }

    private void handleDeleteEvidence(String evidenceId) {
        // TODO: RBAC check — admin only
        // TODO: Confirmation dialog
        // TODO: DELETE evidence record from DB + remove from storage
        // TODO: Log deletion in audit trail
        // TODO: Remove row from evidenceListContainer + update count badges
    }

    // ── Utilities ─────────────────────────────────────────────────

    private String formatFileSize(long bytes) {
        if (bytes < 1024)             return bytes + " B";
        if (bytes < 1024 * 1024)      return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toUpperCase() : "Unknown";
    }
}
