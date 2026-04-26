package com.indicium.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;

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
    @FXML private ComboBox<String> sourceCaseCombo;
    @FXML private VBox             sourceEvidenceList;
    @FXML private VBox             sourceEmptyHint;

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
    @FXML private Button    btnConfirmDelete;

    // ── Wizard state ──
    private enum WizardStep {
        VIEWING_LINKS,
        SELECT_SOURCE,
        SELECT_TARGET_CASE,
        SELECT_TARGET_EV,
        CONFIRM_LINK
    }

    private WizardStep currentStep = WizardStep.VIEWING_LINKS;

    // ── Selections ──
    private String selectedSourceCaseId   = null;
    private String selectedSourceEvidId   = null;
    private String selectedSourceEvidName = null;

    private String selectedTargetCaseId   = null;
    private String selectedTargetCaseName = null;
    private String selectedTargetEvidId   = null;
    private String selectedTargetEvidName = null;

    // ── Pending delete ──
    private String pendingDeleteLinkId = null;

    // ── Constructor ──
    public CorrelationDashBoardController() {
        URL fxmlUrl = getClass().getResource(
                "/com/indicium/ui/CorrelationDashboard.fxml"
        );
        if (fxmlUrl == null)
            throw new RuntimeException("CorrelationDashboard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load CorrelationDashboard.fxml: " + e.getMessage(), e
            );
        }
    }

    @FXML
    public void initialize() {
        setupFilters();
        loadLinks();
        transitionTo(WizardStep.VIEWING_LINKS);
    }

    // ══════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════

    private void setupFilters() {
        // TODO: populate filterCaseCombo from DB — SELECT DISTINCT case_id FROM cases
        filterTypeCombo.getItems().addAll(
                "All", "Image", "Video", "Document", "Audio", "Other"
        );
        filterTypeCombo.setValue("All");
    }

    // ══════════════════════════════════════════════════════════
    //  LOAD EXISTING LINKS
    // ══════════════════════════════════════════════════════════

    private void loadLinks() {
        viewLinksList.getChildren().clear();
        viewLinksList.getChildren().add(emptyState);

        // TODO: SELECT cl.link_id,
        //              src_ev.name, src_ev.type, src_c.case_id, src_c.title,
        //              tgt_ev.name, tgt_ev.type, tgt_c.case_id, tgt_c.title,
        //              u.username, cl.created_at
        //       FROM correlation_links cl
        //       JOIN evidence src_ev ON cl.source_ev_id = src_ev.id
        //       JOIN cases    src_c  ON src_ev.case_id  = src_c.id
        //       JOIN evidence tgt_ev ON cl.target_ev_id = tgt_ev.id
        //       JOIN cases    tgt_c  ON tgt_ev.case_id  = tgt_c.id
        //       JOIN users    u      ON cl.created_by   = u.id
        //       ORDER BY cl.created_at DESC;
        // TODO: For each row call buildLinkRow(...)
        // TODO: Update countLinks label

        showEmptyState(true); // remove once DB wired
    }

    /**
     * Builds one link row in the existing-links list.
     */
    private void buildLinkRow(String linkId,
                              String srcEvName, String srcEvType, String srcCaseId,
                              String tgtEvName, String tgtEvType, String tgtCaseId,
                              String linkedBy, String date) {
        HBox row = new HBox();
        row.getStyleClass().add("link-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(0);

        // Source evidence cell
        VBox srcCell = buildEvCell(srcEvName, srcEvType, srcCaseId, 200);

        // Source case chip
        HBox srcCaseCell = new HBox();
        srcCaseCell.setPrefWidth(160);
        srcCaseCell.setAlignment(Pos.CENTER_LEFT);
        Label srcChip = new Label(srcCaseId);
        srcChip.getStyleClass().add("link-case-chip");
        srcCaseCell.getChildren().add(srcChip);

        // Connector
        HBox connCell = new HBox();
        connCell.getStyleClass().add("link-connector-cell");
        connCell.setAlignment(Pos.CENTER);
        Label connIcon = new Label("⇄");
        connIcon.getStyleClass().add("link-connector-icon");
        connCell.getChildren().add(connIcon);

        // Target evidence cell
        VBox tgtCell = buildEvCell(tgtEvName, tgtEvType, tgtCaseId, 200);

        // Target case chip
        HBox tgtCaseCell = new HBox();
        tgtCaseCell.setPrefWidth(160);
        tgtCaseCell.setAlignment(Pos.CENTER_LEFT);
        Label tgtChip = new Label(tgtCaseId);
        tgtChip.getStyleClass().add("link-case-chip");
        tgtCaseCell.getChildren().add(tgtChip);

        // Meta + delete
        VBox metaCell = new VBox(2);
        HBox.setHgrow(metaCell, Priority.ALWAYS);
        metaCell.setAlignment(Pos.CENTER_LEFT);
        Label lblBy   = new Label(linkedBy);
        lblBy.getStyleClass().add("link-meta");
        Label lblDate = new Label(date);
        lblDate.getStyleClass().add("link-meta-time");
        metaCell.getChildren().addAll(lblBy, lblDate);

        // Delete button — TODO: hide if user role != ADMIN
        Button btnDel = new Button("Remove");
        btnDel.getStyleClass().add("btn-delete");
        btnDel.setOnAction(e -> promptDeleteLink(linkId, srcEvName, tgtEvName));

        row.getChildren().addAll(
                srcCell, srcCaseCell, connCell,
                tgtCell, tgtCaseCell, metaCell, btnDel
        );
        viewLinksList.getChildren().add(row);
    }

    private VBox buildEvCell(String name, String type, String caseId, double width) {
        VBox cell = new VBox(3);
        cell.setPrefWidth(width);
        cell.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("link-ev-name");

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label typeChip = new Label(type.toUpperCase());
        typeChip.getStyleClass().addAll("ev-type-chip", getTypeChipStyle(type));
        meta.getChildren().add(typeChip);

        cell.getChildren().addAll(lblName, meta);
        return cell;
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    // ══════════════════════════════════════════════════════════
    //  WIZARD STATE MACHINE
    // ══════════════════════════════════════════════════════════

    private void transitionTo(WizardStep step) {
        currentStep = step;

        boolean isViewing = (step == WizardStep.VIEWING_LINKS);

        // Show/hide top chrome
        filterRow.setVisible(isViewing);
        filterRow.setManaged(isViewing);
        tableHeader.setVisible(isViewing);
        tableHeader.setManaged(isViewing);

        // Show/hide main panels
        viewLinksList.setVisible(isViewing);
        viewLinksList.setManaged(isViewing);
        viewWizard.setVisible(!isViewing);
        viewWizard.setManaged(!isViewing);

        // Preview bar only when confirm step
        boolean showPreview = (step == WizardStep.CONFIRM_LINK);
        linkPreviewBar.setVisible(showPreview);
        linkPreviewBar.setManaged(showPreview);

        // Update breadcrumb chips
        updateBreadcrumb(step);
    }

    private void updateBreadcrumb(WizardStep step) {
        resetChip(stepOne);
        resetChip(stepTwo);
        resetChip(stepThree);

        switch (step) {
            case SELECT_SOURCE -> {
                stepOne.getStyleClass().add("step-active");
            }
            case SELECT_TARGET_CASE -> {
                stepOne.getStyleClass().add("step-done");
                stepTwo.getStyleClass().add("step-active");
            }
            case SELECT_TARGET_EV, CONFIRM_LINK -> {
                stepOne.getStyleClass().add("step-done");
                stepTwo.getStyleClass().add("step-done");
                stepThree.getStyleClass().add("step-active");
            }
            default -> { /* VIEWING_LINKS — no chips active */ }
        }
    }

    private void resetChip(Label chip) {
        chip.getStyleClass().removeAll("step-active", "step-done", "step-inactive");
        chip.getStyleClass().add("step-inactive");
    }

    // ══════════════════════════════════════════════════════════
    //  WIZARD HANDLERS
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleNewLink() {
        resetWizardSelections();
        populateSourceCaseCombo();
        transitionTo(WizardStep.SELECT_SOURCE);
    }

    @FXML
    private void handleCancelWizard() {
        resetWizardSelections();
        transitionTo(WizardStep.VIEWING_LINKS);
    }

    @FXML
    private void handleSourceCaseSelected() {
        String caseId = sourceCaseCombo.getValue();
        if (caseId == null) return;

        selectedSourceCaseId = caseId;
        selectedSourceEvidId = null;
        loadSourceEvidenceList(caseId);
        transitionTo(WizardStep.SELECT_SOURCE);
    }

    @FXML
    private void handleTargetCaseSearch() {
        String query = targetCaseSearch.getText().trim();
        if (query.isEmpty()) return;

        // TODO: SELECT case_id, title FROM cases
        //       WHERE case_id LIKE ? OR title LIKE ?
        //       AND case_id != selectedSourceCaseId   ← exclude source case
        //       LIMIT 10
        // TODO: For each result call buildTargetCaseResult(caseId, title)
        targetCaseResults.getChildren().clear();
        targetCaseResults.setVisible(true);
        targetCaseResults.setManaged(true);
    }

    private void selectTargetCase(String caseId, String title) {
        selectedTargetCaseId   = caseId;
        selectedTargetCaseName = title;
        selectedTargetEvidId   = null;

        // TODO: Trigger UC-4 Authorize Case Access check here
        //   authorizeCaseAccess(currentUserId, caseId);

        loadTargetEvidenceList(caseId);
        transitionTo(WizardStep.SELECT_TARGET_EV);
    }

    private void selectSourceEvidence(String evidId, String evidName) {
        selectedSourceEvidId   = evidId;
        selectedSourceEvidName = evidName;
        refreshEvidenceSelectionUI(sourceEvidenceList, evidId);
        checkPreviewReady();
    }

    private void selectTargetEvidence(String evidId, String evidName) {
        selectedTargetEvidId   = evidId;
        selectedTargetEvidName = evidName;
        refreshEvidenceSelectionUI(targetEvidenceList, evidId);
        checkPreviewReady();
    }

    /**
     * Called whenever a selection changes — shows preview bar
     * and enables Create Link if both sides are chosen.
     */
    private void checkPreviewReady() {
        boolean ready = selectedSourceEvidId != null && selectedTargetEvidId != null;

        if (ready) {
            previewSourceName.setText(selectedSourceEvidName);
            previewSourceCase.setText(selectedSourceCaseId);
            previewTargetName.setText(selectedTargetEvidName);
            previewTargetCase.setText(selectedTargetCaseId);

            boolean conflict = checkLinkConflict(
                    selectedSourceEvidId, selectedTargetEvidId
            );
            lblConflict.setVisible(conflict);
            lblConflict.setManaged(conflict);
            btnCreateLink.setDisable(conflict);

            transitionTo(WizardStep.CONFIRM_LINK);
        }
    }

    @FXML
    private void handleCreateLink() {
        if (selectedSourceEvidId == null || selectedTargetEvidId == null) return;

        // TODO: INSERT INTO correlation_links
        //         (source_ev_id, target_ev_id, created_by, created_at)
        //       VALUES (?, ?, ?, NOW())
        // TODO: Log to audit_log:
        //         action=LINK_CREATE, type=Evidence,
        //         source=selectedSourceEvidId, target=selectedTargetEvidId
        // TODO: Refresh countLinks badge

        resetWizardSelections();
        transitionTo(WizardStep.VIEWING_LINKS);
        loadLinks();
    }

    // ══════════════════════════════════════════════════════════
    //  DELETE MODAL
    // ══════════════════════════════════════════════════════════

    private void promptDeleteLink(String linkId, String srcName, String tgtName) {
        pendingDeleteLinkId = linkId;
        modalLinkSummary.setText(srcName + "  ⇄  " + tgtName);
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    @FXML
    private void handleConfirmDelete() {
        if (pendingDeleteLinkId == null) return;

        // TODO: DELETE FROM correlation_links WHERE link_id = ?
        // TODO: Log to audit_log: action=LINK_REMOVE
        // TODO: Refresh list

        pendingDeleteLinkId = null;
        handleCloseModal();
        loadLinks();
    }

    @FXML
    private void handleCloseModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        pendingDeleteLinkId = null;
    }

    // ══════════════════════════════════════════════════════════
    //  FILTER HANDLERS
    // ══════════════════════════════════════════════════════════

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterCaseCombo.setValue(null);
        filterTypeCombo.setValue("All");
        applyFilters();
    }

    private void applyFilters() {
        // TODO: Re-query DB with active filter values
        loadLinks();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS — Evidence list builders
    // ══════════════════════════════════════════════════════════

    private void populateSourceCaseCombo() {
        sourceCaseCombo.getItems().clear();
        // TODO: SELECT case_id FROM cases WHERE status = 'ACTIVE'
        //       ORDER BY created_at DESC
    }

    private void loadSourceEvidenceList(String caseId) {
        sourceEvidenceList.getChildren().clear();
        sourceEmptyHint.setVisible(false);
        sourceEmptyHint.setManaged(false);

        // TODO: SELECT ev_id, name, file_type, date_seized
        //       FROM evidence WHERE case_id = ? ORDER BY date_seized DESC
        // TODO: For each row call buildEvidencePickItem(
        //           sourceEvidenceList, evId, name, type, date, true)
    }

    private void loadTargetEvidenceList(String caseId) {
        targetEvidenceList.getChildren().clear();
        targetEmptyHint.setVisible(false);
        targetEmptyHint.setManaged(false);

        // TODO: Same query as loadSourceEvidenceList but for target case
        // TODO: For each row call buildEvidencePickItem(
        //           targetEvidenceList, evId, name, type, date, false)
    }

    /**
     * Builds a clickable evidence item row for the pick lists.
     *
     * @param container  sourceEvidenceList or targetEvidenceList
     * @param evId       evidence primary key
     * @param name       file name
     * @param type       "Image" | "Video" | "Document" | "Audio" | "Other"
     * @param date       date seized string
     * @param isSource   true = source panel, false = target panel
     */
    private void buildEvidencePickItem(VBox container, String evId,
                                       String name, String type,
                                       String date, boolean isSource) {
        HBox item = new HBox(10);
        item.getStyleClass().add("evidence-pick-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setUserData(evId);

        // File type chip
        Label typeChip = new Label(type.toUpperCase());
        typeChip.getStyleClass().addAll("ev-type-chip", getTypeChipStyle(type));

        // Name + date
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblName = new Label(name);
        lblName.getStyleClass().add("ev-item-name");
        Label lblDate = new Label(date);
        lblDate.getStyleClass().add("ev-item-meta");
        info.getChildren().addAll(lblName, lblDate);

        // Checkmark (hidden until selected)
        Label check = new Label("✓");
        check.getStyleClass().add("ev-selected-check");
        check.setVisible(false);
        check.setManaged(false);

        item.getChildren().addAll(typeChip, info, check);

        item.setOnMouseClicked(e -> {
            if (isSource) {
                selectSourceEvidence(evId, name);
            } else {
                selectTargetEvidence(evId, name);
            }
        });

        container.getChildren().add(item);
    }

    /**
     * Builds a target case search result row.
     */
    private void buildTargetCaseResult(String caseId, String title) {
        VBox item = new VBox(2);
        item.getStyleClass().add("case-result-item");
        item.setUserData(caseId);

        Label lblId    = new Label(caseId);
        lblId.getStyleClass().add("case-result-id");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("case-result-title");
        item.getChildren().addAll(lblId, lblTitle);

        item.setOnMouseClicked(e -> {
            selectTargetCase(caseId, title);
            targetCaseResults.setVisible(false);
            targetCaseResults.setManaged(false);
        });

        targetCaseResults.getChildren().add(item);
    }

    /**
     * Refreshes selected/unselected visual state on all items in a list.
     */
    private void refreshEvidenceSelectionUI(VBox list, String selectedId) {
        list.getChildren().forEach(node -> {
            if (node instanceof HBox item) {
                boolean isSelected = selectedId.equals(item.getUserData());
                item.getStyleClass().removeAll(
                        "evidence-pick-item-selected", "evidence-pick-item"
                );
                item.getStyleClass().add(
                        isSelected ? "evidence-pick-item-selected" : "evidence-pick-item"
                );
                // Toggle checkmark visibility
                item.getChildren().stream()
                        .filter(c -> c instanceof Label &&
                                ((Label) c).getStyleClass().contains("ev-selected-check"))
                        .forEach(c -> {
                            c.setVisible(isSelected);
                            c.setManaged(isSelected);
                        });
            }
        });
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS — Misc
    // ══════════════════════════════════════════════════════════

    private boolean checkLinkConflict(String srcEvId, String tgtEvId) {
        // TODO: SELECT COUNT(*) FROM correlation_links
        //       WHERE (source_ev_id = ? AND target_ev_id = ?)
        //          OR (source_ev_id = ? AND target_ev_id = ?)
        return false; // placeholder
    }

    private void resetWizardSelections() {
        selectedSourceCaseId   = null;
        selectedSourceEvidId   = null;
        selectedSourceEvidName = null;
        selectedTargetCaseId   = null;
        selectedTargetCaseName = null;
        selectedTargetEvidId   = null;
        selectedTargetEvidName = null;

        sourceCaseCombo.setValue(null);
        targetCaseSearch.clear();
        sourceEvidenceList.getChildren().clear();
        targetEvidenceList.getChildren().clear();
        targetCaseResults.getChildren().clear();

        sourceEmptyHint.setVisible(true);
        sourceEmptyHint.setManaged(true);
        targetEmptyHint.setVisible(true);
        targetEmptyHint.setManaged(true);
        targetCaseResults.setVisible(false);
        targetCaseResults.setManaged(false);

        lblConflict.setVisible(false);
        lblConflict.setManaged(false);
        btnCreateLink.setDisable(true);
    }

    private String getTypeChipStyle(String type) {
        return switch (type.toLowerCase()) {
            case "image"    -> "ev-type-image";
            case "video"    -> "ev-type-video";
            case "document" -> "ev-type-document";
            case "audio"    -> "ev-type-audio";
            default         -> "ev-type-other";
        };
    }
}
