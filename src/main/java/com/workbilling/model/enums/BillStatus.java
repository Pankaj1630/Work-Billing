package com.workbilling.model.enums;

public enum BillStatus {
    DRAFT,
    COMPLETED,
    CANCELLED;

    public static BillStatus fromDb(String value) {
        return BillStatus.valueOf(value.toUpperCase());
    }
}
