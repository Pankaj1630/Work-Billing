package com.workbilling.service;

import com.workbilling.model.Bill;
import com.workbilling.model.DashboardSummary;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.enums.BillStatus;
import com.workbilling.model.enums.RecordStatus;
import com.workbilling.repository.BillRepository;
import com.workbilling.repository.SettingsRepository;
import com.workbilling.repository.WorkEntryRepository;
import com.workbilling.util.AppPaths;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class DashboardService {
    private final WorkEntryRepository workEntryRepository = new WorkEntryRepository();
    private final BillRepository billRepository = new BillRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();

    public DashboardSummary loadSummary(LocalDate fromDate, LocalDate toDate) throws SQLException {
        DashboardSummary summary = new DashboardSummary();
        summary.setFromDate(fromDate);
        summary.setToDate(toDate);
        summary.setPeriodLabel(fromDate.format(AppPaths.DISPLAY_DATE) + " to " + toDate.format(AppPaths.DISPLAY_DATE));
        summary.setCompany1Name(settingsRepository.getCompanyName(1));
        summary.setCompany2Name(settingsRepository.getCompanyName(2));

        double company1Billed = 0;
        double company2Billed = 0;
        for (Bill bill : billRepository.findCompletedInRange(fromDate, toDate)) {
            if (bill.getCompanyId() == 1) {
                company1Billed += bill.getGrandTotal();
            } else if (bill.getCompanyId() == 2) {
                company2Billed += bill.getGrandTotal();
            }
        }
        summary.setCompany1BilledTotal(company1Billed);
        summary.setCompany2BilledTotal(company2Billed);

        double company1Pending = 0;
        double company2Pending = 0;
        List<WorkEntry> unbilledSaved = workEntryRepository.search(
                null, fromDate, toDate, null, null, false).stream()
                .filter(e -> e.getStatus() == RecordStatus.SAVED)
                .toList();

        for (WorkEntry entry : unbilledSaved) {
            if (entry.getCompanyId() == 1) {
                company1Pending += entry.getDateTotal();
            } else if (entry.getCompanyId() == 2) {
                company2Pending += entry.getDateTotal();
            }
        }
        summary.setCompany1PendingTotal(company1Pending);
        summary.setCompany2PendingTotal(company2Pending);

        summary.setUnbilledSavedCount(unbilledSaved.size());
        summary.setUnbilledSavedTotal(unbilledSaved.stream().mapToDouble(WorkEntry::getDateTotal).sum());

        List<Bill> bills = billRepository.findAll();
        Bill lastCompleted = bills.stream()
                .filter(b -> b.getStatus() == BillStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        if (lastCompleted != null) {
            summary.setHasLastBill(true);
            summary.setLastBillNumber(lastCompleted.getBillNumber());
            summary.setLastBillCompany(lastCompleted.getCompanyName());
            summary.setLastBillTotal(lastCompleted.getGrandTotal());
            if (lastCompleted.getGeneratedAt() != null) {
                summary.setLastBillDate(lastCompleted.getGeneratedAt().toLocalDate());
            } else {
                summary.setLastBillDate(lastCompleted.getToDate());
            }
        } else {
            summary.setHasLastBill(false);
        }

        return summary;
    }

    public static LocalDate defaultFromDate() {
        return YearMonth.now().atDay(1);
    }

    public static LocalDate defaultToDate() {
        return YearMonth.now().atEndOfMonth();
    }
}
