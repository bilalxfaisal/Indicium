package com.indicium.ui;

import com.indicium.controllers.ReportManager;
import com.indicium.models.Report;
import com.indicium.repository.CaseRepository;
import com.indicium.services.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ReportGenController extends StackPane {

    // ── Header ──
    @FXML private Button btnExportPDF;
    @FXML private Button btnExportCSV;

    // ── Section 1 — Report type ──
    @FXML private VBox cardCaseSummary;
    @FXML private VBox cardAuditLog;

    // ── Section 2 — Case selection ──
    @FXML private VBox             sectionCaseSelect;
    @FXML private TextField        caseSearchField;
    @FXML private ComboBox<String> caseStatusFilter;
    @FXML private VBox             caseResultsList;
    @FXML private HBox             selectedCaseChip;
    @FXML private Label            chipCaseId;
    @FXML private Label            chipCaseTitle;

    // ── Section 3 — Parameters ──
    @FXML private TextField        dateFrom;
    @FXML private TextField        dateTo;
    @FXML private ComboBox<String> outputFormat;

    // ── Section 4 — Include toggles ──
    @FXML private VBox  toggleInvestigators;
    @FXML private VBox  togglePhotoEvidence;
    @FXML private VBox  toggleTimeline;
    @FXML private VBox  toggleAuditTrail;
    @FXML private Label checkInvestigators;
    @FXML private Label checkPhotoEvidence;
    @FXML private Label checkTimeline;
    @FXML private Label checkAuditTrail;

    // ── Section 5 — Notes ──
    @FXML private TextArea reportNotes;

    // ── Preview bar ──
    @FXML private VBox   previewBar;
    @FXML private Label  previewReportType;
    @FXML private Label  previewCaseId;
    @FXML private Label  previewDateRange;
    @FXML private Label  previewFormat;
    @FXML private Label  previewSections;
    @FXML private Label  lblValidationError;
    @FXML private Button btnGenerateReport;

    // ── Generating overlay ──
    @FXML private StackPane generatingOverlay;
    @FXML private Label     lblProgress;
    @FXML private Label     lblProgressStep;
    @FXML private StackPane progressFill;

    // ── State ──
    private String  selectedReportType  = "CASE_SUMMARY";
    private String  selectedCaseId      = null;
    private String  selectedCaseName    = null;

    private boolean includeInvestigators = true;
    private boolean includePhotoEvidence = true;
    private boolean includeTimeline      = true;
    private boolean includeAuditTrail    = false;

    // ── Services ──
    private final ReportManager  reportManager = new ReportManager();
    private final CaseRepository caseRepo      = new CaseRepository();

    // ── Last generated report ──
    private Report lastGeneratedReport = null;

    // ══════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════

    public ReportGenController() {
        URL fxmlUrl = getClass().getResource(
                "/com/indicium/ui/ReportGenerationDashBoard.fxml"
        );
        if (fxmlUrl == null)
            throw new RuntimeException("ReportGenerationDashBoard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load ReportGenerationDashBoard.fxml: " + e.getMessage(), e
            );
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        setupCombos();
        setupFieldListeners();
        // ✅ Show the preview bar immediately — it's always visible
        previewBar.setVisible(true);
        previewBar.setManaged(true);
        // ✅ Sync initial toggle styles with initial boolean states
        updateToggleStyle(toggleInvestigators, checkInvestigators, includeInvestigators);
        updateToggleStyle(togglePhotoEvidence,  checkPhotoEvidence,  includePhotoEvidence);
        updateToggleStyle(toggleTimeline,       checkTimeline,       includeTimeline);
        updateToggleStyle(toggleAuditTrail,     checkAuditTrail,     includeAuditTrail);
        refreshPreviewBar();
    }

    // ══════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════

    private void setupCombos() {
        caseStatusFilter.getItems().addAll("All", "Active", "Archived", "Locked");
        caseStatusFilter.setValue("All");
        outputFormat.getItems().addAll("PDF", "CSV");
        outputFormat.setValue("PDF");
        outputFormat.setOnAction(e -> refreshPreviewBar());
    }

    private void setupFieldListeners() {
        dateFrom.textProperty().addListener((o, ov, nv) -> refreshPreviewBar());
        dateTo.textProperty().addListener((o, ov, nv)   -> refreshPreviewBar());
    }

    // ══════════════════════════════════════════════════════════
    //  Toggle style helper — uses CSS class names from the CSS file
    //  CSS has: .toggle-on / .toggle-off  (NOT toggle-card-on/off)
    // ══════════════════════════════════════════════════════════

    private void updateToggleStyle(VBox card, Label check, boolean active) {
        if (active) {
            card.getStyleClass().remove("toggle-off");
            if (!card.getStyleClass().contains("toggle-on"))
                card.getStyleClass().add("toggle-on");
            check.getStyleClass().remove("toggle-check-off");
            if (!check.getStyleClass().contains("toggle-check"))
                check.getStyleClass().add("toggle-check");
            check.setText("✓");
        } else {
            card.getStyleClass().remove("toggle-on");
            if (!card.getStyleClass().contains("toggle-off"))
                card.getStyleClass().add("toggle-off");
            check.getStyleClass().remove("toggle-check");
            if (!check.getStyleClass().contains("toggle-check-off"))
                check.getStyleClass().add("toggle-check-off");
            check.setText("✕");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 1 — Report Type
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleSelectReportType(javafx.scene.input.MouseEvent e) {
        VBox clicked = (VBox) e.getSource();
        selectedReportType = (String) clicked.getUserData();

        cardCaseSummary.getStyleClass().remove("report-type-active");
        cardAuditLog.getStyleClass().remove("report-type-active");
        clicked.getStyleClass().add("report-type-active");

        boolean needsCase = selectedReportType.equals("CASE_SUMMARY");
        sectionCaseSelect.setVisible(needsCase);
        sectionCaseSelect.setManaged(needsCase);

        if (!needsCase) clearCaseSelection();

        refreshPreviewBar();
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 2 — Case Search
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleCaseSearch() {
        String query  = caseSearchField.getText().trim().toLowerCase();
        String status = caseStatusFilter.getValue();

        caseResultsList.getChildren().clear();

        if (query.isEmpty()) {
            caseResultsList.setVisible(false);
            caseResultsList.setManaged(false);
            return;
        }

        List<com.indicium.models.Case> results = caseRepo.searchCases(query, status);

        if (results.isEmpty()) {
            Label noResult = new Label("No cases found.");
            noResult.getStyleClass().add("case-result-empty");
            caseResultsList.getChildren().add(noResult);
        } else {
            for (com.indicium.models.Case c : results) {
                buildCaseResultItem(
                        String.valueOf(c.getCaseID()),
                        c.getTitle(),
                        c.getStatus().name()
                );
            }
        }

        caseResultsList.setVisible(true);
        caseResultsList.setManaged(true);
    }

    private void buildCaseResultItem(String caseId, String title, String status) {
        VBox item = new VBox(2);
        item.getStyleClass().add("case-result-item");

        Label lblId     = new Label("Case #" + caseId);
        lblId.getStyleClass().add("case-result-id");

        Label lblTitle  = new Label(title);
        lblTitle.getStyleClass().add("case-result-title");

        Label lblStatus = new Label(status);
        lblStatus.getStyleClass().add("case-result-id"); // reuse muted style

        item.getChildren().addAll(lblId, lblTitle, lblStatus);
        item.setOnMouseClicked(e -> selectCase(caseId, title));
        caseResultsList.getChildren().add(item);
    }

    private void selectCase(String caseId, String title) {
        selectedCaseId   = caseId;
        selectedCaseName = title;

        chipCaseId.setText("Case #" + caseId);
        chipCaseTitle.setText(title);

        selectedCaseChip.setVisible(true);
        selectedCaseChip.setManaged(true);

        caseResultsList.setVisible(false);
        caseResultsList.setManaged(false);
        caseSearchField.clear();

        setValidationError("");
        refreshPreviewBar();
    }

    @FXML
    private void handleClearCase() {
        clearCaseSelection();
        refreshPreviewBar();
    }

    private void clearCaseSelection() {
        selectedCaseId   = null;
        selectedCaseName = null;
        chipCaseId.setText("—");
        chipCaseTitle.setText("—");
        selectedCaseChip.setVisible(false);
        selectedCaseChip.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 4 — Toggle Sections
    //  Called from FXML onMouseClicked="#handleToggleSection"
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleToggleSection(javafx.scene.input.MouseEvent e) {
        VBox clicked = (VBox) e.getSource();

        if (clicked == toggleInvestigators) {
            includeInvestigators = !includeInvestigators;
            updateToggleStyle(toggleInvestigators, checkInvestigators, includeInvestigators);
        } else if (clicked == togglePhotoEvidence) {
            includePhotoEvidence = !includePhotoEvidence;
            updateToggleStyle(togglePhotoEvidence, checkPhotoEvidence, includePhotoEvidence);
        } else if (clicked == toggleTimeline) {
            includeTimeline = !includeTimeline;
            updateToggleStyle(toggleTimeline, checkTimeline, includeTimeline);
        } else if (clicked == toggleAuditTrail) {
            includeAuditTrail = !includeAuditTrail;
            updateToggleStyle(toggleAuditTrail, checkAuditTrail, includeAuditTrail);
        }

        refreshPreviewBar();
    }

    // ══════════════════════════════════════════════════════════
    //  PREVIEW BAR
    // ══════════════════════════════════════════════════════════

    private void refreshPreviewBar() {
        previewReportType.setText(selectedReportType.replace("_", " "));
        previewCaseId.setText(selectedCaseId != null ? "Case #" + selectedCaseId : "—");

        String from = dateFrom.getText().trim();
        String to   = dateTo.getText().trim();
        previewDateRange.setText(
                (!from.isEmpty() || !to.isEmpty())
                        ? (from.isEmpty() ? "—" : from) + " → " + (to.isEmpty() ? "—" : to)
                        : "All dates"
        );

        previewFormat.setText(outputFormat.getValue() != null ? outputFormat.getValue() : "PDF");

        List<String> sections = new ArrayList<>();
        if (includeInvestigators) sections.add("Investigators");
        if (includePhotoEvidence) sections.add("Evidence");
        if (includeTimeline)      sections.add("Timeline");
        if (includeAuditTrail)    sections.add("Audit");
        previewSections.setText(sections.isEmpty() ? "None" : String.join(", ", sections));
    }

    // ══════════════════════════════════════════════════════════
    //  GENERATE REPORT
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleGenerateReport() {
        setValidationError("");

        if (selectedReportType.equals("CASE_SUMMARY") && selectedCaseId == null) {
            setValidationError("⚠  Please select a case before generating.");
            return;
        }

        int    currentUserID = SessionManager.getInstance().getCurrentUser().getUserID();
        int    caseID        = selectedReportType.equals("CASE_SUMMARY")
                ? Integer.parseInt(selectedCaseId) : 0;
        String format        = outputFormat.getValue() != null ? outputFormat.getValue() : "PDF";

        showGeneratingOverlay(true);
        animateProgress();

        new Thread(() -> {
            Report report = reportManager.generateReport(
                    caseID, currentUserID, selectedReportType, format
            );
            Platform.runLater(() -> {
                showGeneratingOverlay(false);
                if (report == null) {
                    setValidationError("⚠  Report generation failed. Check permissions or case ID.");
                    return;
                }
                lastGeneratedReport = report;
                btnExportPDF.setDisable(false);
                btnExportCSV.setDisable(false);
                showSuccessAlert("Report generated successfully! Use Export to save.");
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════
    //  RESET FORM
    //  Called from FXML onAction="#handleResetForm"
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleResetForm() {
        // Report type
        selectedReportType = "CASE_SUMMARY";
        cardCaseSummary.getStyleClass().add("report-type-active");
        cardAuditLog.getStyleClass().remove("report-type-active");
        sectionCaseSelect.setVisible(true);
        sectionCaseSelect.setManaged(true);

        // Case
        clearCaseSelection();
        caseSearchField.clear();
        caseStatusFilter.setValue("All");
        caseResultsList.getChildren().clear();
        caseResultsList.setVisible(false);
        caseResultsList.setManaged(false);

        // Parameters
        dateFrom.clear();
        dateTo.clear();
        outputFormat.setValue("PDF");

        // Toggles — reset to defaults
        includeInvestigators = true;
        includePhotoEvidence = true;
        includeTimeline      = true;
        includeAuditTrail    = false;
        updateToggleStyle(toggleInvestigators, checkInvestigators, true);
        updateToggleStyle(togglePhotoEvidence,  checkPhotoEvidence,  true);
        updateToggleStyle(toggleTimeline,       checkTimeline,       true);
        updateToggleStyle(toggleAuditTrail,     checkAuditTrail,     false);

        // Notes
        reportNotes.clear();

        // State
        lastGeneratedReport = null;
        btnExportPDF.setDisable(true);
        btnExportCSV.setDisable(true);
        setValidationError("");

        refreshPreviewBar();
    }

    // ══════════════════════════════════════════════════════════
    //  EXPORT PDF
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleExportPDF() {
        if (lastGeneratedReport == null) {
            setValidationError("⚠  Generate a report first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save PDF Report");
        chooser.setInitialFileName(
                "Report_Case" + lastGeneratedReport.getCaseID()
                        + "_" + lastGeneratedReport.getReportID() + ".pdf"
        );
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        boolean saved = reportManager.exportToPDF(lastGeneratedReport, file.getAbsolutePath());
        if (saved) showSuccessAlert("PDF saved to:\n" + file.getAbsolutePath());
        else       setValidationError("⚠  PDF export failed. Check console for details.");
    }

    // ══════════════════════════════════════════════════════════
    //  EXPORT CSV
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleExportCSV() {
        if (lastGeneratedReport == null) {
            setValidationError("⚠  Generate a report first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save CSV Report");
        chooser.setInitialFileName(
                "Report_Case" + lastGeneratedReport.getCaseID()
                        + "_" + lastGeneratedReport.getReportID() + ".csv"
        );
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return;

        boolean saved = reportManager.exportToCSV(lastGeneratedReport, file.getAbsolutePath());
        if (saved) showSuccessAlert("CSV saved to:\n" + file.getAbsolutePath());
        else       setValidationError("⚠  CSV export failed. Check console for details.");
    }

    // ══════════════════════════════════════════════════════════
    //  OVERLAY + PROGRESS
    // ══════════════════════════════════════════════════════════

    private void showGeneratingOverlay(boolean show) {
        generatingOverlay.setVisible(show);
        generatingOverlay.setManaged(show);
    }

    private void animateProgress() {
        String[] steps = {
                "Authorizing access...",
                "Gathering case data...",
                "Formatting report...",
                "Hashing content...",
                "Finalizing..."
        };
        Timeline tl = new Timeline();
        for (int i = 0; i < steps.length; i++) {
            final int    idx = i;
            final double pct = (i + 1) / (double) steps.length;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 600), e -> {
                lblProgressStep.setText(steps[idx]);
                lblProgress.setText((int)(pct * 100) + "%");
            }));
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * 600),
                    new KeyValue(progressFill.prefWidthProperty(), pct * 300)
            ));
        }
        tl.play();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private void setValidationError(String msg) {
        lblValidationError.setText(msg);
        boolean hasError = !msg.isEmpty();
        lblValidationError.setVisible(hasError);
        lblValidationError.setManaged(hasError);
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Indicium — Report");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
