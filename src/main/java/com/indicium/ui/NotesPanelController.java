package com.indicium.ui;

import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.indicium.models.Note;
import com.indicium.models.Task;
import com.indicium.repository.NotesRepository;
import com.indicium.services.SessionManager;

public class NotesPanelController extends VBox {

    // ── Header ──
    @FXML private Label noteCountLabel;

    // ── Tabs ──
    @FXML private Button tabNotes;
    @FXML private Button tabTasks;
    @FXML private VBox   notesPane;
    @FXML private VBox   tasksPane;

    // ── Notes compose ──
    @FXML private TextField noteTitleInput;
    @FXML private TextArea  noteBodyInput;
    @FXML private ComboBox<String> noteTagCombo;

    // ── Notes list ──
    @FXML private VBox notesList;
    @FXML private VBox notesEmpty;

    // ── Tasks compose ──
    @FXML private TextField taskInput;
    @FXML private ComboBox<String> taskPriorityCombo;

    // ── Tasks list ──
    @FXML private VBox tasksList;
    @FXML private VBox tasksEmpty;

    // ── Filter pills ──
    @FXML private Button filterAll;
    @FXML private Button filterOpen;
    @FXML private Button filterDone;

    // ── Data ──
    private final List<Note> notes = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private String activeTaskFilter = "All";
    
    private final NotesRepository notesRepo = new NotesRepository();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");

    // ── Close callback ──
    private Runnable onClose;

    // ══════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════



    // ══════════════════════════════════════════
    //  Initialize
    // ══════════════════════════════════════════

    @FXML
    public void initialize() {
        noteTagCombo.setItems(FXCollections.observableArrayList(
                "General", "Case", "Evidence", "Urgent"
        ));
        taskPriorityCombo.setItems(FXCollections.observableArrayList(
                "High", "Medium", "Low"
        ));
        taskPriorityCombo.setValue("Medium");

        // Load data from DB
        if (SessionManager.getInstance().isLoggedIn()) {
            int userId = SessionManager.getInstance().getCurrentUser().getUserID();
            notes.addAll(notesRepo.getNotesByUserId(userId));
            tasks.addAll(notesRepo.getTasksByUserId(userId));
        }

        // Hide panel off-screen initially
        setTranslateX(340);
        setVisible(false);
        setManaged(false);

        refreshCounts();
        renderNotes();
        renderTasks();
    }

    // ══════════════════════════════════════════
    //  Show / Hide (slide animation)
    // ══════════════════════════════════════════

