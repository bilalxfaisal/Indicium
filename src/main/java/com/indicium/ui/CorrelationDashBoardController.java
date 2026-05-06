package com.indicium.ui;

import com.indicium.controllers.CorrelationManager;
import com.indicium.models.Case;
import com.indicium.models.CorrelationLink;
import com.indicium.models.Evidence;
import com.indicium.repository.CaseRepository;
import com.indicium.services.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CorrelationDashBoardController extends StackPane {

    // ── Header ──
    @FXML private Label countLinks;

    // ── Filter row ──
    @FXML private HBox             filterRow;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterCaseCombo;
    @FXML private ComboBox<String> filterTypeCombo;

    // ── Table header ──
    @FXML private HBox tableHeader;

    // ── View switcher ──
    @FXML private VBox viewLinksList;
    @FXML private VBox viewWizard;
    @FXML private VBox emptyState;

    // ── Wizard breadcrumb ──
    @FXML private Label stepOne;
    @FXML private Label stepTwo;
    @FXML private Label stepThree;

    // ── Source panel ──
    @FXML private ComboBox<Case> sourceCaseCombo;
    @FXML private VBox           sourceEvidenceList;
    @FXML private VBox           sourceEmptyHint;

    // ── Target panel ──
    @FXML private TextField targetCaseSearch;
    @FXML private VBox      targetCaseResults;
    @FXML private VBox      targetEvidenceList;
    @FXML private VBox      targetEmptyHint;

    // ── Link preview bar ──
    @FXML private HBox   linkPreviewBar;
    @FXML private Label  previewSourceName;
    @FXML private Label  previewSourceCase;
    @FXML private Label  previewTargetName;
    @FXML private Label  previewTargetCase;
    @FXML private Label  lblConflict;
    @FXML private Button btnCreateLink;

    // ── Delete modal ──
    @FXML private StackPane modalOverlay;
    @FXML private Label     modalLinkSummary;

    // ── Wizard state ──
    private enum WizardStep {
        VIEWING_LINKS, SELECT_SOURCE,
        SELECT_TARGET_CASE, SELECT_TARGET_EV, CONFIRM_LINK
    }
    private WizardStep currentStep = WizardStep.VIEWING_LINKS;

    // ── Selections ──
    private int    selectedSourceCaseID   = -1;
    private int    selectedSourceEvidID   = -1;
    private String selectedSourceEvidName = "";
    private int    selectedTargetCaseID   = -1;
    private String selectedTargetCaseName = "";
    private int    selectedTargetEvidID   = -1;
    private String selectedTargetEvidName = "";
    private int    pendingDeleteLinkID    = -1;
    private int    pendingDeleteSrcCaseID = -1;

    private CorrelationManager corrManager;
    private CaseRepository     caseRepo;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // ── Constructor ─────────────────────────────────────────────

    public CorrelationDashBoardController() {
        URL fxmlUrl = getClass().getResource(
                "/com/indicium/ui/CorrelationDashboard.fxml");
        if (fxmlUrl == null)
            throw new RuntimeException("CorrelationDashboard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load CorrelationDashboard.fxml: " + e.getMessage(), e);
        }
    }

    // ── Initialize ───────────────────────────────────────────────

    @FXML
    public void initialize() {
        corrManager = new CorrelationManager();
        caseRepo    = new CaseRepository();
        setupFilters();
        loadLinks();
        transitionTo(WizardStep.VIEWING_LINKS);
    }

    // ═══════════════════════════════════════════════════════════
    //  SETUP
    // ═══════════════════════════════════════════════════════════

    private void setupFilters() {
        // Case filter — populated from DB
        filterCaseCombo.getItems().add("All");
        filterCaseCombo.getItems().addAll(corrManager.getDistinctCaseTitles());
        filterCaseCombo.setValue("All");

        filterTypeCombo.getItems().addAll(
                "All", "Image", "Video", "Document", "Audio", "Bitstream", "Other");
        filterTypeCombo.setValue("All");

        // Source case combo — full case list
        setupSourceCaseCombo();
    }

    private void setupSourceCaseCombo() {
        sourceCaseCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Case c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) setText(null);
                else setText("#" + String.format("%04d", c.getCaseID())
                        + "  —  " + c.getTitle());
            }
        });
        sourceCaseCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Case c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText("Choose a case...");
                    setStyle("-fx-text-fill: #90A4AE;");
                } else {
                    setText("#" + String.format("%04d", c.getCaseID())
                            + "  —  " + c.getTitle());
                    setStyle("-fx-text-fill: #0D1B1E; -fx-font-weight: bold;");
                }
            }
        });
        try {
            sourceCaseCombo.getItems().setAll(caseRepo.findAll());
        } catch (Exception e) {
            System.err.println("[CorrelationDashBoard] Failed to load cases: "
                    + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  LOAD & RENDER EXISTING LINKS
    // ═══════════════════════════════════════════════════════════

    private void loadLinks() {
        String search     = searchField != null ? searchField.getText().trim() : "";
        String caseFilter = filterCaseCombo != null ? filterCaseCombo.getValue() : "All";
        String typeFilter = filterTypeCombo != null ? filterTypeCombo.getValue() : "All";

        List<CorrelationLink> links = corrManager.getLinks(search, caseFilter, typeFilter);

        viewLinksList.getChildren().clear();
        viewLinksList.getChildren().add(emptyState);

        if (links.isEmpty()) {
            showEmptyState(true);
            countLinks.setText("0 Links");
            return;
        }

        showEmptyState(false);
        countLinks.setText(links.size() + " Link" + (links.size() == 1 ? "" : "s"));

        for (CorrelationLink lnk : links) {
            buildLinkRow(
                    lnk.getLinkID(),
                    lnk.getSrcEvidName(), lnk.getSrcEvidType(),
                    "#" + String.format("%04d", lnk.getSrcCaseID())
                            + " " + lnk.getSrcCaseTitle(),
                    lnk.getTgtEvidName(), lnk.getTgtEvidType(),
                    "#" + String.format("%04d", lnk.getTgtCaseID())
                            + " " + lnk.getTgtCaseTitle(),
                    lnk.getLinkedBy(),
                    lnk.getCreatedAt() != null
                            ? lnk.getCreatedAt().format(DT_FMT) : "—",
                    lnk.getSrcCaseID()
            );
        }

        // Refresh filter combo with latest case titles
        String current = filterCaseCombo.getValue();
        filterCaseCombo.getItems().clear();
        filterCaseCombo.getItems().add("All");
        filterCaseCombo.getItems().addAll(corrManager.getDistinctCaseTitles());
        filterCaseCombo.setValue(current != null ? current : "All");
    }

    private void buildLinkRow(int linkId,
                              String srcEvName, String srcEvType, String srcCaseLabel,
                              String tgtEvName, String tgtEvType, String tgtCaseLabel,
                              String linkedBy, String date, int srcCaseID) {
        HBox row = new HBox();
        row.getStyleClass().add("link-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(0);

        // Source evidence cell
        VBox srcCell = buildEvCell(srcEvName, srcEvType, 200);

        // Source case chip
        HBox srcCaseCell = new HBox();
        srcCaseCell.setPrefWidth(160);
        srcCaseCell.setAlignment(Pos.CENTER_LEFT);
        Label srcChip = new Label(srcCaseLabel);
        srcChip.getStyleClass().add("link-case-chip");
        srcCaseCell.getChildren().add(srcChip);

        // Connector icon
        HBox connCell = new HBox();
        connCell.getStyleClass().add("link-connector-cell");
        connCell.setAlignment(Pos.CENTER);
        try {
            var stream = getClass().getResourceAsStream(
                    "/com/indicium/ui/Assets/icons8-link-100.png");
            if (stream != null) {
                ImageView iv = new ImageView(new Image(stream));
                iv.setFitWidth(18); iv.setFitHeight(18); iv.setPreserveRatio(true);
                connCell.getChildren().add(iv);
            } else {
                Label fallback = new Label("⇄");
                fallback.getStyleClass().add("link-connector-icon");
                connCell.getChildren().add(fallback);
            }
        } catch (Exception ignored) {
            Label fallback = new Label("⇄");
            fallback.getStyleClass().add("link-connector-icon");
            connCell.getChildren().add(fallback);
        }

        // Target evidence cell
        VBox tgtCell = buildEvCell(tgtEvName, tgtEvType, 200);

        // Target case chip
        HBox tgtCaseCell = new HBox();
        tgtCaseCell.setPrefWidth(160);
        tgtCaseCell.setAlignment(Pos.CENTER_LEFT);
        Label tgtChip = new Label(tgtCaseLabel);
        tgtChip.getStyleClass().add("link-case-chip");
        tgtCaseCell.getChildren().add(tgtChip);

        // Meta
        VBox metaCell = new VBox(2);
        HBox.setHgrow(metaCell, Priority.ALWAYS);
        metaCell.setAlignment(Pos.CENTER_LEFT);
        Label lblBy   = new Label(linkedBy);  lblBy.getStyleClass().add("link-meta");
        Label lblDate = new Label(date);      lblDate.getStyleClass().add("link-meta-time");
        metaCell.getChildren().addAll(lblBy, lblDate);

        // Delete button — admin only
        String role = SessionManager.getInstance().getCurrentUser()
                .getRole().toString();
        Button btnDel = new Button("Remove");
        btnDel.getStyleClass().add("btn-delete");
        btnDel.setDisable(!role.equalsIgnoreCase("ADMIN"));
        btnDel.setOnAction(e -> promptDeleteLink(linkId, srcEvName,
                tgtEvName, srcCaseID));

        row.getChildren().addAll(
                srcCell, srcCaseCell, connCell,
                tgtCell, tgtCaseCell, metaCell, btnDel);
        viewLinksList.getChildren().add(row);
    }

    private VBox buildEvCell(String name, String type, double width) {
        VBox cell = new VBox(3);
        cell.setPrefWidth(width);
        cell.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("link-ev-name");

        Label typeChip = new Label(type != null ? type.toUpperCase() : "OTHER");
        typeChip.getStyleClass().addAll("ev-type-chip", getTypeChipStyle(type));

        cell.getChildren().addAll(lblName, typeChip);
        return cell;
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    // ═══════════════════════════════════════════════════════════
    //  WIZARD STATE MACHINE
    // ═══════════════════════════════════════════════════════════

    private void transitionTo(WizardStep step) {
        currentStep = step;
        boolean isViewing = (step == WizardStep.VIEWING_LINKS);

        filterRow.setVisible(isViewing);   filterRow.setManaged(isViewing);
        tableHeader.setVisible(isViewing); tableHeader.setManaged(isViewing);
        viewLinksList.setVisible(isViewing); viewLinksList.setManaged(isViewing);
        viewWizard.setVisible(!isViewing);   viewWizard.setManaged(!isViewing);

        boolean showPreview = (step == WizardStep.CONFIRM_LINK);
        linkPreviewBar.setVisible(showPreview);
        linkPreviewBar.setManaged(showPreview);

        updateBreadcrumb(step);
    }

    private void updateBreadcrumb(WizardStep step) {
        resetChip(stepOne); resetChip(stepTwo); resetChip(stepThree);
        switch (step) {
            case SELECT_SOURCE ->
                    stepOne.getStyleClass().add("step-active");
            case SELECT_TARGET_CASE -> {
                stepOne.getStyleClass().add("step-done");
                stepTwo.getStyleClass().add("step-active");
            }
            case SELECT_TARGET_EV, CONFIRM_LINK -> {
                stepOne.getStyleClass().add("step-done");
                stepTwo.getStyleClass().add("step-done");
                stepThree.getStyleClass().add("step-active");
            }
            default -> {}
        }
    }

    private void resetChip(Label chip) {
        chip.getStyleClass().removeAll("step-active", "step-done", "step-inactive");
        chip.getStyleClass().add("step-inactive");
    }

    // ═══════════════════════════════════════════════════════════
    //  WIZARD HANDLERS
    // ═══════════════════════════════════════════════════════════

    @FXML
    private void handleNewLink() {
        resetWizardSelections();
        transitionTo(WizardStep.SELECT_SOURCE);
    }

    @FXML
    private void handleCancelWizard() {
        resetWizardSelections();
        transitionTo(WizardStep.VIEWING_LINKS);
    }

    @FXML
    private void handleSourceCaseSelected() {
        Case selected = sourceCaseCombo.getValue();
        if (selected == null) return;

        selectedSourceCaseID = selected.getCaseID();
        selectedSourceEvidID = -1;

        loadEvidencePickList(
                corrManager.getEvidenceForCase(selectedSourceCaseID),
                sourceEvidenceList,
                sourceEmptyHint,
                true   // isSource
        );
        transitionTo(WizardStep.SELECT_SOURCE);
    }

    @FXML
    private void handleTargetCaseSearch() {
        String query = targetCaseSearch.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        try {
            List<Case> results = caseRepo.findAll().stream()
                    .filter(c -> c.getCaseID() != selectedSourceCaseID)
                    .filter(c -> c.getTitle().toLowerCase().contains(query)
                            || String.valueOf(c.getCaseID()).contains(query))
                    .collect(Collectors.toList());

            targetCaseResults.getChildren().clear();

            if (results.isEmpty()) {
                Label none = new Label("No cases found.");
                none.setStyle("-fx-text-fill: #90A4AE; -fx-font-family: 'Segoe UI';"
                        + "-fx-font-size: 12px; -fx-padding: 10 16 10 16;");
                targetCaseResults.getChildren().add(none);
            } else {
                for (Case c : results) {
                    buildTargetCaseResultRow(c);
                }
            }

            targetCaseResults.setVisible(true);
            targetCaseResults.setManaged(true);

        } catch (Exception e) {
            System.err.println("[CorrelationDashBoard] Target case search failed: "
                    + e.getMessage());
        }
    }

    private void buildTargetCaseResultRow(Case c) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FAFAFA; -fx-padding: 8 16 8 16;"
                + "-fx-border-color: transparent transparent #F0F0F0 transparent;"
                + "-fx-border-width: 0 0 1 0; -fx-cursor: hand;");

        Label lblId = new Label("#" + String.format("%04d", c.getCaseID()));
        lblId.setStyle("-fx-text-fill: #90A4AE; -fx-font-family: 'Segoe UI';"
                + "-fx-font-size: 12px; -fx-font-weight: bold;");

        Label lblTitle = new Label(c.getTitle());
        lblTitle.setStyle("-fx-text-fill: #0D1B1E; -fx-font-family: 'Segoe UI';"
                + "-fx-font-size: 13px;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        // Authorize check before allowing selection
        int userID = SessionManager.getInstance().getCurrentUser().getUserID();
        boolean authorized = corrManager.initiateLink(userID, c.getCaseID());

        if (authorized) {
            Button btnSelect = new Button("Select");
            btnSelect.setStyle("-fx-background-color: #00BCD4; -fx-text-fill: #FFFFFF;"
                    + "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px;"
                    + "-fx-font-weight: bold; -fx-background-radius: 6;"
                    + "-fx-border-color: transparent; -fx-padding: 4 12 4 12;"
                    + "-fx-cursor: hand;");
            btnSelect.setOnAction(e -> selectTargetCase(c.getCaseID(), c.getTitle()));
            row.getChildren().addAll(lblId, lblTitle, btnSelect);
        } else {
            Label locked = new Label("No Access");
            locked.setStyle("-fx-text-fill: #EF5350; -fx-font-family: 'Segoe UI';"
                    + "-fx-font-size: 11px; -fx-font-weight: bold;");
            row.getChildren().addAll(lblId, lblTitle, locked);
        }

        targetCaseResults.getChildren().add(row);
    }

    private void selectTargetCase(int caseID, String title) {
        selectedTargetCaseID   = caseID;
        selectedTargetCaseName = title;
        selectedTargetEvidID   = -1;

        targetCaseResults.setVisible(false);
        targetCaseResults.setManaged(false);
        targetCaseSearch.clear();

        loadEvidencePickList(
                corrManager.getEvidenceForCase(caseID),
                targetEvidenceList,
                targetEmptyHint,
                false  // isTarget
        );
        transitionTo(WizardStep.SELECT_TARGET_EV);
    }

    // ═══════════════════════════════════════════════════════════
    //  EVIDENCE PICK LIST
    // ═══════════════════════════════════════════════════════════

    private void loadEvidencePickList(List<Evidence> evidenceList,
                                      VBox container, VBox emptyHint,
                                      boolean isSource) {
        container.getChildren().clear();
        container.getChildren().add(emptyHint);

        if (evidenceList == null || evidenceList.isEmpty()) {
            emptyHint.setVisible(true);
            emptyHint.setManaged(true);
            return;
        }

        emptyHint.setVisible(false);
        emptyHint.setManaged(false);

        for (Evidence ev : evidenceList) {
            HBox item = new HBox(10);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("evidence-pick-item");
            item.setUserData(String.valueOf(ev.getEvidenceID()));

            VBox info = new VBox(3);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label lblName = new Label(ev.getName());
            lblName.getStyleClass().add("ev-item-name");

            Label lblMeta = new Label("EV-" + ev.getEvidenceID()
                    + "  ·  " + (ev.getType() != null ? ev.getType() : "Other"));
            lblMeta.getStyleClass().add("ev-item-meta");

            info.getChildren().addAll(lblName, lblMeta);

            Label typeChip = new Label(
                    ev.getType() != null ? ev.getType().toUpperCase() : "OTHER");
            typeChip.getStyleClass().addAll("ev-type-chip",
                    getTypeChipStyle(ev.getType()));

            item.getChildren().addAll(info, typeChip);

            // Click to select
            item.setOnMouseClicked(e -> {
                if (isSource) {
                    selectSourceEvidence(ev.getEvidenceID(), ev.getName(),
                            container);
                } else {
                    selectTargetEvidence(ev.getEvidenceID(), ev.getName(),
                            container);
                }
            });

            container.getChildren().add(item);
        }
    }

    private void selectSourceEvidence(int evidID, String evidName, VBox container) {
        selectedSourceEvidID   = evidID;
        selectedSourceEvidName = evidName;
        highlightSelectedItem(container, evidID);
        checkPreviewReady();
    }

    private void selectTargetEvidence(int evidID, String evidName, VBox container) {
        selectedTargetEvidID   = evidID;
        selectedTargetEvidName = evidName;
        highlightSelectedItem(container, evidID);
        checkPreviewReady();
    }

    private void highlightSelectedItem(VBox container, int evidID) {
        container.getChildren().forEach(node -> {
            if (node instanceof HBox item) {
                item.getStyleClass().removeAll("evidence-pick-item-selected");
                if (String.valueOf(evidID).equals(item.getUserData())) {
                    item.getStyleClass().add("evidence-pick-item-selected");
                }
            }
        });
    }

    private void checkPreviewReady() {
        if (selectedSourceEvidID == -1 || selectedTargetEvidID == -1) return;

        previewSourceName.setText(selectedSourceEvidName);
        previewSourceCase.setText("Case #" + String.format("%04d", selectedSourceCaseID));
        previewTargetName.setText(selectedTargetEvidName);
        previewTargetCase.setText("Case #" + String.format("%04d", selectedTargetCaseID));

        boolean conflict = corrManager.linkAlreadyExists(
                selectedSourceEvidID, selectedTargetEvidID);
        lblConflict.setVisible(conflict);
        lblConflict.setManaged(conflict);
        btnCreateLink.setDisable(conflict);

        transitionTo(WizardStep.CONFIRM_LINK);
    }

    @FXML
    private void handleCreateLink() {
        if (selectedSourceEvidID == -1 || selectedTargetEvidID == -1) return;

        int userID = SessionManager.getInstance().getCurrentUser().getUserID();

        boolean success = corrManager.createCrossCaseLink(
                userID,
                selectedSourceEvidID, selectedSourceCaseID,
                selectedTargetEvidID, selectedTargetCaseID
        );

        if (success) {
            resetWizardSelections();
            transitionTo(WizardStep.VIEWING_LINKS);
            loadLinks();
        } else {
            lblConflict.setText("Failed to create link. It may already exist or access was denied.");
            lblConflict.setVisible(true);
            lblConflict.setManaged(true);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE MODAL
    // ═══════════════════════════════════════════════════════════

    private void promptDeleteLink(int linkID, String srcName,
                                  String tgtName, int srcCaseID) {
        pendingDeleteLinkID    = linkID;
        pendingDeleteSrcCaseID = srcCaseID;
        modalLinkSummary.setText(srcName + "  ⇄  " + tgtName);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    @FXML
    private void handleConfirmDelete() {
        if (pendingDeleteLinkID == -1) return;

        int userID = SessionManager.getInstance().getCurrentUser().getUserID();
        boolean deleted = corrManager.removeLink(
                userID, pendingDeleteLinkID, pendingDeleteSrcCaseID);

        if (deleted) {
            handleCloseModal();
            loadLinks();
        } else {
            showAlert("Delete Failed", "Could not remove the link.");
        }
    }

    @FXML
    private void handleCloseModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        pendingDeleteLinkID    = -1;
        pendingDeleteSrcCaseID = -1;
    }

    // ═══════════════════════════════════════════════════════════
    //  FILTER HANDLERS
    // ═══════════════════════════════════════════════════════════

    @FXML private void handleSearch() { loadLinks(); }
    @FXML private void handleFilter() { loadLinks(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterCaseCombo.setValue("All");
        filterTypeCombo.setValue("All");
        loadLinks();
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════

    private void resetWizardSelections() {
        selectedSourceCaseID   = -1;
        selectedSourceEvidID   = -1;
        selectedSourceEvidName = "";
        selectedTargetCaseID   = -1;
        selectedTargetCaseName = "";
        selectedTargetEvidID   = -1;
        selectedTargetEvidName = "";
        sourceCaseCombo.setValue(null);
        sourceEvidenceList.getChildren().clear();
        sourceEvidenceList.getChildren().add(sourceEmptyHint);
        targetEvidenceList.getChildren().clear();
        targetEvidenceList.getChildren().add(targetEmptyHint);
        targetCaseResults.getChildren().clear();
        targetCaseResults.setVisible(false);
        targetCaseResults.setManaged(false);
        targetCaseSearch.clear();
    }

    private String getTypeChipStyle(String type) {
        if (type == null) return "ev-type-other";
        return switch (type.toLowerCase()) {
            case "image"     -> "ev-type-image";
            case "video"     -> "ev-type-video";
            case "document"  -> "ev-type-document";
            case "audio"     -> "ev-type-audio";
            case "bitstream" -> "ev-type-bitstream";
            default          -> "ev-type-other";
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
