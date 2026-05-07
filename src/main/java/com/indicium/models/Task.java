package com.indicium.models;

import java.time.LocalDateTime;

public class Task {
    private int taskId;
    private String text;
    private String priority;
    private boolean isDone;
    private LocalDateTime createdAt;
    private int userId;

    public Task(int taskId, String text, String priority, boolean isDone, LocalDateTime createdAt, int userId) {
        this.taskId = taskId;
        this.text = text;
        this.priority = priority;
        this.isDone = isDone;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Task(String text, String priority, boolean isDone, LocalDateTime createdAt, int userId) {
        this.text = text;
        this.priority = priority;
        this.isDone = isDone;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
