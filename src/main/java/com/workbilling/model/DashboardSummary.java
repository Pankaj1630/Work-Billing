package com.workbilling.model;

import java.time.LocalDate;

public class DashboardSummary {
    private String periodLabel;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String company1Name;
    private double company1BilledTotal;
    private double company1PendingTotal;
    private String company2Name;
    private double company2BilledTotal;
    private double company2PendingTotal;
    private int unbilledSavedCount;
    private double unbilledSavedTotal;
    private String lastBillNumber;
    private String lastBillCompany;
    private LocalDate lastBillDate;
    private double lastBillTotal;
    private boolean hasLastBill;

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getCompany1Name() {
        return company1Name;
    }

    public void setCompany1Name(String company1Name) {
        this.company1Name = company1Name;
    }

    public double getCompany1BilledTotal() {
        return company1BilledTotal;
    }

    public void setCompany1BilledTotal(double company1BilledTotal) {
        this.company1BilledTotal = company1BilledTotal;
    }

    public double getCompany1PendingTotal() {
        return company1PendingTotal;
    }

    public void setCompany1PendingTotal(double company1PendingTotal) {
        this.company1PendingTotal = company1PendingTotal;
    }

    public double getCompany1MonthTotal() {
        return company1BilledTotal + company1PendingTotal;
    }

    public String getCompany2Name() {
        return company2Name;
    }

    public void setCompany2Name(String company2Name) {
        this.company2Name = company2Name;
    }

    public double getCompany2BilledTotal() {
        return company2BilledTotal;
    }

    public void setCompany2BilledTotal(double company2BilledTotal) {
        this.company2BilledTotal = company2BilledTotal;
    }

    public double getCompany2PendingTotal() {
        return company2PendingTotal;
    }

    public void setCompany2PendingTotal(double company2PendingTotal) {
        this.company2PendingTotal = company2PendingTotal;
    }

    public double getCompany2MonthTotal() {
        return company2BilledTotal + company2PendingTotal;
    }

    public int getUnbilledSavedCount() {
        return unbilledSavedCount;
    }

    public void setUnbilledSavedCount(int unbilledSavedCount) {
        this.unbilledSavedCount = unbilledSavedCount;
    }

    public double getUnbilledSavedTotal() {
        return unbilledSavedTotal;
    }

    public void setUnbilledSavedTotal(double unbilledSavedTotal) {
        this.unbilledSavedTotal = unbilledSavedTotal;
    }

    public String getLastBillNumber() {
        return lastBillNumber;
    }

    public void setLastBillNumber(String lastBillNumber) {
        this.lastBillNumber = lastBillNumber;
    }

    public String getLastBillCompany() {
        return lastBillCompany;
    }

    public void setLastBillCompany(String lastBillCompany) {
        this.lastBillCompany = lastBillCompany;
    }

    public LocalDate getLastBillDate() {
        return lastBillDate;
    }

    public void setLastBillDate(LocalDate lastBillDate) {
        this.lastBillDate = lastBillDate;
    }

    public double getLastBillTotal() {
        return lastBillTotal;
    }

    public void setLastBillTotal(double lastBillTotal) {
        this.lastBillTotal = lastBillTotal;
    }

    public boolean isHasLastBill() {
        return hasLastBill;
    }

    public void setHasLastBill(boolean hasLastBill) {
        this.hasLastBill = hasLastBill;
    }

    public double getBilledGrandTotal() {
        return company1BilledTotal + company2BilledTotal;
    }

    public double getPendingGrandTotal() {
        return company1PendingTotal + company2PendingTotal;
    }

    public double getMonthGrandTotal() {
        return getBilledGrandTotal() + getPendingGrandTotal();
    }
}
