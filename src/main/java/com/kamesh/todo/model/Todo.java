package com.kamesh.todo.model;

import java.time.LocalDateTime;

public class Todo {
    private int todoId;
    private String todoName;
    private String todoDescription;
    private LocalDateTime createdAt;

    public Todo(int todoId, String todoName, String todoDescription) {
        this.todoId = todoId;
        this.todoName = todoName;
        this.todoDescription = todoDescription;
    }

    public Todo(int todoId, String todoName, String todoDescription, LocalDateTime createdAt) {
        this.todoId = todoId;
        this.todoName = todoName;
        this.todoDescription = todoDescription;
        this.createdAt = createdAt;
    }

    public Todo(int todoId, String todoName) {
        this(todoId, todoName, "");
    }

    public int getTodoId()             { return todoId; }
    public String getTodoName()        { return todoName; }
    public String getTodoDescription() { return todoDescription; }
    public LocalDateTime getCreatedAt(){ return createdAt; }

    public void setTodoId(int id)                { this.todoId = id; }
    public void setTodoName(String name)         { this.todoName = name; }
    public void setTodoDescription(String desc)  { this.todoDescription = desc; }

    /** True if this task was created within the last 24 hours. */
    public boolean isEditable() {
        if (createdAt == null) return false;
        return createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    @Override
    public String toString() {
        return todoName;
    }
}
