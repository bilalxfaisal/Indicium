package com.indicium.repository;

import com.indicium.models.Note;
import com.indicium.models.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NotesRepository {

    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        loadDatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found.");
        }
        createTablesIfNotExists();
    }

    private static void loadDatabaseConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("database.properties")) {
            props.load(in);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASS = props.getProperty("db.password");
        } catch (IOException e) {
            System.err.println("CRITICAL: database.properties file not found!");
        }
    }

    private static void createTablesIfNotExists() {
        String createNotesTable = "CREATE TABLE IF NOT EXISTS Notes (" +
                "NoteID INT AUTO_INCREMENT PRIMARY KEY, " +
                "Title VARCHAR(255) NOT NULL, " +
                "Body TEXT, " +
                "Tag VARCHAR(50), " +
                "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "UserID INT NOT NULL, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE" +
                ")";

        String createTasksTable = "CREATE TABLE IF NOT EXISTS Tasks (" +
                "TaskID INT AUTO_INCREMENT PRIMARY KEY, " +
                "Text VARCHAR(255) NOT NULL, " +
                "Priority VARCHAR(50), " +
                "IsDone BOOLEAN DEFAULT FALSE, " +
                "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "UserID INT NOT NULL, " +
                "FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE" +
                ")";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = con.createStatement()) {
            stmt.execute(createNotesTable);
            stmt.execute(createTasksTable);
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR creating tables: " + e.getMessage());
        }
    }

    // ==========================================
    // NOTES
    // ==========================================
    public Note saveNote(Note note) {
        String sql = "INSERT INTO Notes (Title, Body, Tag, CreatedAt, UserID) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getBody());
            stmt.setString(3, note.getTag());
            stmt.setTimestamp(4, Timestamp.valueOf(note.getCreatedAt()));
            stmt.setInt(5, note.getUserId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    note.setNoteId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR saving note: " + e.getMessage());
        }
        return note;
    }

    public void deleteNote(int noteId) {
        String sql = "DELETE FROM Notes WHERE NoteID = ?";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, noteId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR deleting note: " + e.getMessage());
        }
    }

    public List<Note> getNotesByUserId(int userId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM Notes WHERE UserID = ? ORDER BY CreatedAt ASC";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(new Note(
                            rs.getInt("NoteID"),
                            rs.getString("Title"),
                            rs.getString("Body"),
                            rs.getString("Tag"),
                            rs.getTimestamp("CreatedAt").toLocalDateTime(),
                            rs.getInt("UserID")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR fetching notes: " + e.getMessage());
        }
        return notes;
    }

    // ==========================================
    // TASKS
    // ==========================================
    public Task saveTask(Task task) {
        String sql = "INSERT INTO Tasks (Text, Priority, IsDone, CreatedAt, UserID) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, task.getText());
            stmt.setString(2, task.getPriority());
            stmt.setBoolean(3, task.isDone());
            stmt.setTimestamp(4, Timestamp.valueOf(task.getCreatedAt()));
            stmt.setInt(5, task.getUserId());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setTaskId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR saving task: " + e.getMessage());
        }
        return task;
    }

    public void updateTaskStatus(int taskId, boolean isDone) {
        String sql = "UPDATE Tasks SET IsDone = ? WHERE TaskID = ?";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setBoolean(1, isDone);
            stmt.setInt(2, taskId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR updating task: " + e.getMessage());
        }
    }

    public void deleteTask(int taskId) {
        String sql = "DELETE FROM Tasks WHERE TaskID = ?";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR deleting task: " + e.getMessage());
        }
    }

    public List<Task> getTasksByUserId(int userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM Tasks WHERE UserID = ? ORDER BY CreatedAt ASC";
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new Task(
                            rs.getInt("TaskID"),
                            rs.getString("Text"),
                            rs.getString("Priority"),
                            rs.getBoolean("IsDone"),
                            rs.getTimestamp("CreatedAt").toLocalDateTime(),
                            rs.getInt("UserID")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[NotesRepo] ERROR fetching tasks: " + e.getMessage());
        }
        return tasks;
    }
}
