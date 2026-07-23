package com.workbilling.ui.tabs;

import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.model.enums.RecordStatus;
import com.workbilling.service.AutoSaveService;
import com.workbilling.service.SettingsService;
import com.workbilling.service.WorkEntryService;
import com.workbilling.ui.util.Dialogs;
import com.workbilling.ui.util.UiFormat;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;

public class WorkEntryTab {
    private final WorkEntryService workEntryService = new WorkEntryService();
    private final SettingsService settingsService = new SettingsService();
    private final AutoSaveService autoSaveService = new AutoSaveService(workEntryService);

    private final BorderPane content = new BorderPane();
    private final ComboBox<String> companyCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final Label statusLabel = new Label("Status: New");
    private final Label dateTotalLabel = new Label("Date Total: ₹0.00");
    private final Label autoSaveLabel = new Label("");
    private final VBox areasContainer = new VBox(12);

    private WorkEntry currentEntry;

    public WorkEntryTab() {
        buildUi();
        newEntry();
        autoSaveService.setStatusCallback(msg -> autoSaveLabel.setText(msg));
        autoSaveService.start();
    }

    private void buildUi() {
        HBox header = new HBox(12);
        header.setPadding(new Insets(12));
        header.setAlignment(Pos.CENTER_LEFT);

        companyCombo.setPrefWidth(200);
        datePicker.setPrefWidth(160);

        Button newBtn = new Button("New Entry");
        Button saveBtn = new Button("Save");
        Button markSavedBtn = new Button("Mark as Saved");
        Button duplicateBtn = new Button("Duplicate Previous");
        Button clearBtn = new Button("Clear");
        Button deleteBtn = new Button("Delete Entry");

        newBtn.setOnAction(e -> newEntry());
        saveBtn.setOnAction(e -> saveEntry(false));
        markSavedBtn.setOnAction(e -> markSaved());
        duplicateBtn.setOnAction(e -> showDuplicateDialog());
        clearBtn.setOnAction(e -> clearForm());
        deleteBtn.setOnAction(e -> deleteEntry());

        header.getChildren().addAll(
                new Label("Company:"), companyCombo,
                new Label("Date:"), datePicker,
                newBtn, saveBtn, markSavedBtn, duplicateBtn, clearBtn, deleteBtn,
                statusLabel, dateTotalLabel
        );

        ScrollPane scrollPane = new ScrollPane(areasContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(8));

        HBox footer = new HBox(12);
        footer.setPadding(new Insets(8, 12, 12, 12));
        footer.setAlignment(Pos.CENTER_LEFT);

        Button addAreaBtn = new Button("+ Add Area");
        addAreaBtn.setOnAction(e -> addArea(""));
        footer.getChildren().addAll(addAreaBtn, autoSaveLabel);

        content.setTop(header);
        content.setCenter(scrollPane);
        content.setBottom(footer);

        companyCombo.setOnAction(e -> {
            if (currentEntry != null) {
                currentEntry.setCompanyId(companyCombo.getSelectionModel().getSelectedIndex() + 1);
            }
        });
        datePicker.valueProperty().addListener((obs, old, val) -> {
            if (currentEntry != null && val != null) {
                currentEntry.setWorkDate(val);
            }
        });

        refreshCompanyNames();
    }

    public void loadEntry(long entryId) {
        try {
            WorkEntry entry = workEntryService.findById(entryId).orElse(null);
            if (entry == null) {
                Dialogs.showError("Not Found", "Work entry not found.");
                return;
            }
            if (entry.getStatus() == RecordStatus.BILLED) {
                Dialogs.showInfo("Billed Entry", "This entry is billed. Reopen the bill to edit it.");
            }
            setCurrentEntry(entry);
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void newEntry() {
        WorkEntry entry = new WorkEntry();
        entry.setCompanyId(1);
        entry.setWorkDate(LocalDate.now());
        entry.setStatus(RecordStatus.DRAFT);
        setCurrentEntry(entry);
    }

    private void setCurrentEntry(WorkEntry entry) {
        currentEntry = entry;
        autoSaveService.track(currentEntry);
        refreshCompanyNames();
        companyCombo.getSelectionModel().select(entry.getCompanyId() - 1);
        datePicker.setValue(entry.getWorkDate());
        statusLabel.setText("Status: " + entry.getStatus().name());
        rebuildAreasUi();
        updateDateTotal();
    }

    private void refreshCompanyNames() {
        try {
            String c1 = settingsService.getCompanyName(1);
            String c2 = settingsService.getCompanyName(2);
            companyCombo.setItems(FXCollections.observableArrayList(c1, c2));
        } catch (Exception e) {
            companyCombo.setItems(FXCollections.observableArrayList("Company 1", "Company 2"));
        }
    }

    private void rebuildAreasUi() {
        areasContainer.getChildren().clear();
        if (currentEntry.getAreas().isEmpty()) {
            areasContainer.getChildren().add(new Label("No areas yet. Click '+ Add Area' to begin."));
            return;
        }
        for (WorkArea area : currentEntry.getAreas()) {
            areasContainer.getChildren().add(buildAreaPanel(area));
        }
    }

    private VBox buildAreaPanel(WorkArea area) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("area-panel");
        panel.setPadding(new Insets(10));

        HBox areaHeader = new HBox(8);
        areaHeader.setAlignment(Pos.CENTER_LEFT);
        TextField areaNameField = new TextField(area.getName());
        areaNameField.setPromptText("Area name");
        areaNameField.setPrefWidth(300);
        areaNameField.textProperty().addListener((obs, o, v) -> {
            area.setName(v);
            updateDateTotal();
        });

        Label areaTotalLabel = new Label();
        Runnable updateAreaTotal = () -> areaTotalLabel.setText("Area Total: " + UiFormat.currency(area.getTotal()));
        updateAreaTotal.run();

        Button addItemBtn = new Button("+ Work Item");
        Button removeAreaBtn = new Button("Remove Area");
        removeAreaBtn.getStyleClass().add("danger-button");
        removeAreaBtn.setOnAction(e -> {
            currentEntry.getAreas().remove(area);
            rebuildAreasUi();
            updateDateTotal();
        });
        addItemBtn.setOnAction(e -> {
            area.getItems().add(new WorkItem("", 0, 0, 0));
            rebuildAreasUi();
        });

        areaHeader.getChildren().addAll(new Label("Area:"), areaNameField, addItemBtn, removeAreaBtn, areaTotalLabel);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 0, 0, 0));

