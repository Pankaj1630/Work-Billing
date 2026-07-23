package com.workbilling.service;

import com.workbilling.model.Bill;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.enums.BillStatus;
import com.workbilling.model.enums.RecordStatus;
import com.workbilling.pdf.BillPdfGenerator;
import com.workbilling.repository.BillRepository;
import com.workbilling.repository.SettingsRepository;
import com.workbilling.repository.WorkEntryRepository;
import com.workbilling.util.AppPaths;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BillingService {
    private final BillRepository billRepository = new BillRepository();
    private final WorkEntryRepository workEntryRepository = new WorkEntryRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final BillPdfGenerator pdfGenerator = new BillPdfGenerator();

    public List<WorkEntry> previewEntries(int companyId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        return workEntryRepository.findForBilling(companyId, fromDate, toDate);
    }

    public double calculateGrandTotal(List<WorkEntry> entries) {
        return entries.stream().mapToDouble(WorkEntry::getDateTotal).sum();
    }

    public Path generatePreviewPdf(Bill draftBill, List<WorkEntry> entries) throws Exception {
        String companyName = settingsRepository.getCompanyName(draftBill.getCompanyId());
        draftBill.setCompanyName(companyName);
        draftBill.setWorkEntries(entries);
        draftBill.setGrandTotal(calculateGrandTotal(entries));
        if (draftBill.getBillNumber() == null) {
            draftBill.setBillNumber("PREVIEW");
        }
        Path previewPath = AppPaths.getBillsDir().resolve("preview_" + System.currentTimeMillis() + ".pdf");
        pdfGenerator.generate(draftBill, previewPath);
        return previewPath;
    }

    public Bill finalizeBill(int companyId, LocalDate fromDate, LocalDate toDate, List<WorkEntry> entries) throws Exception {
        if (entries.isEmpty()) {
            throw new IllegalStateException("No work entries available for billing");
        }

        String billNumber = billRepository.nextBillNumber();
        String companyName = settingsRepository.getCompanyName(companyId);
        double grandTotal = calculateGrandTotal(entries);

        Bill bill = new Bill();
        bill.setBillNumber(billNumber);
        bill.setCompanyId(companyId);
        bill.setCompanyName(companyName);
        bill.setFromDate(fromDate);
        bill.setToDate(toDate);
        bill.setGrandTotal(grandTotal);
        bill.setStatus(BillStatus.COMPLETED);
        bill.setGeneratedAt(LocalDateTime.now());
        bill.setWorkEntries(entries);

        String pdfLocation = settingsRepository.get("pdf_save_location");
        Path billsDir = pdfLocation != null ? Path.of(pdfLocation) : AppPaths.getBillsDir();
        Path pdfPath = billsDir.resolve(billNumber + ".pdf");
        pdfGenerator.generate(bill, pdfPath);
        bill.setPdfPath(pdfPath.toString());

        List<Long> entryIds = entries.stream().map(WorkEntry::getId).toList();
        billRepository.save(bill, entryIds);

        for (Long entryId : entryIds) {
            workEntryRepository.updateStatus(entryId, RecordStatus.BILLED);
        }

        return bill;
    }

    public List<Bill> getBillHistory() throws SQLException {
        return billRepository.findAll();
    }

    public Bill getBillDetails(long billId) throws SQLException {
        return billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));
    }

    public void reopenBill(long billId) throws SQLException {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() != BillStatus.COMPLETED) {
            throw new IllegalStateException("Only completed bills can be reopened");
        }

        billRepository.updateStatus(billId, BillStatus.CANCELLED);

        List<Long> entryIds = billRepository.findWorkEntryIdsForBill(billId);
        for (Long entryId : entryIds) {
            workEntryRepository.updateStatus(entryId, RecordStatus.SAVED);
        }
    }

    public void deleteBill(long billId) throws SQLException {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        List<Long> entryIds = billRepository.findWorkEntryIdsForBill(billId);
        for (Long entryId : entryIds) {
            workEntryRepository.updateStatus(entryId, RecordStatus.SAVED);
        }
        billRepository.deleteBill(billId);
    }

    public List<WorkEntry> unlockBillForUpdate(long billId) throws SQLException {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (bill.getStatus() == BillStatus.COMPLETED) {
            reopenBill(billId);
        } else {
            List<Long> entryIds = billRepository.findWorkEntryIdsForBill(billId);
            for (Long entryId : entryIds) {
                workEntryRepository.updateStatus(entryId, RecordStatus.SAVED);
            }
        }

        return getBillDetails(billId).getWorkEntries();
    }
}
