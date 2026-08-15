package com.kamesh.todo.model;

import java.time.LocalDate;

public class CompletionRecord {
    private int taskId;
    private LocalDate date;
    private boolean completed;

    public CompletionRecord(int taskId, LocalDate date, boolean completed) {
        this.taskId = taskId;
        this.date = date;
        this.completed = completed;
    }

    public int getTaskId() { return taskId; }
    public LocalDate getDate() { return date; }
    public boolean isCompleted() { return completed; }
}
