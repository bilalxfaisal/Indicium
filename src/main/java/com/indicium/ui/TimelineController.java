package com.indicium.ui;

import com.indicium.controllers.TimeLineManager;
import com.indicium.models.Case;
import com.indicium.models.CaseStatus;
import com.indicium.models.Evidence;
import com.indicium.models.TimeLineEvent;
import com.indicium.repository.CaseRepository;
import com.indicium.repository.EvidenceRepo;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TimelineController extends StackPane {

    // ── State 1 ─────────────────────────────────────────────────
    @FXML private VBox             caseSelectorState;
    @FXML private ComboBox<Case>   caseSelector;
    @FXML private Label            errorNoCase;

    // ── State 2 ─────────────────────────────────────────────────
    @FXML private VBox             timelineState;
    @FXML private Label            activeCaseLabel;
    @FXML private Label            countEvents;
    @FXML private HBox             readOnlyBanner;
    @FXML private Button           btnAddEvent;
    @FXML private TextField        searchField;
    @FXML private DatePicker       filterDateFrom;
    @FXML private DatePicker       filterDateTo;
    @FXML private VBox             timelineContainer;
    @FXML private VBox             emptyState;

    // ── Add / Edit Modal ─────────────────────────────────────────
    @FXML private StackPane        modalEventForm;
    @FXML private Label            modalFormTitle;
    @FXML private TextField        inputEventTitle;
    @FXML private DatePicker       inputEventDate;
    @FXML private TextField        inputEventTime;
    @FXML private TextArea         inputEventDescription;
    @FXML private ComboBox<String> inputEvidenceLink;
    @FXML private HBox             linkedEvidenceChips;
    @FXML private HBox             warnSameTimestamp;
    @FXML private Label            errorEventTitle;
    @FXML private Label            errorEventDate;
    @FXML private Label            errorEventTime;

    // ── Delete Confirm Modal ─────────────────────────────────────
    @FXML private StackPane        modalDeleteConfirm;
    @FXML private Label            deleteConfirmMessage;

    // ── Internal state ───────────────────────────────────────────
    private int              activeCaseID      = -1;
    private boolean          isReadOnly        = false;
    private int              editingEventID    = -1;   // -1 = Add mode
    private int              pendingDeleteID   = -1;
    private List<TimeLineEvent> allEvents      = new ArrayList<>();
    private final List<Integer> linkedEvidenceIds = new ArrayList<>();

    private TimeLineManager  timelineManager;
    private CaseRepository   caseRepo;
    private EvidenceRepo     evidenceRepo;

    private static final DateTimeFormatter TIME_FMT    = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // ── Constructors ─────────────────────────────────────────────

    public TimelineController() {
        this(-1);
    }

    public TimelineController(int caseID) {
        URL fxmlResource = getClass().getResource("/com/indicium/ui/TimelineDashboard.fxml");
        if (fxmlResource == null)
            throw new RuntimeException("TimelineDashboard.fxml not found.");

        FXMLLoader loader = new FXMLLoader(fxmlResource);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TimelineDashboard.fxml: " + e.getMessage(), e);
        }

        if (caseID > 0) {
            loadTimelineForCase(caseID);
        } else {
            showCaseSelectorState();
        }
    }

    // ── Initialize ───────────────────────────────────────────────

    @FXML
    public void initialize() {
        timelineManager = new TimeLineManager();
        caseRepo        = new CaseRepository();
        evidenceRepo    = new EvidenceRepo();
        setupCaseSelector();
    }

    // ═══════════════════════════════════════════════════════════
    //  STATE 1 — CASE SELECTOR
    // ═══════════════════════════════════════════════════════════

    private void setupCaseSelector() {
        caseSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Case c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) setText(null);
                else setText("#" + String.format("%04d", c.getCaseID()) + "  —  " + c.getTitle());
            }
        });
        caseSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Case c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText("Search or select a case...");
                    setStyle("-fx-text-fill: #90A4AE;");
                } else {
                    setText("#" + String.format("%04d", c.getCaseID()) + "  —  " + c.getTitle());
                    setStyle("-fx-text-fill: #0D1B1E; -fx-font-weight: bold;");
                }
            }
        });

        try {
            List<Case> cases = caseRepo.findAll();
            caseSelector.getItems().setAll(cases);
        } catch (Exception e) {
            System.err.println("[TimelineController] Failed to load cases: " + e.getMessage());
        }
    }

    private void showCaseSelectorState() {
        caseSelectorState.setVisible(true);
        caseSelectorState.setManaged(true);
        timelineState.setVisible(false);
        timelineState.setManaged(false);
    }

    @FXML
    private void handleLoadTimeline() {
        Case selected = caseSelector.getValue();
        if (selected == null) {
            errorNoCase.setVisible(true);
            errorNoCase.setManaged(true);
            return;
        }
        errorNoCase.setVisible(false);
        errorNoCase.setManaged(false);
        loadTimelineForCase(selected.getCaseID());
    }

    // ═══════════════════════════════════════════════════════════
    //  STATE 2 — TIMELINE VIEW
    // ═══════════════════════════════════════════════════════════

    private void loadTimelineForCase(int caseID) {
        this.activeCaseID = caseID;

        Case c = timelineManager.selectCase(caseID);
        if (c != null) {
            activeCaseLabel.setText("#" + String.format("%04d", c.getCaseID()) + "  —  " + c.getTitle());
        }

        isReadOnly = timelineManager.isCaseReadOnly(caseID);
        applyReadOnlyState();

        caseSelectorState.setVisible(false);
        caseSelectorState.setManaged(false);
        timelineState.setVisible(true);
        timelineState.setManaged(true);

        populateEvidenceLinkCombo();
        loadEvents();
    }

    private void applyReadOnlyState() {
        readOnlyBanner.setVisible(isReadOnly);
        readOnlyBanner.setManaged(isReadOnly);
        btnAddEvent.setDisable(isReadOnly);
    }

    @FXML
    private void handleChangeCase() {
        activeCaseID   = -1;
        isReadOnly     = false;
        editingEventID = -1;
        allEvents      = new ArrayList<>();
        caseSelector.setValue(null);
        timelineContainer.getChildren().clear();
        timelineContainer.getChildren().add(emptyState);
        showCaseSelectorState();
    }

    // ═══════════════════════════════════════════════════════════
    //  LOAD & RENDER EVENTS
    // ═══════════════════════════════════════════════════════════

    private void loadEvents() {
        allEvents = timelineManager.getEventsForCase(activeCaseID);
        renderEvents(allEvents);
    }

    private void renderEvents(List<TimeLineEvent> events) {
        timelineContainer.getChildren().clear();
        timelineContainer.getChildren().add(emptyState);

        if (events == null || events.isEmpty()) {
            showEmptyState(true);
            countEvents.setText("0 Events");
            return;
        }

        showEmptyState(false);
        countEvents.setText(events.size() + " Event" + (events.size() == 1 ? "" : "s"));

        // Detect duplicate timestamps
        java.util.Map<LocalDateTime, Long> tsCounts = events.stream()
                .collect(Collectors.groupingBy(TimeLineEvent::getTimestamp, Collectors.counting()));

        java.util.Map<LocalDateTime, Integer> tsIndex = new java.util.HashMap<>();

        for (TimeLineEvent e : events) {
            String dupeLabel = null;
            long count = tsCounts.getOrDefault(e.getTimestamp(), 1L);
            if (count > 1) {
                int idx = tsIndex.merge(e.getTimestamp(), 1, Integer::sum);
                dupeLabel = "(" + idx + " of " + count + ")";
            }

            List<String> chips = new ArrayList<>();
            if (e.getLinkedEvidenceID() > 0)
                chips.add("EV-" + e.getLinkedEvidenceID());

            buildEventCard(
                    e.getEventID(),
                    e.getTitle(),
                    e.getTimestamp().toLocalDate().toString(),
                    e.getTimestamp().toLocalTime().format(TIME_FMT),
                    e.getDescription(),
                    e.getAddedBy(),
                    dupeLabel,
                    chips
            );
        }
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    // ═══════════════════════════════════════════════════════════
    //  BUILD EVENT CARD
    // ═══════════════════════════════════════════════════════════

    private void buildEventCard(int eventId, String title, String date,
                                String time, String description, String addedBy,
                                String dupeLabel, List<String> evidenceChips) {

        VBox wrapper = new VBox(8);
        wrapper.getStyleClass().add("event-wrapper");

        StackPane dot = new StackPane();
        dot.getStyleClass().add("event-dot");

        VBox card = new VBox(6);
        card.getStyleClass().add("event-card");

        // Timestamp row
        HBox timestampRow = new HBox(8);
        timestampRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTimestamp = new Label(date + "  " + time);
        lblTimestamp.getStyleClass().add("event-timestamp");
        timestampRow.getChildren().add(lblTimestamp);

        if (dupeLabel != null) {
            Label lblDupe = new Label(dupeLabel);
            lblDupe.getStyleClass().add("event-timestamp-dupe");
            timestampRow.getChildren().add(lblDupe);
        }

        // Title row with action buttons
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("event-title");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        titleRow.getChildren().add(lblTitle);

        // Edit / Delete buttons (hidden in read-only mode)
        if (!isReadOnly) {
            Button btnEdit = new Button();
            btnEdit.getStyleClass().add("btn-event-edit");
            btnEdit.setTooltip(new Tooltip("Edit event"));
            try {
                var stream = getClass().getResourceAsStream("/com/indicium/ui/Assets/icons8-edit-100.png");
                if (stream != null) {
                    ImageView iv = new ImageView(new Image(stream));
                    iv.setFitWidth(13); iv.setFitHeight(13); iv.setPreserveRatio(true);
                    btnEdit.setGraphic(iv);
                }
            } catch (Exception ignored) {}
            btnEdit.setOnAction(e -> handleEditEvent(eventId));

            Button btnDelete = new Button();
            btnDelete.getStyleClass().add("btn-event-delete");
            btnDelete.setTooltip(new Tooltip("Delete event"));
            try {
                var stream = getClass().getResourceAsStream("/com/indicium/ui/Assets/icons8-delete-100.png");
                if (stream != null) {
                    ImageView iv = new ImageView(new Image(stream));
                    iv.setFitWidth(13); iv.setFitHeight(13); iv.setPreserveRatio(true);
                    btnDelete.setGraphic(iv);
                }
            } catch (Exception ignored) {}
            btnDelete.setOnAction(e -> handleDeletePrompt(eventId, title));

            titleRow.getChildren().addAll(btnEdit, btnDelete);
        }

        // Description
        Label lblDesc = new Label(description != null ? description : "");
        lblDesc.getStyleClass().add("event-description");
        lblDesc.setWrapText(true);
        lblDesc.setVisible(description != null && !description.isBlank());
        lblDesc.setManaged(description != null && !description.isBlank());

        // Meta
        Label lblMeta = new Label("Added by " + (addedBy != null ? addedBy : "Unknown"));
        lblMeta.getStyleClass().add("event-meta");

        // Evidence chips
        HBox chipsRow = new HBox(6);
        chipsRow.getStyleClass().add("chips-row");
        for (String chip : evidenceChips) {
            Label c = new Label(chip);
            c.getStyleClass().add("evidence-chip");
            c.setOnMouseClicked(e -> handleEvidenceChipClick(chip));
            chipsRow.getChildren().add(c);
        }
        chipsRow.setVisible(!evidenceChips.isEmpty());
        chipsRow.setManaged(!evidenceChips.isEmpty());

        card.getChildren().addAll(timestampRow, titleRow, lblDesc, chipsRow, lblMeta);
        wrapper.getChildren().addAll(dot, card);
        timelineContainer.getChildren().add(wrapper);
    }

    // ═══════════════════════════════════════════════════════════
    //  FILTERS
    // ═══════════════════════════════════════════════════════════

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        renderEvents(allEvents);
    }

    private void applyFilters() {
        if (allEvents == null) return;

        String    query    = searchField.getText().toLowerCase().trim();
        LocalDate fromDate = filterDateFrom.getValue();
        LocalDate toDate   = filterDateTo.getValue();

        List<TimeLineEvent> filtered = allEvents.stream()
                .filter(e -> query.isEmpty()
                        || e.getTitle().toLowerCase().contains(query)
                        || (e.getDescription() != null
                        && e.getDescription().toLowerCase().contains(query)))
                .filter(e -> fromDate == null
                        || !e.getTimestamp().toLocalDate().isBefore(fromDate))
                .filter(e -> toDate == null
                        || !e.getTimestamp().toLocalDate().isAfter(toDate))
                .collect(Collectors.toList());

        renderEvents(filtered);
    }

    // ═══════════════════════════════════════════════════════════
    //  ADD / EDIT MODAL
    // ═══════════════════════════════════════════════════════════

    @FXML
    private void handleAddEvent() {
        if (isReadOnly) return;
        editingEventID = -1;
        modalFormTitle.setText("Add Event");
        clearEventForm();
        openEventModal();
    }

    private void handleEditEvent(int eventID) {
        TimeLineEvent e = allEvents.stream()
                .filter(ev -> ev.getEventID() == eventID)
                .findFirst().orElse(null);
        if (e == null) return;

        editingEventID = eventID;
        modalFormTitle.setText("Edit Event");

        inputEventTitle.setText(e.getTitle());
        inputEventDate.setValue(e.getTimestamp().toLocalDate());
        inputEventTime.setText(e.getTimestamp().toLocalTime().format(TIME_FMT));
        inputEventDescription.setText(e.getDescription() != null ? e.getDescription() : "");

        linkedEvidenceIds.clear();
        linkedEvidenceChips.getChildren().clear();
        if (e.getLinkedEvidenceID() > 0) {
            linkedEvidenceIds.add(e.getLinkedEvidenceID());
            addEvidenceChip("EV-" + e.getLinkedEvidenceID());
        }

        openEventModal();
    }

    private void openEventModal() {
        hideFormErrors();
        warnSameTimestamp.setVisible(false);
        warnSameTimestamp.setManaged(false);
        modalEventForm.setVisible(true);
        modalEventForm.setManaged(true);
    }

    @FXML
    private void handleCloseEventModal() {
        modalEventForm.setVisible(false);
        modalEventForm.setManaged(false);
        editingEventID = -1;
    }

    @FXML
    private void handleSubmitEvent() {
        if (!validateEventForm()) return;

        String    title       = inputEventTitle.getText().trim();
        LocalDate date        = inputEventDate.getValue();
        LocalTime time        = LocalTime.parse(inputEventTime.getText().trim(), TIME_FMT);
        LocalDateTime ts      = LocalDateTime.of(date, time);
        String    description = inputEventDescription.getText().trim();
        int       evidenceID  = linkedEvidenceIds.isEmpty() ? 0 : linkedEvidenceIds.get(0);

        // Warn about duplicate timestamp (non-blocking)
        boolean dupTs = allEvents.stream()
                .filter(e -> e.getEventID() != editingEventID)
                .anyMatch(e -> e.getTimestamp().equals(ts));
        warnSameTimestamp.setVisible(dupTs);
        warnSameTimestamp.setManaged(dupTs);

        boolean success;
        if (editingEventID == -1) {
            TimeLineEvent result = timelineManager.addEvent(
                    activeCaseID, title, description, ts, evidenceID);
            success = result != null;
        } else {
            success = timelineManager.editEvent(
                    editingEventID, activeCaseID, title, description, ts, evidenceID);
        }

        if (success) {
            handleCloseEventModal();
            loadEvents();
        } else {
            showAlert("Save Failed", "Could not save the event. Please try again.");
        }
    }

    // ── Evidence link combo ──────────────────────────────────────

    private void populateEvidenceLinkCombo() {
        inputEvidenceLink.getItems().clear();
        inputEvidenceLink.getItems().add("None");
        try {
            evidenceRepo.findByCaseId(activeCaseID).forEach(e ->
                    inputEvidenceLink.getItems().add("EV-" + e.getEvidenceID() + "  " + e.getName()));
        } catch (Exception e) {
            System.err.println("[TimelineController] Could not load evidence: " + e.getMessage());
        }
        inputEvidenceLink.setValue("None");
    }

    @FXML
    private void handleAddEvidenceLink() {
        String selected = inputEvidenceLink.getValue();
        if (selected == null || selected.equals("None")) return;

        // Parse EV-ID from the combo string
        String evId = selected.split("\\s+")[0]; // "EV-5"
        int    id   = Integer.parseInt(evId.replace("EV-", ""));

        if (!linkedEvidenceIds.contains(id)) {
            linkedEvidenceIds.clear();          // only one link per event for now
            linkedEvidenceChips.getChildren().clear();
            linkedEvidenceIds.add(id);
            addEvidenceChip(evId);
        }
    }

    private void addEvidenceChip(String label) {
        HBox chip = new HBox(6);
        chip.getStyleClass().add("evidence-chip-input");
        chip.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("evidence-chip");

        Button remove = new Button("×");
        remove.getStyleClass().add("chip-remove-btn");
        remove.setOnAction(e -> {
            linkedEvidenceChips.getChildren().remove(chip);
            linkedEvidenceIds.removeIf(id -> ("EV-" + id).equals(label));
        });

        chip.getChildren().addAll(lbl, remove);
        linkedEvidenceChips.getChildren().add(chip);
    }

    private void handleEvidenceChipClick(String evId) {
        showAlert("Evidence", "Linked to: " + evId
                + "\nOpen Evidence Dashboard to view details.");
    }

    // ═══════════════════════════════════════════════════════════
    //  DELETE MODAL
    // ═══════════════════════════════════════════════════════════

    private void handleDeletePrompt(int eventID, String title) {
        pendingDeleteID = eventID;
        deleteConfirmMessage.setText(
                "Delete event \"" + title + "\"?\nThis action cannot be undone.");
        modalDeleteConfirm.setVisible(true);
        modalDeleteConfirm.setManaged(true);
    }

    @FXML
    private void handleConfirmDelete() {
        if (pendingDeleteID == -1) return;
        boolean deleted = timelineManager.deleteEvent(pendingDeleteID, activeCaseID);
        if (deleted) {
            handleCloseDeleteModal();
            loadEvents();
        } else {
            showAlert("Delete Failed", "Could not delete the event.");
        }
    }

    @FXML
    private void handleCloseDeleteModal() {
        modalDeleteConfirm.setVisible(false);
        modalDeleteConfirm.setManaged(false);
        pendingDeleteID = -1;
    }

    // ═══════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════

    private boolean validateEventForm() {
        hideFormErrors();
        boolean valid = true;

        if (inputEventTitle.getText().trim().isEmpty()) {
            errorEventTitle.setText("Title is required.");
            errorEventTitle.setVisible(true); errorEventTitle.setManaged(true);
            valid = false;
        }
        if (inputEventDate.getValue() == null) {
            errorEventDate.setText("Date is required.");
            errorEventDate.setVisible(true); errorEventDate.setManaged(true);
            valid = false;
        }
        try {
            LocalTime.parse(inputEventTime.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException e) {
            errorEventTime.setText("Time must be in HH:mm format.");
            errorEventTime.setVisible(true); errorEventTime.setManaged(true);
            valid = false;
        }
        return valid;
    }

    private void hideFormErrors() {
        errorEventTitle.setVisible(false); errorEventTitle.setManaged(false);
        errorEventDate.setVisible(false);  errorEventDate.setManaged(false);
        errorEventTime.setVisible(false);  errorEventTime.setManaged(false);
    }

    private void clearEventForm() {
        inputEventTitle.clear();
        inputEventDate.setValue(null);
        inputEventTime.clear();
        inputEventDescription.clear();
        linkedEvidenceIds.clear();
        linkedEvidenceChips.getChildren().clear();
        inputEvidenceLink.setValue("None");
    }

    // ═══════════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════════

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
