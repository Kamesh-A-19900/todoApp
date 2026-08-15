package com.kamesh.todo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String URL = "jdbc:sqlite:todo.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public static void initializeDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // Master task list
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS tasks ("
                + "id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name        TEXT UNIQUE NOT NULL,"
                + "description TEXT DEFAULT '',"
                + "created_at  TEXT DEFAULT (datetime('now'))"
                + ");"
            );

            // Daily completion records
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS record ("
                + "id      INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date    TEXT DEFAULT CURRENT_DATE,"
                + "taskId  INTEGER NOT NULL,"
                + "status  INTEGER NOT NULL DEFAULT 1 CHECK(status IN (0,1)),"
                + "FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE,"
                + "UNIQUE(date, taskId)"
                + ");"
            );

            // Which tasks are assigned to which month (YYYY-MM)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS monthly_tasks ("
                + "month   TEXT NOT NULL,"
                + "taskId  INTEGER NOT NULL,"
                + "FOREIGN KEY (taskId) REFERENCES tasks(id) ON DELETE CASCADE,"
                + "PRIMARY KEY (month, taskId)"
                + ");"
            );

            // Migration: add created_at to existing tasks tables that don't have it
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN created_at TEXT DEFAULT (datetime('now'))");
            } catch (SQLException ignored) {
                // Column already exists — fine
            }
        }
    }
}
