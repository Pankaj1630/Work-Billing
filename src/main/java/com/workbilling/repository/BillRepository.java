package com.workbilling.repository;

import com.workbilling.db.DatabaseManager;
import com.workbilling.model.Bill;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.enums.BillStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillRepository {
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final WorkEntryRepository workEntryRepository = new WorkEntryRepository();

    public synchronized String nextBillNumber() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int next;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT next_number FROM bill_counter WHERE id = 1")) {
                    next = rs.next() ? rs.getInt("next_number") : 1;
                }
                String billNumber = String.format("BILL-%05d", next);
                try (PreparedStatement ps = conn.prepareStatement("UPDATE bill_counter SET next_number = ? WHERE id = 1")) {
                    ps.setInt(1, next + 1);
                    ps.executeUpdate();
                }
                conn.commit();
                return billNumber;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public long save(Bill bill, List<Long> workEntryIds) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long billId;
                if (bill.getId() == null) {
                    String sql = """
                        INSERT INTO bills (bill_number, company_id, from_date, to_date, grand_total, status, pdf_path, generated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, bill.getBillNumber());
                        ps.setInt(2, bill.getCompanyId());
                        ps.setString(3, bill.getFromDate().toString());
                        ps.setString(4, bill.getToDate().toString());
                        ps.setDouble(5, bill.getGrandTotal());
                        ps.setString(6, bill.getStatus().name());
                        ps.setString(7, bill.getPdfPath());
                        ps.setString(8, bill.getGeneratedAt() != null ? bill.getGeneratedAt().toString() : null);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("Failed to create bill");
                            }
                            billId = keys.getLong(1);
                        }
                    }
                } else {
                    billId = bill.getId();
                    String sql = """
                        UPDATE bills SET bill_number=?, company_id=?, from_date=?, to_date=?, grand_total=?,
                        status=?, pdf_path=?, generated_at=? WHERE id=?
                        """;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, bill.getBillNumber());
                        ps.setInt(2, bill.getCompanyId());
                        ps.setString(3, bill.getFromDate().toString());
                        ps.setString(4, bill.getToDate().toString());
                        ps.setDouble(5, bill.getGrandTotal());
                        ps.setString(6, bill.getStatus().name());
                        ps.setString(7, bill.getPdfPath());
                        ps.setString(8, bill.getGeneratedAt() != null ? bill.getGeneratedAt().toString() : null);
                        ps.setLong(9, billId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bill_work_entries WHERE bill_id = ?")) {
                        ps.setLong(1, billId);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO bill_work_entries (bill_id, work_entry_id) VALUES (?, ?)")) {
                    for (Long entryId : workEntryIds) {
                        ps.setLong(1, billId);
                        ps.setLong(2, entryId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                bill.setId(billId);
                return billId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void updateStatus(long billId, BillStatus status) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE bills SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, billId);
            ps.executeUpdate();
        }
    }

    public List<Bill> findCompletedInRange(LocalDate fromDate, LocalDate toDate) throws SQLException {
        String sql = """
            SELECT * FROM bills
            WHERE status = ?
              AND from_date <= ?
              AND to_date >= ?
            ORDER BY generated_at DESC, id DESC
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, BillStatus.COMPLETED.name());
            ps.setString(2, toDate.toString());
            ps.setString(3, fromDate.toString());
            List<Bill> bills = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapBill(rs));
                }
            }
            return bills;
        }
    }

    public List<Bill> findAll() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bills ORDER BY generated_at DESC, id DESC")) {
            List<Bill> bills = new ArrayList<>();
            while (rs.next()) {
                bills.add(mapBill(rs));
            }
            return bills;
        }
    }

    public Optional<Bill> findById(long id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM bills WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Bill bill = mapBill(rs);
                    bill.setWorkEntries(loadWorkEntries(conn, id));
                    return Optional.of(bill);
                }
            }
        }
        return Optional.empty();
    }

    public List<Long> findWorkEntryIdsForBill(long billId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT work_entry_id FROM bill_work_entries WHERE bill_id = ?")) {
            ps.setLong(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("work_entry_id"));
                }
            }
        }
        return ids;
    }

    private List<WorkEntry> loadWorkEntries(Connection conn, long billId) throws SQLException {
        List<WorkEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT e.id FROM work_entries e
            JOIN bill_work_entries bwe ON bwe.work_entry_id = e.id
            WHERE bwe.bill_id = ?
            ORDER BY e.work_date
            """)) {
            ps.setLong(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workEntryRepository.findById(rs.getLong("id")).ifPresent(entries::add);
                }
            }
        }
        return entries;
    }

    private Bill mapBill(ResultSet rs) throws SQLException {
        Bill bill = new Bill();
        bill.setId(rs.getLong("id"));
        bill.setBillNumber(rs.getString("bill_number"));
        bill.setCompanyId(rs.getInt("company_id"));
        bill.setCompanyName(settingsRepository.getCompanyName(bill.getCompanyId()));
        bill.setFromDate(LocalDate.parse(rs.getString("from_date")));
        bill.setToDate(LocalDate.parse(rs.getString("to_date")));
        bill.setGrandTotal(rs.getDouble("grand_total"));
        bill.setStatus(BillStatus.fromDb(rs.getString("status")));
        bill.setPdfPath(rs.getString("pdf_path"));
        String generatedAt = rs.getString("generated_at");
        if (generatedAt != null) {
            bill.setGeneratedAt(LocalDateTime.parse(generatedAt));
        }
        return bill;
    }

    public void deleteBill(long billId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bills WHERE id = ?")) {
            ps.setLong(1, billId);
            ps.executeUpdate();
        }
    }
}
