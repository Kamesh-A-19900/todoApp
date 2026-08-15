package com.kamesh.todo.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class CompletionDao {

    private final Connection conn;

    public CompletionDao(Connection conn) {
        this.conn = conn;
    }

    /**
     * Returns a map of taskId → set of dates (within the given month) where status=1.
     */
    public Map<Integer, Set<LocalDate>> getCompletionsForMonth(int year, int month) throws SQLException {
        Map<Integer, Set<LocalDate>> result = new HashMap<>();
        String prefix = String.format("%04d-%02d-", year, month);
        String sql = "SELECT taskId, date FROM record WHERE date LIKE ? AND status = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int taskId = rs.getInt("taskId");
                    LocalDate date = LocalDate.parse(rs.getString("date"));
                    result.computeIfAbsent(taskId, k -> new HashSet<>()).add(date);
                }
            }
        }
        return result;
    }

    /**
     * Returns completed day count per taskId for the given month.
     * Map: taskId -> count of days with status=1
     */
    public Map<Integer, Integer> getCompletionCountsForMonth(int year, int month) throws SQLException {
        Map<Integer, Integer> result = new HashMap<>();
        String prefix = String.format("%04d-%02d-", year, month);
        String sql = "SELECT taskId, COUNT(*) AS cnt FROM record "
                   + "WHERE date LIKE ? AND status = 1 GROUP BY taskId";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("taskId"), rs.getInt("cnt"));
                }
            }
        }
        return result;
    }

    /**
     * Returns all distinct YearMonth values that have at least one record row.
     */
    public List<java.time.YearMonth> getDistinctMonths() throws SQLException {
        List<java.time.YearMonth> months = new ArrayList<>();
        String sql = "SELECT DISTINCT substr(date,1,7) AS ym FROM record ORDER BY ym";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String ym = rs.getString("ym"); // "YYYY-MM"
                String[] parts = ym.split("-");
                months.add(java.time.YearMonth.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1])));
            }
        }
        return months;
    }

    /**
     * Upserts a completion record for the given task and date.
     */
    public void setCompletion(int taskId, LocalDate date, boolean completed) throws SQLException {
        String sql = "INSERT OR REPLACE INTO record(date, taskId, status) VALUES(?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, taskId);
            ps.setInt(3, completed ? 1 : 0);
            ps.executeUpdate();
        }
    }
}
