package com.workbilling.repository;

import com.workbilling.db.DatabaseManager;
import com.workbilling.model.Bill;
import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.model.enums.BillStatus;
import com.workbilling.model.enums.RecordStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SettingsRepository {

    public String get(String key) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        }
    }

    public void set(String key, String value) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    public String getCompanyName(int companyId) throws SQLException {
        return get(companyId == 1 ? "company1_name" : "company2_name");
    }
}
