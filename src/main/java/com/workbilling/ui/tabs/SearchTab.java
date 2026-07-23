package com.workbilling.ui.tabs;

import com.workbilling.model.WorkEntry;
import com.workbilling.service.ExcelExportService;
import com.workbilling.service.SettingsService;
import com.workbilling.service.WorkEntryService;
import com.workbilling.ui.util.Dialogs;
import com.workbilling.ui.util.UiFormat;
import com.workbilling.util.AppPaths;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.function.LongConsumer;

public class SearchTab {
    private final WorkEntryService workEntryService = new WorkEntryService();
    private final SettingsService settingsService = new SettingsService();
    private final ExcelExportService excelExportService = new ExcelExportService();
    private final LongConsumer onOpenEntry;

    private final BorderPane content = new BorderPane();
    private final ComboBox<String> companyCombo = new ComboBox<>();
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();
    private final TextField areaField = new TextField();
    private final TextField descriptionField = new TextField();
    private final CheckBox includeBilled = new CheckBox("Include Billed");
    private final TableView<WorkEntry> table = new TableView<>();

    public SearchTab(LongConsumer onOpenEntry) {
        this.onOpenEntry = onOpenEntry;
        buildUi();
        search();
    }

    private void buildUi() {
        HBox filters = new HBox(10);
        filters.setPadding(new Insets(12));
        filters.setAlignment(Pos.CENTER_LEFT);

        companyCombo.setPromptText("All Companies");
        companyCombo.setPrefWidth(180);
        areaField.setPromptText("Area name");
        areaField.setPrefWidth(140);
        descriptionField.setPromptText("Work description");
        descriptionField.setPrefWidth(160);

        Button searchBtn = new Button("Search");
        Button openBtn = new Button("Open Selected");
        Button exportBtn = new Button("Export to Excel");
        searchBtn.setOnAction(e -> search());
        openBtn.setOnAction(e -> openSelected());
        exportBtn.setOnAction(e -> exportToExcel());

        filters.getChildren().addAll(
                new Label("Company:"), companyCombo,
                new Label("From:"), fromDate,
                new Label("To:"), toDate,
                new Label("Area:"), areaField,
                new Label("Description:"), descriptionField,
                includeBilled,
                searchBtn, openBtn, exportBtn
        );

        TableColumn<WorkEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getWorkDate().format(AppPaths.DISPLAY_DATE)));
        dateCol.setPrefWidth(120);

        TableColumn<WorkEntry, String> companyCol = new TableColumn<>("Company");
        companyCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCompanyName()));
        companyCol.setPrefWidth(150);

        TableColumn<WorkEntry, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setPrefWidth(80);

        TableColumn<WorkEntry, String> areasCol = new TableColumn<>("Areas");
        areasCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getAreas().size())));
        areasCol.setPrefWidth(60);

        TableColumn<WorkEntry, String> totalCol = new TableColumn<>("Date Total");
        totalCol.setCellValueFactory(c -> new SimpleStringProperty(UiFormat.currency(c.getValue().getDateTotal())));
        totalCol.setPrefWidth(120);

        table.getColumns().addAll(dateCol, companyCol, statusCol, areasCol, totalCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelected();
        });

        refreshCompanies();
        content.setTop(filters);
        content.setCenter(table);
    }

    private void refreshCompanies() {
        try {
            companyCombo.setItems(FXCollections.observableArrayList(
                    "All",
                    settingsService.getCompanyName(1),
                    settingsService.getCompanyName(2)
            ));
            companyCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            companyCombo.setItems(FXCollections.observableArrayList("All", "Company 1", "Company 2"));
            companyCombo.getSelectionModel().selectFirst();
        }
    }

    private void search() {
        try {
            Integer companyId = null;
            int idx = companyCombo.getSelectionModel().getSelectedIndex();
            if (idx > 0) companyId = idx;

            var results = workEntryService.search(
                    companyId,
                    fromDate.getValue(),
                    toDate.getValue(),
                    areaField.getText(),
                    descriptionField.getText(),
                    includeBilled.isSelected()
            );
            table.setItems(FXCollections.observableArrayList(results));
        } catch (Exception e) {
            Dialogs.showError("Search Failed", e.getMessage());
        }
    }

    private void openSelected() {
        WorkEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.showInfo("Select Entry", "Please select a work entry to open.");
            return;
        }
        onOpenEntry.accept(selected.getId());
    }

    private void exportToExcel() {
        if (table.getItems().isEmpty()) {
            Dialogs.showInfo("No Data", "Run a search first to export results.");
            return;
        }
        try {
            var path = excelExportService.exportWorkEntries(table.getItems());
            Dialogs.showInfo("Exported", "Excel file saved to:\n" + path);
        } catch (Exception e) {
            Dialogs.showError("Export Failed", e.getMessage());
        }
    }

    public BorderPane getContent() {
        return content;
    }
}
