package com.workbilling.service;

import com.workbilling.db.DatabaseManager;
import com.workbilling.repository.SettingsRepository;
import com.workbilling.util.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {
    private final SettingsRepository settingsRepository = new SettingsRepository();

    public Path backup() throws Exception {
        String location = settingsRepository.get("backup_location");
        Path backupDir = location != null ? Path.of(location) : AppPaths.getBackupDir();
        Files.createDirectories(backupDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path backupFile = backupDir.resolve("work_billing_backup_" + timestamp + ".db");
        Files.copy(AppPaths.getDatabasePath(), backupFile, StandardCopyOption.REPLACE_EXISTING);
        return backupFile;
    }

    public void restore(Path backupFile) throws Exception {
        if (!Files.exists(backupFile)) {
            throw new IOException("Backup file not found: " + backupFile);
        }
        Path dbPath = AppPaths.getDatabasePath();
        Files.copy(backupFile, dbPath, StandardCopyOption.REPLACE_EXISTING);
        DatabaseManager.initialize();
    }
}
