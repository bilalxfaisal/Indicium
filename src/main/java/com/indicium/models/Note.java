package com.indicium.models;

import java.time.LocalDateTime;

public class Note {
    private int noteId;
    private String title;
    private String body;
    private String tag;
    private LocalDateTime createdAt;
    private int userId;

    public Note(int noteId, String title, String body, String tag, LocalDateTime createdAt, int userId) {
        this.noteId = noteId;
        this.title = title;
        this.body = body;
        this.tag = tag;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Note(String title, String body, String tag, LocalDateTime createdAt, int userId) {
        this.title = title;
        this.body = body;
        this.tag = tag;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public int getNoteId() { return noteId; }
    public void setNoteId(int noteId) { this.noteId = noteId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