    public void show() {
        setVisible(true);
        setManaged(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), this);
        tt.setFromX(340);
        tt.setToX(0);
        tt.play();
    }

    public void hide() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), this);
        tt.setFromX(0);
        tt.setToX(340);
        tt.setOnFinished(e -> {
            setVisible(false);
            setManaged(false);
        });
        tt.play();
    }

    public void setOnClose(Runnable r) { this.onClose = r; }

    @FXML
    private void handleClose() {
        hide();
        if (onClose != null) onClose.run();
    }

    // ══════════════════════════════════════════
    //  Tab switching
    // ══════════════════════════════════════════

    @FXML
    private void switchToNotes() {
        notesPane.setVisible(true);  notesPane.setManaged(true);
        tasksPane.setVisible(false); tasksPane.setManaged(false);
        tabNotes.getStyleClass().add("tab-active");
        tabTasks.getStyleClass().remove("tab-active");
    }

    @FXML
    private void switchToTasks() {
        tasksPane.setVisible(true);  tasksPane.setManaged(true);
        notesPane.setVisible(false); notesPane.setManaged(false);
        tabTasks.getStyleClass().add("tab-active");
        tabNotes.getStyleClass().remove("tab-active");
    }

    // ══════════════════════════════════════════
    //  NOTES
    // ══════════════════════════════════════════

    @FXML
    private void handleAddNote() {
        String title = noteTitleInput.getText().trim();
        String body  = noteBodyInput.getText().trim();
        if (title.isEmpty()) {
            noteTitleInput.setStyle("-fx-border-color: #C62828;");
            return;
        }
        noteTitleInput.setStyle("");

        String tag = noteTagCombo.getValue() != null ? noteTagCombo.getValue() : "General";
        int userId = SessionManager.getInstance().getCurrentUser().getUserID();
        
        Note note = new Note(title, body, tag, LocalDateTime.now(), userId);
        Note savedNote = notesRepo.saveNote(note);
        notes.add(savedNote);

        noteTitleInput.clear();
        noteBodyInput.clear();
        noteTagCombo.setValue(null);

        renderNotes();
        refreshCounts();
    }

    private void renderNotes() {
        notesList.getChildren().clear();
        notesList.getChildren().add(notesEmpty);

        notesEmpty.setVisible(notes.isEmpty());
        notesEmpty.setManaged(notes.isEmpty());

        for (int i = notes.size() - 1; i >= 0; i--) {
            Note n = notes.get(i);
            notesList.getChildren().add(buildNoteCard(n));
        }
    }

    private VBox buildNoteCard(Note note) {
        // Tag chip
        Label tag = new Label(note.getTag());
        tag.getStyleClass().addAll("tag-chip", "tag-" + note.getTag().toLowerCase());

        // Delete button
        Button del = new Button("✕");
        del.getStyleClass().add("note-delete-btn");
        del.setOnAction(e -> {
            notesRepo.deleteNote(note.getNoteId());
            notes.remove(note);
            renderNotes();
            refreshCounts();
        });

        // Top row: tag + spacer + delete
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox topRow = new HBox(6, tag, spacer, del);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(note.getTitle());
        title.getStyleClass().add("note-card-title");
        title.setWrapText(true);

        Label body = new Label(note.getBody());
        body.getStyleClass().add("note-card-body");
        body.setWrapText(true);
        body.setVisible(!note.getBody().isEmpty());
        body.setManaged(!note.getBody().isEmpty());

        Label meta = new Label(note.getCreatedAt().format(FMT));
        meta.getStyleClass().add("note-card-meta");

        VBox card = new VBox(6, topRow, title, body, meta);
        card.getStyleClass().add("note-card");
        return card;
    }

    // ══════════════════════════════════════════
    //  TASKS
    // ══════════════════════════════════════════

    @FXML
    private void handleAddTask() {
        String text = taskInput.getText().trim();
        if (text.isEmpty()) return;

        String priority = taskPriorityCombo.getValue() != null
                ? taskPriorityCombo.getValue() : "Medium";
                
        int userId = SessionManager.getInstance().getCurrentUser().getUserID();

        Task task = new Task(text, priority, false, LocalDateTime.now(), userId);
        Task savedTask = notesRepo.saveTask(task);
        tasks.add(savedTask);
        taskInput.clear();

        renderTasks();
        refreshCounts();
    }

    @FXML
    private void filterTasks(javafx.event.ActionEvent e) {
        Button src = (Button) e.getSource();
        activeTaskFilter = src.getText();

        filterAll.getStyleClass().remove("pill-active");
        filterOpen.getStyleClass().remove("pill-active");
        filterDone.getStyleClass().remove("pill-active");
        src.getStyleClass().add("pill-active");

        renderTasks();
    }

    private void renderTasks() {
        tasksList.getChildren().clear();
        tasksList.getChildren().add(tasksEmpty);

        List<Task> filtered = tasks.stream().filter(t -> switch (activeTaskFilter) {
            case "Open" -> !t.isDone();
            case "Done" -> t.isDone();
            default     -> true;
        }).toList();

        tasksEmpty.setVisible(filtered.isEmpty());
        tasksEmpty.setManaged(filtered.isEmpty());

        for (int i = filtered.size() - 1; i >= 0; i--) {
            Task t = filtered.get(i);
            tasksList.getChildren().add(buildTaskItem(t));
        }
    }

    private HBox buildTaskItem(Task task) {
        // Checkbox
        CheckBox cb = new CheckBox();
        cb.setSelected(task.isDone());
        cb.setStyle("-fx-cursor: hand;");

        // Task text
        Label text = new Label(task.getText());
        text.getStyleClass().add(task.isDone() ? "task-text-done" : "task-text");
        text.setWrapText(true);
        HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

        // Priority badge
        Label pri = new Label(task.getPriority());
        pri.getStyleClass().add("task-priority-" + task.getPriority().toLowerCase());

        // Delete
        Button del = new Button("✕");
        del.getStyleClass().add("task-delete-btn");
        del.setOnAction(e -> {
            notesRepo.deleteTask(task.getTaskId());
            tasks.remove(task);
            renderTasks();
            refreshCounts();
        });

        cb.setOnAction(e -> {
            task.setDone(cb.isSelected());
            notesRepo.updateTaskStatus(task.getTaskId(), cb.isSelected());
            renderTasks();
            refreshCounts();
        });

        HBox row = new HBox(10, cb, text, pri, del);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().addAll("task-item", task.isDone() ? "task-item-done" : "");
        return row;
    }

    // ══════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════

    private void refreshCounts() {
        long done = tasks.stream().filter(Task::isDone).count();
        noteCountLabel.setText(notes.size() + " notes · "
                + tasks.size() + " tasks (" + done + " done)");
    }
}
