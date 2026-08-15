package com.kamesh.todo.service;

import com.kamesh.todo.dao.CompletionDao;
import com.kamesh.todo.dao.TodoDao;
import com.kamesh.todo.model.Todo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TodoService {

    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final TodoDao todoDao;
    private final CompletionDao completionDao;

    public TodoService(TodoDao todoDao, CompletionDao completionDao) {
        this.todoDao = todoDao;
        this.completionDao = completionDao;
    }

    /** All tasks in master list. */
    public List<Todo> getTodos() throws SQLException {
        return todoDao.getAllTodos();
    }

    /** Tasks assigned to the given month. */
    public List<Todo> getMonthlyTasks(YearMonth month) throws SQLException {
        return todoDao.getMonthlyTasks(month.format(MONTH_KEY));
    }

    /** Tasks NOT yet assigned to the given month. */
    public List<Todo> getUnassignedTasks(YearMonth month) throws SQLException {
        return todoDao.getUnassignedTasks(month.format(MONTH_KEY));
    }

    /** Add a new task to the master list. */
    public void addTodo(String name, String description) throws SQLException {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Task name cannot be blank.");
        todoDao.addTodo(name.trim(), description == null ? "" : description.trim());
    }

    /** Update task. Description editable anytime; name only within 24hrs. */
    public void updateTodo(int id, String name, String description, Todo task) throws SQLException {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Task name cannot be blank.");
        // If name changed but outside 24hr window, block it
        if (!task.getTodoName().equals(name.trim()) && !task.isEditable())
            throw new IllegalStateException("Task name can only be changed within 24 hours of creation.");
        todoDao.updateTodo(id, name.trim(), description == null ? "" : description.trim());
    }

    /** Assign a task to the current month's calendar. */
    public void assignToMonth(int taskId, YearMonth month) throws SQLException {
        todoDao.assignToMonth(taskId, month.format(MONTH_KEY));
    }

    /** Remove a task from the current month's calendar. */
    public void removeFromMonth(int taskId, YearMonth month) throws SQLException {
        todoDao.removeFromMonth(taskId, month.format(MONTH_KEY));
    }

    public Map<Integer, Set<LocalDate>> getMonthCompletions(YearMonth month) throws SQLException {
        return completionDao.getCompletionsForMonth(month.getYear(), month.getMonthValue());
    }

    public Map<Integer, Integer> getCompletionCounts(YearMonth month) throws SQLException {
        return completionDao.getCompletionCountsForMonth(month.getYear(), month.getMonthValue());
    }

    public List<YearMonth> getDistinctMonths() throws SQLException {
        return completionDao.getDistinctMonths();
    }

    public void toggleCompletion(int taskId, LocalDate date, boolean checked) throws SQLException {
        completionDao.setCompletion(taskId, date, checked);
    }
}