        int row = 0;
        grid.add(new Label("Description"), 0, row);
        grid.add(new Label("Sq.Ft"), 1, row);
        grid.add(new Label("Nos"), 2, row);
        grid.add(new Label("Rate"), 3, row);
        grid.add(new Label("Manual"), 4, row);
        grid.add(new Label("Amount"), 5, row);
        grid.add(new Label("Total"), 6, row);
        grid.add(new Label(""), 7, row);
        row++;

        for (WorkItem item : area.getItems()) {
            TextField descField = new TextField(item.getDescription());
            descField.setPrefWidth(240);
            descField.textProperty().addListener((obs, o, v) -> {
                item.setDescription(v);
                updateDateTotal();
                updateAreaTotal.run();
            });

            TextField sqFtField = createNumberField(item.getSqFt());
            sqFtField.setPromptText("optional");
            TextField nosField = createNumberField(item.getNos());
            TextField rateField = createNumberField(item.getRate());
            CheckBox manualCheckBox = new CheckBox("Enter Amount Manually");
            manualCheckBox.setSelected(item.isManualAmount());
            TextField amountField = createNumberField(item.getStoredAmount());
            amountField.setDisable(!item.isManualAmount());
            Label totalLabel = new Label(UiFormat.currency(item.getAmount()));

            Runnable refreshTotals = () -> {
                totalLabel.setText(UiFormat.currency(item.getAmount()));
                updateAreaTotal.run();
                updateDateTotal();
            };

            Runnable applyManualMode = () -> {
                boolean manual = manualCheckBox.isSelected();
                item.setManualAmount(manual);
                sqFtField.setDisable(manual);
                nosField.setDisable(manual);
                rateField.setDisable(manual);
                amountField.setDisable(!manual);
                if (manual) {
                    amountField.setText(formatNumber(item.getStoredAmount()));
                } else {
                    amountField.setText("");
                }
                refreshTotals.run();
            };

            Runnable updateCalculatedFields = () -> {
                if (!item.isManualAmount()) {
                    item.setSqFt(WorkItem.parseSqFt(sqFtField.getText()));
                    item.setNos(parseDouble(nosField.getText()));
                    item.setRate(parseDouble(rateField.getText()));
                    refreshTotals.run();
                }
            };

            Runnable updateManualAmount = () -> {
                if (item.isManualAmount()) {
                    item.setStoredAmount(parseDouble(amountField.getText()));
                    refreshTotals.run();
                }
            };

            manualCheckBox.selectedProperty().addListener((obs, o, selected) -> applyManualMode.run());
            sqFtField.textProperty().addListener((obs, o, v) -> updateCalculatedFields.run());
            nosField.textProperty().addListener((obs, o, v) -> updateCalculatedFields.run());
            rateField.textProperty().addListener((obs, o, v) -> updateCalculatedFields.run());
            amountField.textProperty().addListener((obs, o, v) -> updateManualAmount.run());

            applyManualMode.run();

            Button removeItemBtn = new Button("X");
            removeItemBtn.getStyleClass().add("danger-button");
            removeItemBtn.setOnAction(e -> {
                area.getItems().remove(item);
                rebuildAreasUi();
                updateDateTotal();
            });

            grid.add(descField, 0, row);
            grid.add(sqFtField, 1, row);
            grid.add(nosField, 2, row);
            grid.add(rateField, 3, row);
            grid.add(manualCheckBox, 4, row);
            grid.add(amountField, 5, row);
            grid.add(totalLabel, 6, row);
            grid.add(removeItemBtn, 7, row);
            row++;
        }

