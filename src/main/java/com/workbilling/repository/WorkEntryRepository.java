package com.workbilling.repository;

import com.workbilling.db.DatabaseManager;
import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.model.enums.RecordStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkEntryRepository {
    private final SettingsRepository settingsRepository = new SettingsRepository();

    public long save(WorkEntry entry) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long entryId = upsertEntry(conn, entry);
                deleteAreasNotInList(conn, entryId, entry.getAreas());
                for (int ai = 0; ai < entry.getAreas().size(); ai++) {
                    WorkArea area = entry.getAreas().get(ai);
                    area.setSortOrder(ai);
                    long areaId = upsertArea(conn, entryId, area);
                    deleteItemsNotInList(conn, areaId, area.getItems());
                    for (int ii = 0; ii < area.getItems().size(); ii++) {
                        WorkItem item = area.getItems().get(ii);
                        item.setSortOrder(ii);
                        upsertItem(conn, areaId, item);
                    }
                }
                conn.commit();
                entry.setId(entryId);
                return entryId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private long upsertEntry(Connection conn, WorkEntry entry) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        if (entry.getId() == null) {
            String sql = "INSERT INTO work_entries (company_id, work_date, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, entry.getCompanyId());
                ps.setString(2, entry.getWorkDate().toString());
                ps.setString(3, entry.getStatus().name());
                ps.setString(4, now.toString());
                ps.setString(5, now.toString());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
        } else {
            String sql = "UPDATE work_entries SET company_id=?, work_date=?, status=?, updated_at=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, entry.getCompanyId());
                ps.setString(2, entry.getWorkDate().toString());
                ps.setString(3, entry.getStatus().name());
                ps.setString(4, now.toString());
                ps.setLong(5, entry.getId());
                ps.executeUpdate();
            }
            return entry.getId();
        }
        throw new SQLException("Failed to save work entry");
    }

    private long upsertArea(Connection conn, long entryId, WorkArea area) throws SQLException {
        if (area.getId() == null) {
            String sql = "INSERT INTO work_areas (work_entry_id, name, sort_order) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, entryId);
                ps.setString(2, area.getName());
                ps.setInt(3, area.getSortOrder());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        long id = keys.getLong(1);
                        area.setId(id);
                        return id;
                    }
                }
            }
        } else {
            String sql = "UPDATE work_areas SET name=?, sort_order=? WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, area.getName());
                ps.setInt(2, area.getSortOrder());
                ps.setLong(3, area.getId());
                ps.executeUpdate();
            }
            return area.getId();
        }
        throw new SQLException("Failed to save work area");
    }

    private void upsertItem(Connection conn, long areaId, WorkItem item) throws SQLException {
        if (item.getId() == null) {
            String sql = """
                    INSERT INTO work_items (work_area_id, description, sq_ft, nos, rate, manual_amount, amount, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, areaId);
                ps.setString(2, item.getDescription());
                ps.setDouble(3, item.getSqFt());
                ps.setDouble(4, item.getNos());
                ps.setDouble(5, item.getRate());
                ps.setInt(6, item.isManualAmount() ? 1 : 0);
                ps.setDouble(7, item.getAmount());
                ps.setInt(8, item.getSortOrder());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        item.setId(keys.getLong(1));
                    }
                }
            }
        } else {
            String sql = """
                    UPDATE work_items SET description=?, sq_ft=?, nos=?, rate=?, manual_amount=?, amount=?, sort_order=?
                    WHERE id=?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, item.getDescription());
                ps.setDouble(2, item.getSqFt());
                ps.setDouble(3, item.getNos());
                ps.setDouble(4, item.getRate());
                ps.setInt(5, item.isManualAmount() ? 1 : 0);
                ps.setDouble(6, item.getAmount());
                ps.setInt(7, item.getSortOrder());
                ps.setLong(8, item.getId());
                ps.executeUpdate();
            }
        }
    }

    private void deleteAreasNotInList(Connection conn, long entryId, List<WorkArea> areas) throws SQLException {
        List<Long> ids = areas.stream().map(WorkArea::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM work_areas WHERE work_entry_id = ?")) {
                ps.setLong(1, entryId);
                ps.executeUpdate();
            }
            return;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        String sql = "DELETE FROM work_areas WHERE work_entry_id = ? AND id NOT IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, entryId);
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 2, ids.get(i));
            }
            ps.executeUpdate();
        }
    }

    private void deleteItemsNotInList(Connection conn, long areaId, List<WorkItem> items) throws SQLException {
        List<Long> ids = items.stream().map(WorkItem::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM work_items WHERE work_area_id = ?")) {
                ps.setLong(1, areaId);
                ps.executeUpdate();
            }
            return;
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        String sql = "DELETE FROM work_items WHERE work_area_id = ? AND id NOT IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, areaId);
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 2, ids.get(i));
            }
            ps.executeUpdate();
        }
    }

    public Optional<WorkEntry> findById(long id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM work_entries WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    WorkEntry entry = mapEntry(rs);
                    entry.setAreas(loadAreas(conn, id));
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    public List<WorkEntry> search(Integer companyId, LocalDate fromDate, LocalDate toDate,
                                  String areaName, String description, boolean includeBilled) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT e.* FROM work_entries e
            LEFT JOIN work_areas a ON a.work_entry_id = e.id
            LEFT JOIN work_items i ON i.work_area_id = a.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();

        if (companyId != null) {
            sql.append(" AND e.company_id = ?");
            params.add(companyId);
        }
        if (fromDate != null) {
            sql.append(" AND e.work_date >= ?");
            params.add(fromDate.toString());
        }
        if (toDate != null) {
            sql.append(" AND e.work_date <= ?");
            params.add(toDate.toString());
        }
        if (!includeBilled) {
            sql.append(" AND e.status != 'BILLED'");
        }
        if (areaName != null && !areaName.isBlank()) {
            sql.append(" AND a.name LIKE ?");
            params.add("%" + areaName.trim() + "%");
        }
        if (description != null && !description.isBlank()) {
            sql.append(" AND i.description LIKE ?");
            params.add("%" + description.trim() + "%");
        }
        sql.append(" ORDER BY e.work_date DESC, e.id DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            List<WorkEntry> entries = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WorkEntry entry = mapEntry(rs);
                    entry.setAreas(loadAreas(conn, entry.getId()));
                    entries.add(entry);
                }
            }
            return entries;
        }
    }

    public List<WorkEntry> findForBilling(int companyId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        return search(companyId, fromDate, toDate, null, null, false).stream()
                .filter(e -> e.getStatus() == RecordStatus.SAVED)
                .toList();
    }

    public void updateStatus(long entryId, RecordStatus status) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE work_entries SET status = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, LocalDateTime.now().toString());
            ps.setLong(3, entryId);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM work_entries WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private List<WorkArea> loadAreas(Connection conn, long entryId) throws SQLException {
        List<WorkArea> areas = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM work_areas WHERE work_entry_id = ? ORDER BY sort_order, id")) {
            ps.setLong(1, entryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WorkArea area = mapArea(rs);
                    area.setItems(loadItems(conn, area.getId()));
                    areas.add(area);
                }
            }
        }
        return areas;
    }

    private List<WorkItem> loadItems(Connection conn, long areaId) throws SQLException {
        List<WorkItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM work_items WHERE work_area_id = ? ORDER BY sort_order, id")) {
            ps.setLong(1, areaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapItem(rs));
                }
            }
        }
        return items;
    }

    private WorkEntry mapEntry(ResultSet rs) throws SQLException {
        WorkEntry entry = new WorkEntry();
        entry.setId(rs.getLong("id"));
        entry.setCompanyId(rs.getInt("company_id"));
        entry.setCompanyName(settingsRepository.getCompanyName(entry.getCompanyId()));
        entry.setWorkDate(LocalDate.parse(rs.getString("work_date")));
        entry.setStatus(RecordStatus.fromDb(rs.getString("status")));
        entry.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        entry.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at")));
        return entry;
    }

    private WorkArea mapArea(ResultSet rs) throws SQLException {
        WorkArea area = new WorkArea();
        area.setId(rs.getLong("id"));
        area.setWorkEntryId(rs.getLong("work_entry_id"));
        area.setName(rs.getString("name"));
        area.setSortOrder(rs.getInt("sort_order"));
        return area;
    }

    private WorkItem mapItem(ResultSet rs) throws SQLException {
        WorkItem item = new WorkItem();
        item.setId(rs.getLong("id"));
        item.setWorkAreaId(rs.getLong("work_area_id"));
        item.setDescription(rs.getString("description"));
        item.setSqFt(rs.getDouble("sq_ft"));
        item.setNos(rs.getDouble("nos"));
        item.setRate(rs.getDouble("rate"));
        item.setManualAmount(rs.getInt("manual_amount") == 1);
        item.setStoredAmount(rs.getDouble("amount"));
        item.setSortOrder(rs.getInt("sort_order"));
        return item;
    }
}
