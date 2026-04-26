package com.indicium.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

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
    private String  selectedReportType = "CASE_SUMMARY";
    private String  selectedCaseId     = null;
    private String  selectedCaseName   = null;

    // Toggle states
    private boolean includeInvestigators = true;
    private boolean includePhotoEvidence = true;
    private boolean includeTimeline      = true;
    private boolean includeAuditTrail    = false;

    // ── Constructor ──
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

    @FXML
    public void initialize() {
        setupCombos();
        setupFieldListeners();
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
    //  SECTION 1 — Report Type
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleSelectReportType(javafx.scene.input.MouseEvent e) {
        VBox clicked = (VBox) e.getSource();
        selectedReportType = (String) clicked.getUserData();

        // Toggle active card style
        cardCaseSummary.getStyleClass().remove("report-type-active");
        cardAuditLog.getStyleClass().remove("report-type-active");
        clicked.getStyleClass().add("report-type-active");

        // Hide case selector for audit log — it's system-wide
        boolean needsCase = selectedReportType.equals("CASE_SUMMARY");
        sectionCaseSelect.setVisible(needsCase);
        sectionCaseSelect.setManaged(needsCase);

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

        // TODO: SELECT case_id, title, status FROM cases
        //       WHERE (LOWER(case_id) LIKE ? OR LOWER(title) LIKE ?)
        //       AND (status = ? OR ? = 'All')
        //       ORDER BY created_at DESC LIMIT 8
        // TODO: For each result call buildCaseResultItem(caseId, title, status)

        caseResultsList.setVisible(true);
        caseResultsList.setManaged(true);
    }

    private void buildCaseResultItem(String caseId, String title, String status) {
        VBox item = new VBox(2);
        item.getStyleClass().add("case-result-item");
        item.setUserData(caseId);

        Label lblId    = new Label(caseId);
        lblId.getStyleClass().add("case-result-id");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("case-result-title");
        item.getChildren().addAll(lblId, lblTitle);

        item.setOnMouseClicked(e -> selectCase(caseId, title));
        caseResultsList.getChildren().add(item);
    }

    private void selectCase(String caseId, String title) {
        selectedCaseId   = caseId;
        selectedCaseName = title;

        chipCaseId.setText(caseId);
        chipCaseTitle.setText(title);

        selectedCaseChip.setVisible(true);
        selectedCaseChip.setManaged(true);
        caseResultsList.setVisible(false);
        caseResultsList.setManaged(false);
        caseSearchField.clear();

        refreshPreviewBar();
    }

    @FXML
    private void handleClearCase() {
        selectedCaseId   = null;
        selectedCaseName = null;
        selectedCaseChip.setVisible(false);
        selectedCaseChip.setManaged(false);
        refreshPreviewBar();
    }

    // ══════════════════════════════════════════════════════════
    //  SECTION 4 — Include Toggles
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleToggleSection(javafx.scene.input.MouseEvent e) {
        VBox toggle = (VBox) e.getSource();
        String section = (String) toggle.getUserData();

        switch (section) {
            case "INVESTIGATORS"  -> includeInvestigators = flip(
                    toggle, checkInvestigators, includeInvestigators);
            case "PHOTO_EVIDENCE" -> includePhotoEvidence = flip(
                    toggle, checkPhotoEvidence, includePhotoEvidence);
            case "TIMELINE"       -> includeTimeline = flip(
                    toggle, checkTimeline, includeTimeline);
            case "AUDIT_TRAIL"    -> includeAuditTrail = flip(
                    toggle, checkAuditTrail, includeAuditTrail);
        }

        refreshPreviewBar();
    }

    private boolean flip(VBox toggle, Label check, boolean current) {
        boolean next = !current;
        toggle.getStyleClass().removeAll("toggle-on", "toggle-off");
        toggle.getStyleClass().add(next ? "toggle-on" : "toggle-off");
        check.getStyleClass().removeAll("toggle-check", "toggle-check-off");
        check.getStyleClass().add(next ? "toggle-check" : "toggle-check-off");
        check.setText(next ? "✓" : "✕");
        return next;
    }

    // ══════════════════════════════════════════════════════════
    //  PREVIEW BAR
    // ══════════════════════════════════════════════════════════

    private void refreshPreviewBar() {
        boolean hasCase  = selectedCaseId != null
                || selectedReportType.equals("AUDIT_LOG");
        boolean hasDate  = !dateFrom.getText().isBlank()
                && !dateTo.getText().isBlank();
        boolean ready    = hasCase && hasDate;

        previewBar.setVisible(ready);
        previewBar.setManaged(ready);

        if (!ready) {
            btnExportPDF.setDisable(true);
            btnExportCSV.setDisable(true);
            return;
        }

        // Populate preview labels
        previewReportType.setText(
                selectedReportType.equals("CASE_SUMMARY") ? "Case Summary" : "Audit Log"
        );
        previewCaseId.setText(
                selectedCaseId != null ? selectedCaseId : "System-wide"
        );
        previewDateRange.setText(dateFrom.getText() + " → " + dateTo.getText());
        previewFormat.setText(outputFormat.getValue());

        List<String> sections = new ArrayList<>();
        if (includeInvestigators) sections.add("Investigators");
        if (includePhotoEvidence) sections.add("Photos");
        if (includeTimeline)      sections.add("Timeline");
        if (includeAuditTrail)    sections.add("Audit");
        previewSections.setText(sections.isEmpty() ? "None" : String.join(", ", sections));

        btnExportPDF.setDisable(false);
        btnExportCSV.setDisable(false);
    }

    // ══════════════════════════════════════════════════════════
    //  GENERATE / EXPORT
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleGenerateReport() {
        if (!validateForm()) return;
        showGeneratingOverlay("PDF");
    }

    @FXML
    private void handleExportPDF() {
        if (!validateForm()) return;
        showGeneratingOverlay("PDF");
    }

    @FXML
    private void handleExportCSV() {
        if (!validateForm()) return;
        showGeneratingOverlay("CSV");
    }

    private boolean validateForm() {
        lblValidationError.setVisible(false);
        lblValidationError.setManaged(false);

        if (selectedReportType.equals("CASE_SUMMARY") && selectedCaseId == null) {
            showValidationError("Please select a case before generating.");
            return false;
        }
        if (dateFrom.getText().isBlank() || dateTo.getText().isBlank()) {
            showValidationError("Please enter both From and To dates.");
            return false;
        }
        // TODO: Validate date format YYYY-MM-DD
        // TODO: Validate dateFrom is before dateTo
        return true;
    }

    private void showValidationError(String msg) {
        lblValidationError.setText("⚠  " + msg);
        lblValidationError.setVisible(true);
        lblValidationError.setManaged(true);
    }

    private void showGeneratingOverlay(String format) {
        generatingOverlay.setVisible(true);
        generatingOverlay.setManaged(true);

        // Animate progress bar 0 → full width over 2.5s
        double targetWidth = 260.0;
        Timeline progress = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressFill.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(800),
                        e -> lblProgressStep.setText("Querying case data..."),
                        new KeyValue(progressFill.prefWidthProperty(), targetWidth * 0.3)),
                new KeyFrame(Duration.millis(1600),
                        e -> lblProgressStep.setText("Compiling evidence..."),
                        new KeyValue(progressFill.prefWidthProperty(), targetWidth * 0.65)),
                new KeyFrame(Duration.millis(2400),
                        e -> lblProgressStep.setText("Formatting " + format + "..."),
                        new KeyValue(progressFill.prefWidthProperty(), targetWidth * 0.9)),
                new KeyFrame(Duration.millis(2800),
                        e -> {
                            lblProgressStep.setText("Done.");
                            lblProgress.setText("100%");
                            progressFill.setPrefWidth(targetWidth);
                            finishGeneration(format);
                        })
        );

        // Update % label in parallel
        Timeline pct = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressFill.prefWidthProperty(), 0))
        );
        progress.currentTimeProperty().addListener((o, ov, nv) -> {
            double pctVal = Math.min(100,
                    (nv.toMillis() / 2800.0) * 100);
            lblProgress.setText((int) pctVal + "%");
        });

        progress.play();
    }

    private void finishGeneration(String format) {
        // TODO: Trigger actual report generation:
        //   1. Gather case data from DB using selectedCaseId
        //   2. Gather investigators: SELECT u.name FROM case_users cu
        //                            JOIN users u ON cu.user_id = u.id
        //                            WHERE cu.case_id = ?
        //   3. Gather photo evidence: SELECT * FROM evidence
        //                             WHERE case_id = ? AND file_type = 'IMAGE'
        //   4. Gather timeline events: SELECT * FROM timeline
        //                              WHERE case_id = ? ORDER BY event_time
        //   5. Gather audit trail: SELECT * FROM audit_log
        //                          WHERE case_id = ?
        //                          AND timestamp BETWEEN dateFrom AND dateTo
        //   6. Pass collected data to ReportBuilder.generate(data, format)
        //   7. Open FileChooser for save path
        //   8. Write file to chosen path
        //   9. Log to audit_log: action=REPORT_GENERATE, type=Case

        javafx.animation.PauseTransition wait =
                new javafx.animation.PauseTransition(Duration.millis(600));
        wait.setOnFinished(e -> {
            generatingOverlay.setVisible(false);
            generatingOverlay.setManaged(false);
            progressFill.setPrefWidth(0);
            lblProgress.setText("0%");
            lblProgressStep.setText("Gathering case data...");
        });
        wait.play();
    }

    @FXML
    private void handleResetForm() {
        selectedCaseId   = null;
        selectedCaseName = null;
        dateFrom.clear();
        dateTo.clear();
        reportNotes.clear();
        outputFormat.setValue("PDF");
        caseSearchField.clear();
        selectedCaseChip.setVisible(false);
        selectedCaseChip.setManaged(false);
        caseResultsList.setVisible(false);
        caseResultsList.setManaged(false);

        // Reset toggles to defaults
        includeInvestigators = true;
        includePhotoEvidence = true;
        includeTimeline      = true;
        includeAuditTrail    = false;
        flip(toggleInvestigators, checkInvestigators, false);
        flip(togglePhotoEvidence, checkPhotoEvidence, false);
        flip(toggleTimeline,      checkTimeline,      false);
        flip(toggleAuditTrail,    checkAuditTrail,    true);

        // Reset type card
        cardAuditLog.getStyleClass().remove("report-type-active");
        cardCaseSummary.getStyleClass().add("report-type-active");
        selectedReportType = "CASE_SUMMARY";
        sectionCaseSelect.setVisible(true);
        sectionCaseSelect.setManaged(true);

        refreshPreviewBar();
    }
}
