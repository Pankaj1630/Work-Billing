package com.workbilling.model;

public class WorkItem {
    private Long id;
    private Long workAreaId;
    private String description;
    private double sqFt;
    private double nos;
    private double rate;
    private boolean manualAmount;
    private double amount;
    private int sortOrder;

    public WorkItem() {
    }

    public WorkItem(String description, double sqFt, double nos, double rate) {
        this.description = description;
        this.sqFt = sqFt;
        this.nos = nos;
        this.rate = rate;
    }

    public boolean hasSqFt() {
        return sqFt > 0;
    }

    public boolean isManualAmount() {
        return manualAmount;
    }

    public void setManualAmount(boolean manualAmount) {
        this.manualAmount = manualAmount;
    }

    public double getStoredAmount() {
        return amount;
    }

    public void setStoredAmount(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        if (manualAmount) {
            return amount;
        }
        return calculateAmount();
    }

    private double calculateAmount() {
        boolean hasSqFt = sqFt > 0;
        boolean hasNos = nos > 0;
        boolean hasRate = rate > 0;

        if (!hasRate) {
            return 0;
        }
        if (!hasSqFt && !hasNos) {
            return 0;
        }
        if (hasSqFt && hasNos) {
            return sqFt * nos * rate;
        }
        if (hasSqFt) {
            return sqFt * rate;
        }
        return nos * rate;
    }

    public String getSqFtDisplay() {
        if (manualAmount) {
            return "NA";
        }
        return formatSqFt(sqFt);
    }

    public static String formatSqFt(double sqFt) {
        if (sqFt <= 0) {
            return "NA";
        }
        if (sqFt == Math.floor(sqFt)) {
            return String.format("%.0f", sqFt);
        }
        return String.format("%.2f", sqFt);
    }

    public static double parseSqFt(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return value > 0 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getNosDisplay() {
        if (manualAmount) {
            return "NA";
        }
        return formatNos(nos);
    }

    public String getRateDisplay() {
        if (manualAmount) {
            return "NA";
        }
        if (rate <= 0) {
            return "NA";
        }
        if (rate == Math.floor(rate)) {
            return String.format("%.0f", rate);
        }
        return String.format("%.2f", rate);
    }

    public static String formatNos(double nos) {
        if (nos <= 0) {
            return "NA";
        }
        if (nos == Math.floor(nos)) {
            return String.format("%.0f", nos);
        }
        return String.format("%.2f", nos);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkAreaId() {
        return workAreaId;
    }

    public void setWorkAreaId(Long workAreaId) {
        this.workAreaId = workAreaId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getSqFt() {
        return sqFt;
    }

    public void setSqFt(double sqFt) {
        this.sqFt = sqFt;
    }

    public double getNos() {
        return nos;
    }

    public void setNos(double nos) {
        this.nos = nos;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
