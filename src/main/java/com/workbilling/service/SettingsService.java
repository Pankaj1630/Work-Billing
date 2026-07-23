package com.workbilling.service;

import com.workbilling.repository.SettingsRepository;

import java.sql.SQLException;

public class SettingsService {
    private final SettingsRepository repository = new SettingsRepository();

    public String getCompanyName(int companyId) throws SQLException {
        return repository.getCompanyName(companyId);
    }

    public void setCompanyName(int companyId, String name) throws SQLException {
        repository.set(companyId == 1 ? "company1_name" : "company2_name", name);
    }

    public String getPdfSaveLocation() throws SQLException {
        return repository.get("pdf_save_location");
    }

    public void setPdfSaveLocation(String path) throws SQLException {
        repository.set("pdf_save_location", path);
    }

    public String getBackupLocation() throws SQLException {
        return repository.get("backup_location");
    }

    public void setBackupLocation(String path) throws SQLException {
        repository.set("backup_location", path);
    }

    public String getSetting(String key) throws SQLException {
        return repository.get(key);
    }

    public void setSetting(String key, String value) throws SQLException {
        repository.set(key, value);
    }
}
