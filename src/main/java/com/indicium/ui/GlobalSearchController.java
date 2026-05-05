package com.indicium.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GlobalSearchController extends StackPane {

    // ══════════════════════════════════════════════════════════
    //  FXML Bindings
    // ══════════════════════════════════════════════════════════

    @FXML private StackPane  backdrop;
    @FXML private VBox       searchPanel;
    @FXML private TextField  searchField;
    @FXML private ScrollPane resultsScroll;
    @FXML private VBox       resultsContainer;
    @FXML private VBox       emptyState;
    @FXML private Label      emptyLabel;
    @FXML private Label      resultCount;

    @FXML private Button chipAll;
    @FXML private Button chipCases;
    @FXML private Button chipEvidence;
    @FXML private Button chipSuspects;

    // ══════════════════════════════════════════════════════════
    //  State
    // ══════════════════════════════════════════════════════════

    private String           activeFilter = "ALL";
    private int              focusedIndex = -1;
    private final List<HBox> resultRows   = new ArrayList<>();

    // ══════════════════════════════════════════════════════════
    //  Callback — fired when user selects a result
    // ══════════════════════════════════════════════════════════

    private ResultSelectedCallback onResultSelected;

    public interface ResultSelectedCallback {
        void onSelected(String type, String id);
    }

    public void setOnResultSelected(ResultSelectedCallback cb) {
        this.onResultSelected = cb;
    }

    // ══════════════════════════════════════════════════════════
    //  Constructor — loads FXML
    // ══════════════════════════════════════════════════════════

    public GlobalSearchController() {
        URL url = getClass().getResource("/com/indicium/ui/GlobalSearch.fxml");
        if (url == null)
            throw new RuntimeException("GlobalSearch.fxml not found.");

        FXMLLoader loader = new FXMLLoader(url);
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load GlobalSearch.fxml: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        setupPanelAnimation();
        setupSearchListener();
        setupKeyboardNav();
    }

    // ══════════════════════════════════════════════════════════
    //  Show / Hide overlay
    // ══════════════════════════════════════════════════════════

    public void show() {
        this.setVisible(true);
        this.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(150), searchPanel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        Platform.runLater(() -> searchField.requestFocus());
    }

    public void hide() {
        FadeTransition ft = new FadeTransition(Duration.millis(120), searchPanel);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            this.setVisible(false);
            this.setManaged(false);
            searchField.clear();
            clearResults();
        });
        ft.play();
    }

    @FXML
    private void handleBackdropClick() {
        hide();
    }

    // ══════════════════════════════════════════════════════════
    //  Animation setup
    // ══════════════════════════════════════════════════════════

    private void setupPanelAnimation() {
        searchPanel.setOpacity(0);
        this.setVisible(false);
        this.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════
    //  Search listener — fires on every keystroke
    // ══════════════════════════════════════════════════════════

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.trim();
            if (query.length() < 2) {
                clearResults();
                return;
            }
            runSearch(query);
        });
    }

    // ══════════════════════════════════════════════════════════
    //  Keyboard navigation
    // ══════════════════════════════════════════════════════════

    private void setupKeyboardNav() {
        searchField.setOnKeyPressed(e -> {
            if      (e.getCode() == KeyCode.ESCAPE) { hide(); }
            else if (e.getCode() == KeyCode.DOWN)   { moveFocus(1);  e.consume(); }
            else if (e.getCode() == KeyCode.UP)     { moveFocus(-1); e.consume(); }
            else if (e.getCode() == KeyCode.ENTER)  { activateFocused(); e.consume(); }
        });
    }

    private void moveFocus(int direction) {
        if (resultRows.isEmpty()) return;

        if (focusedIndex >= 0 && focusedIndex < resultRows.size())
            resultRows.get(focusedIndex).getStyleClass().remove("result-focused");

        focusedIndex = Math.max(0, Math.min(resultRows.size() - 1, focusedIndex + direction));
        resultRows.get(focusedIndex).getStyleClass().add("result-focused");
    }

    private void activateFocused() {
        if (focusedIndex >= 0 && focusedIndex < resultRows.size()) {
            resultRows.get(focusedIndex).fireEvent(
                    new javafx.scene.input.MouseEvent(
                            javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                            0, 0, 0, 0,
                            javafx.scene.input.MouseButton.PRIMARY,
                            1, false, false, false, false,
                            true, false, false, false, false, false, null
                    )
            );
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Filter chips
    // ══════════════════════════════════════════════════════════

    @FXML private void handleChipAll()      { setActiveChip("ALL",      chipAll);      rerunSearch(); }
    @FXML private void handleChipCases()    { setActiveChip("CASES",    chipCases);    rerunSearch(); }
    @FXML private void handleChipEvidence() { setActiveChip("EVIDENCE", chipEvidence); rerunSearch(); }
    @FXML private void handleChipSuspects() { setActiveChip("SUSPECTS", chipSuspects); rerunSearch(); }

    private void setActiveChip(String filter, Button active) {
        activeFilter = filter;
        List.of(chipAll, chipCases, chipEvidence, chipSuspects)
                .forEach(b -> b.getStyleClass().remove("chip-active"));
        active.getStyleClass().add("chip-active");
    }

    private void rerunSearch() {
        String q = searchField.getText().trim();
        if (q.length() >= 2) runSearch(q);
    }

    // ══════════════════════════════════════════════════════════
    //  Core search
    // ══════════════════════════════════════════════════════════

    private void runSearch(String query) {
        clearResults();
        resultRows.clear();
        focusedIndex = -1;

        List<SearchResult> results = new ArrayList<>();

        // TODO: Open a DatabaseConnection
        // TODO: If activeFilter is "ALL" or "CASES"    — query cases table    (case_id, title, status LIKE %query%)    and add SearchResult("CASE",     id, title, "Status: "+status)
        // TODO: If activeFilter is "ALL" or "EVIDENCE" — query evidence table (evidence_id, name, type LIKE %query%)   and add SearchResult("EVIDENCE", id, name,  "Type: "+type+" · Case: "+case_id)
        // TODO: If activeFilter is "ALL" or "SUSPECTS" — query suspects table (suspect_id, name LIKE %query%)          and add SearchResult("SUSPECT",  id, name,  "Case: "+case_id)
        // TODO: Catch SQLException and print stack trace

        if (results.isEmpty()) {
            showEmpty("No results for \u201c" + query + "\u201d");
        } else {
            renderResults(results);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Render results grouped by type
    // ══════════════════════════════════════════════════════════

    private void renderResults(List<SearchResult> results) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        resultsScroll.setVisible(true);
        resultsScroll.setManaged(true);

        String lastType = "";
        for (SearchResult r : results) {

            // Group header — printed once per type change
            if (!r.type().equals(lastType)) {
                Label header = new Label(r.type() + "S");
                header.getStyleClass().add("search-group-header");
                resultsContainer.getChildren().add(header);
                lastType = r.type();
            }

            HBox row = buildResultRow(r);
            resultRows.add(row);
            resultsContainer.getChildren().add(row);
        }

        resultCount.setText(results.size() + " result" + (results.size() == 1 ? "" : "s"));
    }

    private HBox buildResultRow(SearchResult r) {
        HBox row = new HBox(12);
        row.getStyleClass().add("search-result-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Text block
        VBox text = new VBox(2);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label title    = new Label(r.title());
        Label subtitle = new Label(r.subtitle());
        title.getStyleClass().add("result-title");
        subtitle.getStyleClass().add("result-subtitle");
        text.getChildren().addAll(title, subtitle);

        // Type badge
        Label badge = new Label(r.type());
        badge.getStyleClass().addAll("result-badge",
                switch (r.type()) {
                    case "CASE"     -> "badge-case";
                    case "EVIDENCE" -> "badge-evidence";
                    default         -> "badge-suspect";
                }
        );

        row.getChildren().addAll(text, badge);

        // TODO: On click — call onResultSelected.onSelected(r.type(), r.id()) then hide()
        row.setOnMouseClicked(e -> {
            // TODO: navigate to the correct view based on type
        });

        return row;
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    private void clearResults() {
        resultsContainer.getChildren().clear();
        resultRows.clear();
        resultCount.setText("");
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        resultsScroll.setVisible(true);
        resultsScroll.setManaged(true);
    }

    private void showEmpty(String message) {
        resultsScroll.setVisible(false);
        resultsScroll.setManaged(false);
        emptyLabel.setText(message);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        resultCount.setText("0 results");
    }

    // ══════════════════════════════════════════════════════════
    //  Data model
    // ══════════════════════════════════════════════════════════

    private record SearchResult(String type, String id, String title, String subtitle) {}
}
