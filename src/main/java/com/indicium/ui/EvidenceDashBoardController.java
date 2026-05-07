package com.indicium.ui;

import com.indicium.controllers.EvidenceManager;
import com.indicium.models.Case;
import com.indicium.models.CaseStatus;
import com.indicium.models.Evidence;
import com.indicium.models.EvidenceStatus;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.EvidenceRepo;
import com.indicium.services.HashGenerator;
import com.indicium.services.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EvidenceDashBoardController extends StackPane {

    // ── Header badges ──
    @FXML private Label countVerified;
    @FXML private Label countTampered;
    @FXML private Label countPending;

    // ── Case selector ──
    @FXML private ComboBox<Case> caseSelector;
    @FXML private HBox           activeCaseBanner;
    @FXML private Label          activeCaseLabel;

    // ── Filters ──
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;
    @FXML private DatePicker       filterDateFrom;
    @FXML private DatePicker       filterDateTo;

    // ── Evidence list ──
    @FXML private VBox   evidenceListContainer;
    @FXML private VBox   emptyState;
    @FXML private Label  emptyStateTitle;
    @FXML private Label  emptyStateSub;
    @FXML private Button emptyAddBtn;

    // ── Add Evidence Modal ──
    @FXML private StackPane        modalAddEvidence;
    @FXML private Label            modalCaseTag;
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
    @FXML private Label     bulkCaseTag;
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
    private File           selectedFile          = null;
    private int            currentCaseID         = -1;
    private int            currentUserID;
    private String         currentCaseTitle      = "";
    private String         activeLinkEvidenceId  = null;
    private List<Evidence> allEvidence           = new ArrayList<>();
    private List<Case>     allOpenCases          = new ArrayList<>();

    // ── Linked cases per evidence (in-memory, keyed by evidenceId) ──
    private final java.util.Map<String, List<Case>> linkedCasesMap = new java.util.HashMap<>();

    private EvidenceRepo    evidenceRepo;
    private EvidenceManager evidenceManager;
    private CaseRepository  caseRepo;

    // =============================================
    //  Constructor
    // =============================================

    public EvidenceDashBoardController() {
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

    // =============================================
    //  Public entry point (called from CaseDashBoard)
    // =============================================

    public void initWithCase(int caseID, String caseTitle) {
        this.currentCaseID    = caseID;
        this.currentCaseTitle = caseTitle;

        if (caseSelector != null) {
            caseSelector.getItems().stream()
                    .filter(c -> c.getCaseID() == caseID)
                    .findFirst()
                    .ifPresent(c -> caseSelector.setValue(c));
        }

        updateActiveCaseBanner(caseTitle);
        loadEvidence();
    }

    // =============================================
    //  Initialize
    // =============================================

    @FXML
    public void initialize() {
        currentUserID   = SessionManager.getInstance().getCurrentUser().getUserID();
        evidenceRepo    = new EvidenceRepo();
        evidenceManager = new EvidenceManager();
        caseRepo        = new CaseRepository();
        setupFilterOptions();
        setupCaseSelector();
        showEmptyState(true, false);
    }

    // =============================================
    //  Case Selector
    // =============================================

    private void setupCaseSelector() {
        caseSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Case c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    setText("#" + String.format("%04d", c.getCaseID()) + "  —  " + c.getTitle());
                }
            }
        });
        caseSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Case c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText("Select a case...");
                    setStyle("-fx-text-fill: #90A4AE;");
                } else {
                    setText("#" + String.format("%04d", c.getCaseID()) + "  —  " + c.getTitle());
                    setStyle("-fx-text-fill: #0D1B1E; -fx-font-weight: bold;");
                }
            }
        });
        loadOpenCases();
    }

    private void loadOpenCases() {
        try {
            allOpenCases = caseRepo.findAll().stream()
                    .filter(c -> c.getStatus() == CaseStatus.OPEN)
                    .collect(Collectors.toList());
            caseSelector.getItems().setAll(allOpenCases);
        } catch (Exception e) {
            System.err.println("[EvidenceDashBoard] Failed to load cases: " + e.getMessage());
        }
    }

    @FXML
    private void handleCaseSelected() {
        Case selected = caseSelector.getValue();
        if (selected == null) return;
        currentCaseID    = selected.getCaseID();
        currentCaseTitle = selected.getTitle();
        updateActiveCaseBanner(currentCaseTitle);
        loadEvidence();
    }

    @FXML
    private void handleClearCase() {
        caseSelector.setValue(null);
        currentCaseID    = -1;
        currentCaseTitle = "";
        allEvidence      = new ArrayList<>();
        activeCaseBanner.setVisible(false);
        activeCaseBanner.setManaged(false);
        countVerified.setText("0 Verified");
        countTampered.setText("0 Archived");
        countPending.setText("0 Collected");
        showEmptyState(true, false);
    }

    private void updateActiveCaseBanner(String caseTitle) {
        activeCaseLabel.setText("Viewing evidence for:  " + caseTitle);
        activeCaseBanner.setVisible(true);
        activeCaseBanner.setManaged(true);
    }

    // =============================================
    //  Filter Setup
    // =============================================

    private void setupFilterOptions() {
        filterType.getItems().addAll("All", "Image", "Video", "Document", "Audio", "Bitstream", "Other");
        filterStatus.getItems().addAll("All", "Verified", "Collected", "Linked", "Archived", "Discarded");
        filterType.setValue("All");
        filterStatus.setValue("All");
        inputType.getItems().addAll("Image", "Video", "Document", "Audio", "Bitstream", "Other");
    }

    // =============================================
    //  Load & Render Evidence
    // =============================================

    private void loadEvidence() {
        if (currentCaseID == -1) {
            showEmptyState(true, false);
            return;
        }
        try {
            allEvidence = evidenceRepo.findByCaseId(currentCaseID);
            renderEvidence(allEvidence);
            updateCountBadges(allEvidence);
        } catch (Exception e) {
            System.err.println("[EvidenceDashBoard] Load failed: " + e.getMessage());
            showEmptyState(true, true);
        }
    }

    private void renderEvidence(List<Evidence> list) {
        evidenceListContainer.getChildren().removeIf(n -> n.getStyleClass().contains("evidence-row"));

        if (list == null || list.isEmpty()) {
            showEmptyState(true, true);
            return;
        }
        showEmptyState(false, true);
        for (Evidence e : list) {
            buildEvidenceRow(
                    String.valueOf(e.getEvidenceID()),
                    e.getName(),
                    e.getType() != null ? e.getType() : "Other",
                    formatSize(e.getSize()),
                    e.getDateSeized() != null ? e.getDateSeized().toLocalDate().toString() : "N/A",
                    mapStatusToDisplay(e.getStatus())
            );
        }
    }

    private void updateCountBadges(List<Evidence> list) {
        long verified  = list.stream().filter(e -> e.getStatus() == EvidenceStatus.VERIFIED).count();
        long archived  = list.stream().filter(e -> e.getStatus() == EvidenceStatus.ARCHIVED).count();
        long collected = list.stream().filter(e -> e.getStatus() == EvidenceStatus.COLLECTED).count();
        countVerified.setText(verified  + " Verified");
        countTampered.setText(archived  + " Archived");
        countPending.setText(collected  + " Collected");
    }

    private void showEmptyState(boolean show, boolean caseLoaded) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
        if (show) {
            if (caseLoaded) {
                emptyStateTitle.setText("No evidence found for this case.");
                emptyStateSub.setText("Upload the first piece of evidence using the button above.");
                emptyAddBtn.setVisible(true);
                emptyAddBtn.setManaged(true);
            } else {
                emptyStateTitle.setText("Select a case to view evidence.");
                emptyStateSub.setText("Use the case selector above to load evidence for a case.");
                emptyAddBtn.setVisible(false);
                emptyAddBtn.setManaged(false);
            }
        }
    }

    // =============================================
    //  Build Evidence Row
    // =============================================

    private void buildEvidenceRow(String evidenceId, String filename, String type,
                                  String size, String dateSeized, String status) {
        HBox row = new HBox();
        row.getStyleClass().add("evidence-row");
        row.setSpacing(0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblId     = new Label(evidenceId); lblId.getStyleClass().add("evidence-id");        lblId.setPrefWidth(120);
        Label lblFile   = new Label(filename);   lblFile.getStyleClass().add("evidence-filename"); lblFile.setPrefWidth(200);
        Label lblType   = new Label(type);       lblType.getStyleClass().addAll("type-chip", getTypeStyle(type)); lblType.setPrefWidth(100);
        Label lblSize   = new Label(size);       lblSize.getStyleClass().add("evidence-meta");     lblSize.setPrefWidth(80);
        Label lblDate   = new Label(dateSeized); lblDate.getStyleClass().add("evidence-meta");     lblDate.setPrefWidth(110);
        Label lblStatus = new Label(status);     lblStatus.getStyleClass().addAll("status-chip", getStatusStyle(status)); lblStatus.setPrefWidth(110);

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

    // =============================================
    //  Overflow Menu
    // =============================================

    private void showOverflowMenu(Button anchor, String evidenceId, String status) {
        ContextMenu menu = new ContextMenu();

        String  role    = SessionManager.getInstance().getCurrentUser().getRole().toString();
        boolean isAdmin = role.equalsIgnoreCase("ADMIN");

        MenuItem view     = makeMenuItem("View",             "icons8-open-100.png");
        MenuItem download = makeMenuItem("Download",         "icons8-edit-100.png");
        MenuItem verify   = makeMenuItem("Verify Integrity", "icons8-lock-100.png");
        MenuItem link     = makeMenuItem("Manage Links",     "icons8-link-100.png");
        MenuItem discard  = makeMenuItem("Discard",          "icons8-delete-100.png");
        discard.getStyleClass().add("menu-item-delete");
        discard.setDisable(!isAdmin || "Discarded".equalsIgnoreCase(status));

        MenuItem restore = makeMenuItem("Restore", "icons8-edit-100.png");
        restore.setDisable(!isAdmin || !"Discarded".equalsIgnoreCase(status));

        view.setOnAction(e     -> handleViewEvidence(evidenceId));
        download.setOnAction(e -> handleDownloadEvidence(evidenceId));
        verify.setOnAction(e   -> handleVerifyEvidence(evidenceId));
        link.setOnAction(e     -> handleOpenLinkManager(evidenceId));
        discard.setOnAction(e  -> handleDiscardEvidence(evidenceId));
        restore.setOnAction(e  -> handleRestoreEvidence(evidenceId));

        menu.getItems().addAll(view, download, new SeparatorMenuItem(),
                verify, link, new SeparatorMenuItem(), discard, restore);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private MenuItem makeMenuItem(String label, String iconFile) {
        MenuItem item = new MenuItem(label);
        try {
            var stream = getClass().getResourceAsStream("/com/indicium/ui/Assets/" + iconFile);
            if (stream != null) {
                ImageView icon = new ImageView(new Image(stream));
                icon.setFitWidth(14); icon.setFitHeight(14); icon.setPreserveRatio(true);
                item.setGraphic(icon);
            }
        } catch (Exception e) {
            System.err.println("[EvidenceDashBoard] Icon missing: " + iconFile);
        }
        return item;
    }

    // =============================================
    //  Filter Handlers
    // =============================================

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterType.setValue("All");
        filterStatus.setValue("All");
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        renderEvidence(allEvidence);
        updateCountBadges(allEvidence);
    }

    private void applyFilters() {
        if (allEvidence == null) return;

        String search = searchField.getText().toLowerCase().trim();
        String type   = filterType.getValue();
        String status = filterStatus.getValue();

        List<Evidence> filtered = allEvidence.stream()
                .filter(e -> search.isEmpty()
                        || e.getName().toLowerCase().contains(search)
                        || String.valueOf(e.getEvidenceID()).contains(search))
                .filter(e -> type.equals("All")
                        || (e.getType() != null && e.getType().equalsIgnoreCase(type)))
                .filter(e -> status.equals("All")
                        || mapStatusToDisplay(e.getStatus()).equalsIgnoreCase(status))
                .collect(Collectors.toList());

        renderEvidence(filtered);
        updateCountBadges(filtered);
    }

    // =============================================
    //  Add Evidence Modal
    // =============================================

    @FXML
    private void handleAddEvidence() {
        if (currentCaseID == -1) {
            showAlert("No Case Selected", "Please select a case from the dropdown above before adding evidence.");
            return;
        }
        selectedFile = null;
        selectedFileName.setText("No file selected");
        inputType.setValue(null);
        inputSource.clear();
        inputDateSeized.setValue(null);
        inputDescription.clear();
        modalCaseTag.setText("#" + String.format("%04d", currentCaseID) + "  —  " + currentCaseTitle);
        hideErrors();
        modalAddEvidence.setVisible(true);
        modalAddEvidence.setManaged(true);
    }

    @FXML
    private void handleCloseAddModal() {
        modalAddEvidence.setVisible(false);
        modalAddEvidence.setManaged(false);
    }

    @FXML
    private void handlePickFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Evidence File");
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            selectedFileName.setText(file.getName());
        }
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles())
            event.acceptTransferModes(TransferMode.COPY);
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles() && !db.getFiles().isEmpty()) {
            selectedFile = db.getFiles().get(0);
            selectedFileName.setText(selectedFile.getName());
        }
        event.setDropCompleted(true);
        event.consume();
    }

    @FXML
    private void handleSubmitEvidence() {
        if (!validateEvidenceForm()) return;
        if (currentCaseID == -1) {
            showAlert("No Case Selected", "Please select a case from the dropdown above.");
            return;
        }

        Evidence result = evidenceManager.ingestEvidence(
                currentUserID, currentCaseID, currentCaseTitle, selectedFile
        );

        if (result == null) {
            errorDuplicate.setText("Duplicate file or ingest failed.");
            errorDuplicate.setVisible(true);
            errorDuplicate.setManaged(true);
            return;
        }

        System.out.println("[EvidenceDashBoard] Evidence ingested: " + result.getEvidenceID());
        loadEvidence();
        handleCloseAddModal();
    }

    // =============================================
    //  Bulk Import Modal
    // =============================================

    @FXML
    private void handleBulkImport() {
        if (currentCaseID == -1) {
            showAlert("No Case Selected", "Please select a case from the dropdown above before importing.");
            return;
        }
        bulkFolderPath.clear();
        bulkPreviewContainer.getChildren().clear();
        bulkCaseTag.setText("#" + String.format("%04d", currentCaseID) + "  —  " + currentCaseTitle);
        progressArea.setVisible(false);  progressArea.setManaged(false);
        summaryArea.setVisible(false);   summaryArea.setManaged(false);
        modalBulkImport.setVisible(true);
        modalBulkImport.setManaged(true);
    }

    @FXML
    private void handleCloseBulkModal() {
        modalBulkImport.setVisible(false);
        modalBulkImport.setManaged(false);
    }

    @FXML
    private void handlePickFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");
        File folder = chooser.showDialog(getScene().getWindow());
        if (folder != null) {
            bulkFolderPath.setText(folder.getAbsolutePath());
            previewBulkFiles(folder);
        }
    }

    private void previewBulkFiles(File folder) {
        bulkPreviewContainer.getChildren().clear();
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) return;
        for (File f : files) {
            if (f.isFile()) {
                Label lbl = new Label(f.getName() + "  (" + formatSize(f.length()) + ")");
                lbl.getStyleClass().add("bulk-preview-item");
                lbl.setStyle("-fx-padding: 6 12 6 12; -fx-text-fill: #0D1B1E; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");
                bulkPreviewContainer.getChildren().add(lbl);
            }
        }
    }

    @FXML
    private void handleStartImport() {
        String path = bulkFolderPath.getText().trim();
        if (path.isEmpty()) {
            showAlert("No Folder Selected", "Please select a folder to import from.");
            return;
        }

        File   folder = new File(path);
        File[] files  = folder.listFiles();
        if (files == null) {
            showAlert("Invalid Folder", "The selected path is not a valid folder.");
            return;
        }

        int imported = 0, skipped = 0, failed = 0;
        progressArea.setVisible(true);
        progressArea.setManaged(true);

        for (File f : files) {
            if (!f.isFile()) continue;
            progressLabel.setText("Processing: " + f.getName());

            try {
                Evidence result = evidenceManager.ingestEvidence(
                        currentUserID, currentCaseID, currentCaseTitle, f
                );
                if (result != null) imported++;
                else                skipped++;
            } catch (Exception e) {
                System.err.println("[BulkImport] Failed: " + f.getName() + " — " + e.getMessage());
                failed++;
            }
        }

        summaryImported.setText("✅ " + imported + " files imported");
        summarySkipped.setText("⚠ "  + skipped  + " duplicates skipped");
        summaryFailed.setText("❌ "   + failed   + " files failed");
        summaryArea.setVisible(true);
        summaryArea.setManaged(true);
        progressLabel.setText("Done.");

        loadEvidence();
    }

    // =============================================
    //  Link Manager Modal
    // =============================================

    private void handleOpenLinkManager(String evidenceId) {
        activeLinkEvidenceId = evidenceId;

        // Find the evidence name for the subtitle
        String evidenceName = allEvidence.stream()
                .filter(e -> String.valueOf(e.getEvidenceID()).equals(evidenceId))
                .map(Evidence::getName)
                .findFirst()
                .orElse("EV-" + evidenceId);

        linkManagerSubtitle.setText("EV-" + evidenceId + "  —  " + evidenceName);
        linkSearchField.clear();
        linkSearchResults.getChildren().clear();
        linkSearchResults.setVisible(false);
        linkSearchResults.setManaged(false);

        renderExistingLinks(evidenceId);

        modalLinkManager.setVisible(true);
        modalLinkManager.setManaged(true);
    }

    private void renderExistingLinks(String evidenceId) {
        // Remove all existing link rows (keep emptyLinks node)
        linksContainer.getChildren().removeIf(n -> n.getStyleClass().contains("link-row"));

        List<Case> linked = linkedCasesMap.getOrDefault(evidenceId, new ArrayList<>());

        boolean hasLinks = !linked.isEmpty();
        emptyLinks.setVisible(!hasLinks);
        emptyLinks.setManaged(!hasLinks);

        for (Case c : linked) {
            buildLinkRow(evidenceId, c);
        }
    }

    private void buildLinkRow(String evidenceId, Case c) {
        HBox row = new HBox();
        row.getStyleClass().add("link-row");
        row.setSpacing(0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10 16 10 16; " +
                "-fx-border-color: transparent transparent #F0F0F0 transparent; -fx-border-width: 0 0 1 0;");

        Label lblCaseId = new Label("#" + String.format("%04d", c.getCaseID()));
        lblCaseId.setStyle("-fx-text-fill: #90A4AE; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblCaseId.setPrefWidth(80);

        Label lblTitle = new Label(c.getTitle());
        lblTitle.setStyle("-fx-text-fill: #0D1B1E; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblStatus = new Label(c.getStatus() != null ? c.getStatus().toString() : "OPEN");
        lblStatus.getStyleClass().addAll("status-chip", "status-active");

        Button btnUnlink = new Button("Unlink");
        btnUnlink.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF5350; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; " +
                "-fx-border-color: #EF5350; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-cursor: hand;");
        btnUnlink.setOnAction(e -> handleUnlinkCase(evidenceId, c));

        row.getChildren().addAll(lblCaseId, lblTitle, lblStatus, btnUnlink);
        linksContainer.getChildren().add(row);
    }

    @FXML
    private void handleSearchLinkCase() {
        String query = linkSearchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            linkSearchResults.setVisible(false);
            linkSearchResults.setManaged(false);
            return;
        }

        // Filter open cases — exclude current case and already linked ones
        List<Case> linked  = linkedCasesMap.getOrDefault(activeLinkEvidenceId, new ArrayList<>());
        List<Integer> linkedIds = linked.stream().map(Case::getCaseID).collect(Collectors.toList());

        List<Case> results = allOpenCases.stream()
                .filter(c -> c.getCaseID() != currentCaseID)
                .filter(c -> !linkedIds.contains(c.getCaseID()))
                .filter(c -> c.getTitle().toLowerCase().contains(query)
                        || String.format("%04d", c.getCaseID()).contains(query))
                .collect(Collectors.toList());

        linkSearchResults.getChildren().clear();

        if (results.isEmpty()) {
            Label none = new Label("No matching cases found.");
            none.setStyle("-fx-text-fill: #90A4AE; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-padding: 10 16 10 16;");
            linkSearchResults.getChildren().add(none);
        } else {
            for (Case c : results) {
                buildSearchResultRow(c);
            }
        }

        linkSearchResults.setVisible(true);
        linkSearchResults.setManaged(true);
    }

    private void buildSearchResultRow(Case c) {
        HBox row = new HBox();
        row.setSpacing(0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 8 16 8 16; " +
                "-fx-border-color: transparent transparent #F0F0F0 transparent; -fx-border-width: 0 0 1 0;");

        Label lblCaseId = new Label("#" + String.format("%04d", c.getCaseID()));
        lblCaseId.setStyle("-fx-text-fill: #90A4AE; -fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblCaseId.setPrefWidth(80);

        Label lblTitle = new Label(c.getTitle());
        lblTitle.setStyle("-fx-text-fill: #0D1B1E; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Button btnLink = new Button("+ Link");
        btnLink.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: #FFFFFF; " +
                "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-border-color: transparent; " +
                "-fx-padding: 4 12 4 12; -fx-cursor: hand;");
        btnLink.setOnAction(e -> handleLinkCase(c));

        row.getChildren().addAll(lblCaseId, lblTitle, btnLink);
        linkSearchResults.getChildren().add(row);
    }

    private void handleLinkCase(Case targetCase) {
        if (activeLinkEvidenceId == null) return;

        // Add to in-memory map
        linkedCasesMap
                .computeIfAbsent(activeLinkEvidenceId, k -> new ArrayList<>())
                .add(targetCase);

        System.out.println("[EvidenceDashBoard] Linked EV-" + activeLinkEvidenceId
                + " → Case #" + targetCase.getCaseID() + " (" + targetCase.getTitle() + ")");

        // Refresh the links panel and clear search
        linkSearchField.clear();
        linkSearchResults.getChildren().clear();
        linkSearchResults.setVisible(false);
        linkSearchResults.setManaged(false);
        renderExistingLinks(activeLinkEvidenceId);

        // Update evidence status to LINKED if not already verified
        allEvidence.stream()
                .filter(e -> String.valueOf(e.getEvidenceID()).equals(activeLinkEvidenceId))
                .findFirst()
                .ifPresent(e -> {
                    if (e.getStatus() == EvidenceStatus.COLLECTED) {
                        e.setStatus(EvidenceStatus.LINKED);
                        com.indicium.repository.EvidenceRepo.updateStatus(e.getEvidenceID(), EvidenceStatus.LINKED);
                        loadEvidence();
                    }
                });
    }

    private void handleUnlinkCase(String evidenceId, Case targetCase) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Link");
        confirm.setHeaderText("Unlink from Case #" + String.format("%04d", targetCase.getCaseID()) + "?");
        confirm.setContentText("This will remove the cross-case link for EV-" + evidenceId + ".");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<Case> linked = linkedCasesMap.get(evidenceId);
                if (linked != null) {
                    linked.removeIf(c -> c.getCaseID() == targetCase.getCaseID());
                    
                    if (linked.isEmpty()) {
                        allEvidence.stream()
                                .filter(e -> String.valueOf(e.getEvidenceID()).equals(evidenceId))
                                .findFirst()
                                .ifPresent(e -> {
                                    if (e.getStatus() == EvidenceStatus.LINKED) {
                                        e.setStatus(EvidenceStatus.COLLECTED);
                                        com.indicium.repository.EvidenceRepo.updateStatus(e.getEvidenceID(), EvidenceStatus.COLLECTED);
                                        loadEvidence();
                                    }
                                });
                    }
                }
                System.out.println("[EvidenceDashBoard] Unlinked EV-" + evidenceId
                        + " from Case #" + targetCase.getCaseID());
                renderExistingLinks(evidenceId);
            }
        });
    }

    @FXML
    private void handleCloseLinkModal() {
        modalLinkManager.setVisible(false);
        modalLinkManager.setManaged(false);
        activeLinkEvidenceId = null;
    }

    // =============================================
    //  Evidence Row Actions
    // =============================================

    private void handleViewEvidence(String evidenceId) {
        int      id = Integer.parseInt(evidenceId);
        Evidence e  = evidenceRepo.findById(id);
        if (e == null) return;

        String storedHash  = e.getDigitalFingerprint();
        String currentHash = HashGenerator.generateSHA256(e.getFilePath());

        if (storedHash == null || !storedHash.equals(currentHash)) {
            showAlert("Integrity Check Failed",
                    "File hash mismatch — this evidence may have been tampered with.\n" +
                            "Evidence ID: " + evidenceId);
            e.setStatus(EvidenceStatus.DISCARDED);
            com.indicium.repository.EvidenceRepo.updateStatus(id, EvidenceStatus.DISCARDED);
            loadEvidence();
            return;
        }

        // Build link summary string
        List<Case> linked = linkedCasesMap.getOrDefault(evidenceId, new ArrayList<>());
        String linkSummary = linked.isEmpty()
                ? "None"
                : linked.stream()
                .map(c -> "#" + String.format("%04d", c.getCaseID()) + " " + c.getTitle())
                .collect(Collectors.joining(", "));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Evidence — " + e.getName());
        alert.setHeaderText("EV-" + e.getEvidenceID() + "  |  " + e.getType());
        alert.setContentText(
                "File:          " + e.getName()                       + "\n" +
                        "Path:          " + e.getFilePath()                   + "\n" +
                        "Hash:          " + storedHash                        + "\n" +
                        "Status:        " + mapStatusToDisplay(e.getStatus()) + "\n" +
                        "Linked Cases:  " + linkSummary
        );
        alert.showAndWait();
    }

    private void handleDownloadEvidence(String evidenceId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Download Evidence");
        confirm.setHeaderText("Download EV-" + evidenceId + "?");
        confirm.setContentText("This action will be logged in the audit trail.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int      id = Integer.parseInt(evidenceId);
                Evidence e  = evidenceRepo.findById(id);
                if (e == null) return;

                FileChooser save = new FileChooser();
                save.setTitle("Save Evidence File");
                
                String ext = "";
                int idx = e.getFilePath().lastIndexOf('.');
                if (idx > 0) {
                    ext = e.getFilePath().substring(idx);
                    save.getExtensionFilters().add(new FileChooser.ExtensionFilter("Evidence File (" + ext + ")", "*" + ext));
                }
                
                String initName = e.getName();
                if (!ext.isEmpty() && !initName.toLowerCase().endsWith(ext.toLowerCase())) {
                    initName += ext;
                }
                save.setInitialFileName(initName);
                
                File dest = save.showSaveDialog(getScene().getWindow());

                if (dest != null) {
                    try {
                        java.nio.file.Files.copy(
                                java.nio.file.Path.of(e.getFilePath()),
                                dest.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                        System.out.println("[EvidenceDashBoard] Downloaded to: " + dest.getAbsolutePath());
                        showAlert("Download Complete", "File saved to:\n" + dest.getAbsolutePath());
                    } catch (Exception ex) {
                        showAlert("Download Failed", "Could not save file: " + ex.getMessage());
                    }
                }
            }
        });
    }

    private void handleVerifyEvidence(String evidenceId) {
        int      id = Integer.parseInt(evidenceId);
        Evidence e  = evidenceRepo.findById(id);
        if (e == null) return;

        String  storedHash  = e.getDigitalFingerprint();
        String  currentHash = HashGenerator.generateSHA256(e.getFilePath());
        boolean valid       = storedHash != null && storedHash.equals(currentHash);

        Alert result = new Alert(valid ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        result.setTitle("Integrity Check");
        result.setHeaderText(valid ? "✅ Hash Verified" : "⚠ Hash Mismatch — Possible Tampering");
        result.setContentText(
                "Stored hash:   " + storedHash  + "\n" +
                        "Current hash:  " + currentHash
        );
        result.showAndWait();

        if (!valid) {
            e.setStatus(EvidenceStatus.DISCARDED);
            com.indicium.repository.EvidenceRepo.updateStatus(id, EvidenceStatus.DISCARDED);
            int currentUserID = com.indicium.services.SessionManager.getInstance().getCurrentUser().getUserID();
            new com.indicium.services.AuditLog().logEvent(currentUserID, "Integrity check failed. Evidence automatically discarded.", com.indicium.services.AuditCategory.EVIDENCE, currentCaseID, id);
            loadEvidence();
        }
    }

    private void handleDiscardEvidence(String evidenceId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Discard Evidence");
        confirm.setHeaderText("Discard EV-" + evidenceId + "?");
        confirm.setContentText("This will mark the evidence as discarded. This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int      id = Integer.parseInt(evidenceId);
                Evidence e  = evidenceRepo.findById(id);
                if (e == null) return;
                e.setStatus(EvidenceStatus.DISCARDED);
                com.indicium.repository.EvidenceRepo.updateStatus(id, EvidenceStatus.DISCARDED);
                int currentUserID = com.indicium.services.SessionManager.getInstance().getCurrentUser().getUserID();
                new com.indicium.services.AuditLog().logEvent(currentUserID, "Manual Discard", com.indicium.services.AuditCategory.EVIDENCE, currentCaseID, id);
                System.out.println("[EvidenceDashBoard] Evidence discarded: " + evidenceId);
                loadEvidence();
            }
        });
    }

    private void handleRestoreEvidence(String evidenceId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Evidence");
        confirm.setHeaderText("Restore EV-" + evidenceId + "?");
        confirm.setContentText("This will restore the discarded evidence back to Collected status.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int id = Integer.parseInt(evidenceId);
                Evidence e = evidenceRepo.findById(id);
                if (e == null) return;
                
                String storedHash = e.getDigitalFingerprint();
                String currentHash = HashGenerator.generateSHA256(e.getFilePath());
                if (storedHash == null || !storedHash.equals(currentHash)) {
                    showAlert("Restore Failed", "Cannot restore! File hash mismatch — this evidence has been tampered with.");
                    return;
                }
                
                e.setStatus(EvidenceStatus.COLLECTED);
                com.indicium.repository.EvidenceRepo.updateStatus(id, EvidenceStatus.COLLECTED);
                int currentUserID = com.indicium.services.SessionManager.getInstance().getCurrentUser().getUserID();
                new com.indicium.services.AuditLog().logEvent(currentUserID, "Restored Discarded Evidence", com.indicium.services.AuditCategory.EVIDENCE, currentCaseID, id);
                System.out.println("[EvidenceDashBoard] Evidence restored: " + evidenceId);
                loadEvidence();
            }
        });
    }

    // =============================================
    //  Validation
    // =============================================

    private boolean validateEvidenceForm() {
        boolean valid = true;
        hideErrors();

        if (selectedFile == null) {
            selectedFileName.setText("Please select a file.");
            selectedFileName.setStyle("-fx-text-fill: #EF5350;");
            valid = false;
        }
        if (inputSource.getText().trim().isEmpty()) {
            errorSource.setText("Source is required.");
            errorSource.setVisible(true); errorSource.setManaged(true);
            valid = false;
        }
        if (inputDateSeized.getValue() == null) {
            errorDateSeized.setText("Date seized is required.");
            errorDateSeized.setVisible(true); errorDateSeized.setManaged(true);
            valid = false;
        }
        return valid;
    }

    private void hideErrors() {
        errorSource.setVisible(false);     errorSource.setManaged(false);
        errorDateSeized.setVisible(false); errorDateSeized.setManaged(false);
        errorDuplicate.setVisible(false);  errorDuplicate.setManaged(false);
        selectedFileName.setStyle("");
    }

    // =============================================
    //  Style Helpers
    // =============================================

    private String mapStatusToDisplay(EvidenceStatus status) {
        if (status == null) return "Collected";
        return switch (status) {
            case COLLECTED -> "Collected";
            case VERIFIED  -> "Verified";
            case LINKED    -> "Linked";
            case ARCHIVED  -> "Archived";
            case DISCARDED -> "Discarded";
        };
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
            case "Verified"  -> "status-verified";
            case "Linked"    -> "status-linked";
            case "Archived"  -> "status-archived";
            case "Discarded" -> "status-discarded";
            default          -> "status-collected";
        };
    }

    private String formatSize(long bytes) {
        if (bytes <= 0)          return "N/A";
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // =============================================
    //  Utility
    // =============================================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