        if (area.getItems().isEmpty()) {
            Label hint = new Label("No work items. Click '+ Work Item'.");
            grid.add(hint, 0, row, 8, 1);
        }

        panel.getChildren().addAll(areaHeader, grid);
        return panel;
    }

    private TextField createNumberField(double value) {
        TextField field = new TextField(formatNumber(value));
        field.setPrefWidth(80);
        return field;
    }

    private String formatNumber(double value) {
        if (value == 0) return "";
        if (value == Math.floor(value)) return String.format("%.0f", value);
        return String.format("%.2f", value);
    }

    private double parseDouble(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addArea(String name) {
        currentEntry.getAreas().add(new WorkArea(name));
        rebuildAreasUi();
    }

    private void updateDateTotal() {
        dateTotalLabel.setText("Date Total: " + UiFormat.currency(currentEntry.getDateTotal()));
    }

    private void saveEntry(boolean silent) {
        try {
            currentEntry.setCompanyId(companyCombo.getSelectionModel().getSelectedIndex() + 1);
            currentEntry.setWorkDate(datePicker.getValue());
            workEntryService.save(currentEntry);
            statusLabel.setText("Status: " + currentEntry.getStatus().name());
            if (!silent) {
                Dialogs.showInfo("Saved", "Work entry saved successfully.");
            }
        } catch (Exception e) {
            Dialogs.showError("Save Failed", e.getMessage());
        }
    }

    private void markSaved() {
        try {
            if (currentEntry.getAreas().isEmpty()) {
                Dialogs.showError("Validation", "Add at least one area before saving.");
                return;
            }
            currentEntry.setCompanyId(companyCombo.getSelectionModel().getSelectedIndex() + 1);
            currentEntry.setWorkDate(datePicker.getValue());
            workEntryService.markSaved(currentEntry);
            int savedCompanyId = currentEntry.getCompanyId();
            Dialogs.showInfo("Saved", "Entry marked as Saved and ready for billing.");
            newEntry();
            companyCombo.getSelectionModel().select(savedCompanyId - 1);
            currentEntry.setCompanyId(savedCompanyId);
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void clearForm() {
        boolean hasContent = !currentEntry.getAreas().isEmpty()
                || (currentEntry.getId() != null && currentEntry.getStatus() != RecordStatus.DRAFT);

        if (hasContent) {
            if (!Dialogs.confirmWarning("Clear Form",
                    "All entries on screen will be removed",
                    "This clears the current form only.\n" +
                    "Saved data in the database is NOT deleted unless you already saved this entry.\n\n" +
                    "Continue?")) {
                return;
            }
        }

        int companyIndex = companyCombo.getSelectionModel().getSelectedIndex();
        if (companyIndex < 0) {
            companyIndex = 0;
        }
        newEntry();
        companyCombo.getSelectionModel().select(companyIndex);
        currentEntry.setCompanyId(companyIndex + 1);
    }

    private void deleteEntry() {
        if (currentEntry.getId() == null) {
            newEntry();
            return;
        }
        if (!Dialogs.confirm("Delete Entry", "Delete this work entry permanently?")) {
            return;
        }
        try {
            workEntryService.delete(currentEntry.getId());
            newEntry();
            Dialogs.showInfo("Deleted", "Work entry deleted.");
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void showDuplicateDialog() {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Duplicate Previous Entry");
        dialog.setHeaderText("Select source entry and new date");

        ComboBox<WorkEntry> sourceCombo = new ComboBox<>();
        sourceCombo.setPrefWidth(400);
        sourceCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WorkEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getWorkDate() + " - " + item.getCompanyName() + " (" + item.getStatus() + ")");
                }
            }
        });
        sourceCombo.setButtonCell(sourceCombo.getCellFactory().call(null));

        DatePicker newDatePicker = new DatePicker(LocalDate.now());

        try {
            sourceCombo.setItems(FXCollections.observableArrayList(
                    workEntryService.search(null, null, null, null, null, true)));
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
            return;
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Source Entry:"), 0, 0);
        grid.add(sourceCombo, 1, 0);
        grid.add(new Label("New Date:"), 0, 1);
        grid.add(newDatePicker, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? newDatePicker.getValue() : null);
        dialog.showAndWait().ifPresent(newDate -> {
            WorkEntry source = sourceCombo.getValue();
            if (source == null || newDate == null) return;
            try {
                WorkEntry copy = workEntryService.duplicate(source.getId(), newDate);
                setCurrentEntry(copy);
                saveEntry(true);
                Dialogs.showInfo("Duplicated", "Entry duplicated. Modify values as needed.");
            } catch (Exception e) {
                Dialogs.showError("Error", e.getMessage());
            }
        });
    }

    public BorderPane getContent() {
        return content;
    }
}
