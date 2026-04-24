package com.indicium.ui;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TimelineController extends StackPane {

    // ── State 1: Case Selector ──────────────────────────────────
    @FXML private VBox              caseSelectorState;
    @FXML private ComboBox<String>  caseSelector;
    @FXML private Label             errorNoCase;

    // ── State 2: Timeline View ──────────────────────────────────
    @FXML private VBox              timelineState;
    @FXML private Label             activeCaseLabel;
    @FXML private Label             countEvents;
    @FXML private HBox              readOnlyBanner;
    @FXML private Button            btnAddEvent;
    @FXML private Button            btnChangeCase;
    @FXML private TextField         searchField;
    @FXML private DatePicker        filterDateFrom;
    @FXML private DatePicker        filterDateTo;
    @FXML private VBox              timelineContainer;
    @FXML private VBox              emptyState;

    // ── Add / Edit Modal ────────────────────────────────────────
    @FXML private StackPane         modalEventForm;
    @FXML private Label             modalFormTitle;
    @FXML private TextField         inputEventTitle;
    @FXML private DatePicker        inputEventDate;
    @FXML private TextField         inputEventTime;
    @FXML private TextArea          inputEventDescription;
    @FXML private ComboBox<String>  inputEvidenceLink;
    @FXML private HBox              linkedEvidenceChips;
    @FXML private HBox              warnSameTimestamp;
    @FXML private Label             errorEventTitle;
    @FXML private Label             errorEventDate;
    @FXML private Label             errorEventTime;
    @FXML private Label             errorEvidenceLink;

    // ── Delete Confirm Modal ────────────────────────────────────
    @FXML private StackPane         modalDeleteConfirm;
    @FXML private Label             deleteConfirmMessage;

    // ── Internal state ──────────────────────────────────────────
    private String          activeCaseId        = null;
    private boolean         isReadOnly          = false;
    private String          editingEventId      = null;  // null = Add mode, set = Edit mode
    private String          pendingDeleteId     = null;
    private final List<String> linkedEvidenceIds = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Constructors ─────────────────────────────────────────────

    /** Called from sidebar — shows case selector first */
    public TimelineController() {
        this(null);
    }

    /** Called from CaseDashBoardController overflow — skips selector */
    public TimelineController(String caseId) {
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

        if (caseId != null) {
            loadTimelineForCase(caseId);
        } else {
            showCaseSelectorState();
        }
    }

    @FXML
    public void initialize() {
        populateCaseSelector();
    }

    // ── Case Selector State ──────────────────────────────────────

    private void showCaseSelectorState() {
        caseSelectorState.setVisible(true);
        caseSelectorState.setManaged(true);
        timelineState.setVisible(false);
        timelineState.setManaged(false);
    }

    private void populateCaseSelector() {
        // TODO: Query DB for all cases the current user has access to
        // TODO: Populate caseSelector with "CASE-ID — Case Title" strings
        // TODO: Store a Map<String displayLabel, String caseId> for lookup on load
    }

    @FXML
    private void handleLoadTimeline() {
        String selected = caseSelector.getValue();
        if (selected == null || selected.isBlank()) {
            errorNoCase.setVisible(true);
            errorNoCase.setManaged(true);
            return;
        }
        errorNoCase.setVisible(false);
        errorNoCase.setManaged(false);

        // TODO: Resolve display label back to actual caseId from the map
        String caseId = selected; // replace with map lookup
        loadTimelineForCase(caseId);
    }

    // ── Timeline View State ──────────────────────────────────────

    private void loadTimelineForCase(String caseId) {
        this.activeCaseId = caseId;

        // TODO: Query DB for case title and status
        // TODO: Set activeCaseLabel to "CASE-ID — Case Title"
        activeCaseLabel.setText(caseId);

        // TODO: Check case status — if Locked or Archived, set isReadOnly = true
        applyReadOnlyState();

        // Switch to timeline view
        caseSelectorState.setVisible(false);
        caseSelectorState.setManaged(false);
        timelineState.setVisible(true);
        timelineState.setManaged(true);

        loadEvents();
        populateEvidenceLinkCombo();
    }

    private void applyReadOnlyState() {
        readOnlyBanner.setVisible(isReadOnly);
        readOnlyBanner.setManaged(isReadOnly);
        btnAddEvent.setDisable(isReadOnly);
    }

    @FXML
    private void handleChangeCase() {
        activeCaseId    = null;
        isReadOnly      = false;
        editingEventId  = null;
        timelineContainer.getChildren().clear();
        timelineContainer.getChildren().add(emptyState);
        caseSelector.setValue(null);
        showCaseSelectorState();
    }

    // ── Load Events ──────────────────────────────────────────────

    private void loadEvents() {
        timelineContainer.getChildren().clear();
        timelineContainer.getChildren().add(emptyState);

        // TODO: Query DB for all events WHERE case_id = activeCaseId ORDER BY event_date, event_time ASC
        // TODO: For each result call buildEventCard(...)
        // TODO: Detect duplicate timestamps — pass dupeLabel string if needed
        // TODO: Update countEvents label

        showEmptyState(true); // remove this line once DB is wired
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisible(show);
        emptyState.setManaged(show);
    }

    /**
     * Builds a single event card and appends it to timelineContainer.
     *
     * @param eventId     DB primary key for this event
     * @param title       Event title
     * @param date        e.g. "2026-04-10"
     * @param time        e.g. "14:35"
     * @param description Optional description text
     * @param addedBy     Username of the creator
     * @param dupeLabel   Pass "(1 of 2)" etc. if timestamp is shared, else null
     * @param evidenceIds List of linked evidence IDs for chip rendering
     */
    private void buildEventCard(String eventId, String title, String date,
                                String time, String description, String addedBy,
                                String dupeLabel, List<String> evidenceIds) {

        // Outer wrapper — provides the left cyan border line
        VBox wrapper = new VBox(8);
        wrapper.getStyleClass().add("event-wrapper");

        // Dot on the line
        StackPane dot = new StackPane();
        dot.getStyleClass().add("event-dot");

        // Card
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

        // Title
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("event-title");

        // Description (only if present)
        Label lblDesc = new Label(description != null ? description : "");
        lblDesc.getStyleClass().add("event-description");
        lblDesc.setWrapText(true);
        lblDesc.setVisible(description != null && !description.isBlank());
        lblDesc.setManaged(description != null && !description.isBlank());

        // Meta
        Label lblMeta = new Label("Added by " + addedBy);
        lblMeta.getStyleClass().add("event-meta");

        // Evidence chips
        HBox chipsRow = new HBox(6);
        chipsRow.getStyleClass().add("chips-row");
        for (String evId : evidenceIds) {
            Label chip = new Label(evId);
            chip.getStyleClass().add("evidence-chip");
            chip.setOnMouseClicked(e -> handleEvidenceChipClick(evId));
            chipsRow.getChildren().add(chip);
        }
        chipsRow.setVisible(!evidenceIds.isEmpty());
        chipsRow.setManaged(!evidenceIds.isEmpty());

        // Action buttons (hidden in read-only mode)
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("Edit");
        btnEdit.getStyleClass().add("btn-edit-event");
        btnEdit.setOnAction(e -> handleEditEvent(eventId));

        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("btn-delete-event");
        btnDelete.setOnAction(e -> handleDeleteEvent(eventId, title));

        actions.getChildren().addAll(btnEdit, btnDelete);
        actions.setVisible(!isReadOnly);
        actions.setManaged(!isReadOnly);

        card.getChildren().addAll(timestampRow, lblTitle, lblDesc, chipsRow, lblMeta, actions);

        // Assemble wrapper: dot overlaid, then card
        HBox rowLayout = new HBox(0);
        rowLayout.setAlignment(Pos.TOP_LEFT);
        rowLayout.getChildren().addAll(dot, card);
        HBox.setHgrow(card, Priority.ALWAYS);

        wrapper.getChildren().add(rowLayout);
        timelineContainer.getChildren().add(wrapper);
    }

    // ── Filter Handlers ──────────────────────────────────────────

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        applyFilters();
    }

    private void applyFilters() {
        String    query = searchField.getText().toLowerCase().trim();
        LocalDate from  = filterDateFrom.getValue();
        LocalDate to    = filterDateTo.getValue();
        // TODO: Filter timelineContainer children against query + date range
        // TODO: Show emptyState if no cards pass the filter
    }

    // ── Add / Edit Modal ─────────────────────────────────────────

    @FXML
    private void handleAddEvent() {
        editingEventId = null;
        clearEventModal();
        modalFormTitle.setText("Add Event");
        openModal(modalEventForm);
    }

    private void handleEditEvent(String eventId) {
        editingEventId = eventId;
        clearEventModal();
        modalFormTitle.setText("Edit Event");

        // TODO: Query DB for event by eventId
        // TODO: Populate inputEventTitle, inputEventDate, inputEventTime,
        //        inputEventDescription with existing values
        // TODO: Populate linkedEvidenceChips with existing links

        openModal(modalEventForm);
    }

    @FXML private void handleCloseEventModal() { closeAllModals(); }

    @FXML
    private void handleAddEvidenceLink() {
        String selected = inputEvidenceLink.getValue();
        if (selected == null || selected.isBlank()) return;
        if (linkedEvidenceIds.contains(selected)) return; // no duplicates

        linkedEvidenceIds.add(selected);

        Label chip = new Label(selected);
        chip.getStyleClass().add("evidence-chip");
        chip.setOnMouseClicked(e -> {
            linkedEvidenceIds.remove(selected);
            linkedEvidenceChips.getChildren().remove(chip);
        });
        linkedEvidenceChips.getChildren().add(chip);
        inputEvidenceLink.setValue(null);

        // TODO: Validate selected evidence belongs to activeCaseId (extension 5b)
        // TODO: Show errorEvidenceLink if it doesn't belong to this case
    }

    @FXML
    private void handleSubmitEvent() {
        if (!validateEventForm()) return;

        String    title       = inputEventTitle.getText().trim();
        LocalDate date        = inputEventDate.getValue();
        String    time        = inputEventTime.getText().trim();
        String    description = inputEventDescription.getText().trim();

        // Check for same-timestamp conflict
        // TODO: Query DB for existing events with same date+time in this case
        // TODO: If found, show warnSameTimestamp — do NOT block, just warn

        if (editingEventId == null) {
            // ── CREATE ──
            // TODO: INSERT event into DB (caseId, title, date, time, description, createdBy, createdAt)
            // TODO: INSERT evidence links for each id in linkedEvidenceIds
            // TODO: Log "Event Created" in audit trail
        } else {
            // ── UPDATE ──
            // TODO: UPDATE event record in DB by editingEventId
            // TODO: DELETE old evidence links, INSERT new ones
            // TODO: Log "Event Updated" with old vs new values in audit trail
        }

        closeAllModals();
        loadEvents();
    }

    private boolean validateEventForm() {
        boolean valid = true;

        if (inputEventTitle.getText().trim().isEmpty()) {
            showFieldError(errorEventTitle, "Event title is required.");
            valid = false;
        } else {
            hideFieldError(errorEventTitle);
        }

        if (inputEventDate.getValue() == null) {
            showFieldError(errorEventDate, "Event date is required.");
            valid = false;
        } else {
            hideFieldError(errorEventDate);
        }

        String time = inputEventTime.getText().trim();
        if (time.isEmpty()) {
            showFieldError(errorEventTime, "Event time is required.");
            valid = false;
        } else {
            try {
                LocalTime.parse(time, TIME_FMT);
                hideFieldError(errorEventTime);
            } catch (DateTimeParseException ex) {
                showFieldError(errorEventTime, "Use HH:mm format (e.g. 14:35).");
                valid = false;
            }
        }

        return valid;
    }

    // ── Delete Modal ─────────────────────────────────────────────

    private void handleDeleteEvent(String eventId, String eventTitle) {
        pendingDeleteId = eventId;
        deleteConfirmMessage.setText(
                "Delete \"" + eventTitle + "\"? This cannot be undone and will be logged in the audit trail."
        );
        openModal(modalDeleteConfirm);
    }

    @FXML private void handleCancelDelete() { pendingDeleteId = null; closeAllModals(); }

    @FXML
    private void handleConfirmDelete() {
        if (pendingDeleteId == null) return;

        // TODO: DELETE event record from DB by pendingDeleteId
        // TODO: DELETE associated evidence links
        // TODO: Log "Event Deleted" in audit trail
        // TODO: Remove corresponding card from timelineContainer
        // TODO: Update countEvents label
        // TODO: Show emptyState if no events remain

        pendingDeleteId = null;
        closeAllModals();
        loadEvents();
    }

    // ── Evidence Chip Click (on event cards) ─────────────────────

    private void handleEvidenceChipClick(String evidenceId) {
        // TODO: Show a small popup with evidence details (filename, type, status)
        // TODO: Re-verify hash on click and reflect tamper status
    }

    // ── Evidence Link ComboBox ────────────────────────────────────

    private void populateEvidenceLinkCombo() {
        inputEvidenceLink.getItems().clear();
        // TODO: Query DB for all evidence WHERE case_id = activeCaseId
        // TODO: Add "EV-XXXX — filename" strings to inputEvidenceLink
    }

    // ── Modal Helpers ─────────────────────────────────────────────

    private void openModal(StackPane modal) {
        modal.setVisible(true);
        modal.setManaged(true);
    }

    private void closeAllModals() {
        modalEventForm.setVisible(false);    modalEventForm.setManaged(false);
        modalDeleteConfirm.setVisible(false); modalDeleteConfirm.setManaged(false);
    }

    // ── Form Helpers ──────────────────────────────────────────────

    private void clearEventModal() {
        inputEventTitle.clear();
        inputEventDate.setValue(null);
        inputEventTime.clear();
        inputEventDescription.clear();
        inputEvidenceLink.setValue(null);
        linkedEvidenceChips.getChildren().clear();
        linkedEvidenceIds.clear();
        warnSameTimestamp.setVisible(false);
        warnSameTimestamp.setManaged(false);
        hideFieldError(errorEventTitle);
        hideFieldError(errorEventDate);
        hideFieldError(errorEventTime);
        hideFieldError(errorEvidenceLink);
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideFieldError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }
}
