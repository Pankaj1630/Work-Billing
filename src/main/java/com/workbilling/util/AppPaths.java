package com.workbilling.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public final class AppPaths {
    public static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    public static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static Path appDataDir;

    private AppPaths() {
    }

    public static void init() throws IOException {
        String userHome = System.getProperty("user.home");
        appDataDir = Paths.get(userHome, "WorkManagementBilling");
        Files.createDirectories(appDataDir);
        Files.createDirectories(getBillsDir());
        Files.createDirectories(getBackupDir());
    }

    public static Path getAppDataDir() {
        return appDataDir;
    }

    public static Path getDatabasePath() {
        return appDataDir.resolve("work_billing.db");
    }

    public static Path getBillsDir() {
        return appDataDir.resolve("Bills");
    }

    public static Path getBackupDir() {
        return appDataDir.resolve("Backups");
    }

    public static String formatCurrency(double amount) {
        return String.format("₹%,.2f", amount);
    }
}
