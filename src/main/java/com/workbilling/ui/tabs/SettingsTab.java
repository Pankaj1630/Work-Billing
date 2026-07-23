package com.workbilling.ui.tabs;

import com.workbilling.service.BackupService;
import com.workbilling.service.SettingsService;
import com.workbilling.ui.util.Dialogs;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.nio.file.Path;

public class SettingsTab {
    private final SettingsService settingsService = new SettingsService();
    private final BackupService backupService = new BackupService();

    private final VBox content = new VBox(16);
    private final TextField company1Field = new TextField();
    private final TextField company2Field = new TextField();
    private final TextField pdfLocationField = new TextField();
    private final TextField backupLocationField = new TextField();

    public SettingsTab() {
        buildUi();
        loadSettings();
    }

    private void buildUi() {
        content.setPadding(new Insets(20));
        content.setMaxWidth(700);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        company1Field.setPrefWidth(350);
        company2Field.setPrefWidth(350);
        pdfLocationField.setPrefWidth(350);
        backupLocationField.setPrefWidth(350);

        Button browsePdfBtn = new Button("Browse");
        browsePdfBtn.setOnAction(e -> browseDirectory(pdfLocationField));
        Button browseBackupBtn = new Button("Browse");
        browseBackupBtn.setOnAction(e -> browseDirectory(backupLocationField));

        int row = 0;
        grid.add(new Label("Company 1 Name:"), 0, row);
        grid.add(company1Field, 1, row++);
        grid.add(new Label("Company 2 Name:"), 0, row);
        grid.add(company2Field, 1, row++);
        grid.add(new Label("PDF Save Location:"), 0, row);
        grid.add(new HBox(8, pdfLocationField, browsePdfBtn), 1, row++);
        grid.add(new Label("Backup Location:"), 0, row);
        grid.add(new HBox(8, backupLocationField, browseBackupBtn), 1, row++);

        Button saveBtn = new Button("Save Settings");
        saveBtn.setOnAction(e -> saveSettings());

        Button backupBtn = new Button("Backup Database");
        backupBtn.setOnAction(e -> backup());

        Button restoreBtn = new Button("Restore Database");
        restoreBtn.setOnAction(e -> restore());

        HBox actions = new HBox(10, saveBtn, backupBtn, restoreBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(
                new Label("Settings"),
                grid,
                actions,
                new Label("Data is stored locally in your user folder under WorkManagementBilling.")
        );
    }

    private void browseDirectory(TextField target) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder");
        var dir = chooser.showDialog(content.getScene().getWindow());
        if (dir != null) {
            target.setText(dir.getAbsolutePath());
        }
    }

    private void loadSettings() {
        try {
            company1Field.setText(settingsService.getCompanyName(1));
            company2Field.setText(settingsService.getCompanyName(2));
            pdfLocationField.setText(settingsService.getPdfSaveLocation());
            backupLocationField.setText(settingsService.getBackupLocation());
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void saveSettings() {
        try {
            if (company1Field.getText().isBlank() || company2Field.getText().isBlank()) {
                Dialogs.showError("Validation", "Company names cannot be empty.");
                return;
            }
            settingsService.setCompanyName(1, company1Field.getText().trim());
            settingsService.setCompanyName(2, company2Field.getText().trim());
            settingsService.setPdfSaveLocation(pdfLocationField.getText().trim());
            settingsService.setBackupLocation(backupLocationField.getText().trim());
            Dialogs.showInfo("Saved", "Settings saved successfully.");
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void backup() {
        try {
            Path backupFile = backupService.backup();
            Dialogs.showInfo("Backup Complete", "Database backed up to:\n" + backupFile);
        } catch (Exception e) {
            Dialogs.showError("Backup Failed", e.getMessage());
        }
    }

    private void restore() {
        if (!Dialogs.confirm("Restore Database",
                "This will replace all current data with the backup. Continue?")) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Backup File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        var file = chooser.showOpenDialog(content.getScene().getWindow());
        if (file == null) return;
        try {
            backupService.restore(file.toPath());
            Dialogs.showInfo("Restored", "Database restored. Please restart the application.");
        } catch (Exception e) {
            Dialogs.showError("Restore Failed", e.getMessage());
        }
    }

    public VBox getContent() {
        return content;
    }
}
