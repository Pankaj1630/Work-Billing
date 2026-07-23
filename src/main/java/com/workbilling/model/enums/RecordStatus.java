package com.workbilling.model.enums;

public enum RecordStatus {
    DRAFT,
    SAVED,
    BILLED;

    public static RecordStatus fromDb(String value) {
        return RecordStatus.valueOf(value.toUpperCase());
    }
}
