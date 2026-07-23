package com.workbilling.db;

import com.workbilling.util.AppPaths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL_PREFIX + AppPaths.getDatabasePath());
    }

    public static void initialize() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS work_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    company_id INTEGER NOT NULL,
                    work_date TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'DRAFT',
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS work_areas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    work_entry_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (work_entry_id) REFERENCES work_entries(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS work_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    work_area_id INTEGER NOT NULL,
                    description TEXT NOT NULL,
                    sq_ft REAL NOT NULL DEFAULT 0,
                    nos REAL NOT NULL DEFAULT 0,
                    rate REAL NOT NULL DEFAULT 0,
                    manual_amount INTEGER NOT NULL DEFAULT 0,
                    amount REAL NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (work_area_id) REFERENCES work_areas(id) ON DELETE CASCADE
                )
                """);

            migrateWorkItemsTable(stmt);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bills (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    bill_number TEXT NOT NULL UNIQUE,
                    company_id INTEGER NOT NULL,
                    from_date TEXT NOT NULL,
                    to_date TEXT NOT NULL,
                    grand_total REAL NOT NULL,
                    status TEXT NOT NULL DEFAULT 'DRAFT',
                    pdf_path TEXT,
                    generated_at TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bill_work_entries (
                    bill_id INTEGER NOT NULL,
                    work_entry_id INTEGER NOT NULL,
                    PRIMARY KEY (bill_id, work_entry_id),
                    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
                    FOREIGN KEY (work_entry_id) REFERENCES work_entries(id)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bill_counter (
                    id INTEGER PRIMARY KEY CHECK (id = 1),
                    next_number INTEGER NOT NULL DEFAULT 1
                )
                """);

            stmt.execute("INSERT OR IGNORE INTO bill_counter (id, next_number) VALUES (1, 1)");

            insertDefaultSetting(stmt, "company1_name", "Company 1");
            insertDefaultSetting(stmt, "company2_name", "Company 2");
            insertDefaultSetting(stmt, "pdf_save_location", AppPaths.getBillsDir().toString());
            insertDefaultSetting(stmt, "backup_location", AppPaths.getBackupDir().toString());
        }
    }

    private static void migrateWorkItemsTable(Statement stmt) throws SQLException {
        addColumnIfMissing(stmt, "work_items", "manual_amount", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(stmt, "work_items", "amount", "REAL NOT NULL DEFAULT 0");
    }

    private static void addColumnIfMissing(Statement stmt, String table, String column, String definition)
            throws SQLException {
        boolean exists = false;
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static void insertDefaultSetting(Statement stmt, String key, String value) throws SQLException {
        stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('" + key + "', '" + value.replace("'", "''") + "')");
    }
}
