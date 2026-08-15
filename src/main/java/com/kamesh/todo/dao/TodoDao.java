package com.kamesh.todo.dao;

import com.kamesh.todo.model.Todo;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TodoDao {

    private final Connection conn;

    public TodoDao(Connection conn) {
        this.conn = conn;
    }

    /** All tasks in master list, newest first. */
    public List<Todo> getAllTodos() throws SQLException {
        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT id, name, description, created_at FROM tasks ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                todos.add(map(rs));
            }
        }
        return todos;
    }

    /** Tasks assigned to a given month (YYYY-MM). */
    public List<Todo> getMonthlyTasks(String month) throws SQLException {
        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT t.id, t.name, t.description, t.created_at FROM tasks t "
                   + "JOIN monthly_tasks mt ON mt.taskId = t.id AND mt.month = ? ORDER BY t.id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) todos.add(map(rs));
            }
        }
        return todos;
    }

    /** Tasks NOT yet assigned to the given month. */
    public List<Todo> getUnassignedTasks(String month) throws SQLException {
        List<Todo> todos = new ArrayList<>();
        String sql = "SELECT id, name, description, created_at FROM tasks "
                   + "WHERE id NOT IN (SELECT taskId FROM monthly_tasks WHERE month = ?) ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) todos.add(map(rs));
            }
        }
        return todos;
    }

    public void addTodo(String name, String description) throws SQLException {
        String sql = "INSERT INTO tasks(name, description) VALUES(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description == null ? "" : description);
            ps.executeUpdate();
        }
    }

    /** Update name/description — only allowed within 24 hrs (enforced at service layer). */
    public void updateTodo(int id, String name, String description) throws SQLException {
        String sql = "UPDATE tasks SET name = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description == null ? "" : description);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void assignToMonth(int taskId, String month) throws SQLException {
        String sql = "INSERT OR IGNORE INTO monthly_tasks(month, taskId) VALUES(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    public void removeFromMonth(int taskId, String month) throws SQLException {
        String sql = "DELETE FROM monthly_tasks WHERE month = ? AND taskId = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        }
    }

    private Todo map(ResultSet rs) throws SQLException {
        String raw = rs.getString("created_at");
        LocalDateTime createdAt = null;
        if (raw != null && !raw.isEmpty()) {
            try { createdAt = LocalDateTime.parse(raw.replace(" ", "T")); }
            catch (Exception ignored) {}
        }
        return new Todo(rs.getInt("id"), rs.getString("name"),
                rs.getString("description"), createdAt);
    }
}
