package com.workbilling.ui.tabs;

import com.workbilling.model.Bill;
import com.workbilling.model.WorkEntry;
import com.workbilling.service.BillingService;
import com.workbilling.service.SettingsService;
import com.workbilling.ui.util.BillActionHandler;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.LongConsumer;

public class BillingTab {
    private final BillingService billingService = new BillingService();
    private final SettingsService settingsService = new SettingsService();
    private final Stage stage;
    private final LongConsumer openWorkEntry;

    private final BorderPane content = new BorderPane();
    private final ComboBox<String> companyCombo = new ComboBox<>();
    private final DatePicker fromDate = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker toDate = new DatePicker(LocalDate.now());
    private final TableView<WorkEntry> previewTable = new TableView<>();
    private final TableView<Bill> billsTable = new TableView<>();
    private final Label grandTotalLabel = new Label("Grand Total: ₹0.00");
    private final Label previewStatusLabel = new Label("");
    private final Button deleteBillBtn = new Button("Delete Bill");
    private final Button updateRecordsBtn = new Button("Update Records");

    private List<WorkEntry> previewEntries;
    private Path previewPdfPath;
    private Bill draftBill;

    public BillingTab(Stage stage, LongConsumer openWorkEntry) {
        this.stage = stage;
        this.openWorkEntry = openWorkEntry;
        buildUi();
        refreshCompanies();
        refreshBills();
    }

    private void buildUi() {
        HBox filters = new HBox(10);
        filters.setPadding(new Insets(12));
        filters.setAlignment(Pos.CENTER_LEFT);

        companyCombo.setPrefWidth(200);
        Button loadBtn = new Button("Load Entries");
        Button previewBtn = new Button("Generate Preview");
        Button confirmBtn = new Button("Confirm & Finalize Bill");
        Button openPreviewBtn = new Button("Open Preview PDF");

        loadBtn.setOnAction(e -> loadEntries());
        previewBtn.setOnAction(e -> generatePreview());
        confirmBtn.setOnAction(e -> finalizeBill());
        openPreviewBtn.setOnAction(e -> openPreviewPdf());

        filters.getChildren().addAll(
                new Label("Company:"), companyCombo,
                new Label("From:"), fromDate,
                new Label("To:"), toDate,
                loadBtn, previewBtn, openPreviewBtn, confirmBtn
        );

        TableColumn<WorkEntry, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getWorkDate().format(AppPaths.DISPLAY_DATE)));

        TableColumn<WorkEntry, String> areasCol = new TableColumn<>("Areas");
        areasCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getAreas().size())));

        TableColumn<WorkEntry, String> totalCol = new TableColumn<>("Date Total");
        totalCol.setCellValueFactory(c -> new SimpleStringProperty(UiFormat.currency(c.getValue().getDateTotal())));

        previewTable.getColumns().addAll(dateCol, areasCol, totalCol);
        previewTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label billsTitle = new Label("Generated Bills");
        billsTitle.setStyle("-fx-font-weight: bold;");

        TableColumn<Bill, String> billNumCol = new TableColumn<>("Bill No");
        billNumCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBillNumber()));

        TableColumn<Bill, String> billCompanyCol = new TableColumn<>("Company");
        billCompanyCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCompanyName()));

        TableColumn<Bill, String> billPeriodCol = new TableColumn<>("Period");
        billPeriodCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFromDate().format(AppPaths.DISPLAY_DATE) + " - " +
                c.getValue().getToDate().format(AppPaths.DISPLAY_DATE)));

        TableColumn<Bill, String> billTotalCol = new TableColumn<>("Total");
        billTotalCol.setCellValueFactory(c -> new SimpleStringProperty(UiFormat.currency(c.getValue().getGrandTotal())));

        TableColumn<Bill, String> billStatusCol = new TableColumn<>("Status");
        billStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        billsTable.getColumns().addAll(billNumCol, billCompanyCol, billPeriodCol, billTotalCol, billStatusCol);
        billsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        billsTable.setPrefHeight(160);
        billsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, bill) -> {
            boolean selected = bill != null;
            deleteBillBtn.setDisable(!selected);
            updateRecordsBtn.setDisable(!selected);
        });

        deleteBillBtn.setDisable(true);
        updateRecordsBtn.setDisable(true);
        deleteBillBtn.getStyleClass().add("danger-button");
        deleteBillBtn.setOnAction(e -> {
            Bill selected = billsTable.getSelectionModel().getSelectedItem();
            BillActionHandler.deleteBill(selected, this::refreshBills);
        });
        updateRecordsBtn.setOnAction(e -> {
            Bill selected = billsTable.getSelectionModel().getSelectedItem();
            BillActionHandler.updateBillRecords(selected, openWorkEntry, this::refreshBills);
        });

        HBox billActions = new HBox(10, updateRecordsBtn, deleteBillBtn);
        billActions.setAlignment(Pos.CENTER_LEFT);

        VBox billsSection = new VBox(8, billsTitle, billsTable, billActions);
        billsSection.setPadding(new Insets(8, 12, 12, 12));

        VBox bottom = new VBox(8);
        bottom.setPadding(new Insets(8, 12, 0, 12));
        bottom.getChildren().addAll(grandTotalLabel, previewStatusLabel,
                new Label("Tip: Only Saved (unbilled) entries appear above. Mark entries as Saved before billing."));

        VBox centerBox = new VBox(8, previewTable, new Separator(), bottom, billsSection);
        VBox.setVgrow(previewTable, Priority.ALWAYS);

        content.setTop(filters);
        content.setCenter(centerBox);
    }

    private void refreshBills() {
        try {
            billsTable.setItems(FXCollections.observableArrayList(billingService.getBillHistory()));
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void refreshCompanies() {
        try {
            companyCombo.setItems(FXCollections.observableArrayList(
                    settingsService.getCompanyName(1),
                    settingsService.getCompanyName(2)
            ));
            companyCombo.getSelectionModel().selectFirst();
        } catch (Exception e) {
            companyCombo.setItems(FXCollections.observableArrayList("Company 1", "Company 2"));
        }
    }

    private int selectedCompanyId() {
        return companyCombo.getSelectionModel().getSelectedIndex() + 1;
    }

    private void loadEntries() {
        try {
            if (fromDate.getValue() == null || toDate.getValue() == null) {
                Dialogs.showError("Validation", "Please select from and to dates.");
                return;
            }
            if (fromDate.getValue().isAfter(toDate.getValue())) {
                Dialogs.showError("Validation", "From date cannot be after to date.");
                return;
            }
            previewEntries = billingService.previewEntries(selectedCompanyId(), fromDate.getValue(), toDate.getValue());
            previewTable.setItems(FXCollections.observableArrayList(previewEntries));
            double total = billingService.calculateGrandTotal(previewEntries);
            grandTotalLabel.setText("Grand Total: " + UiFormat.currency(total));
            previewStatusLabel.setText(previewEntries.isEmpty()
                    ? "No billable entries found for the selected range."
                    : previewEntries.size() + " entries loaded for billing.");
            previewPdfPath = null;
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void generatePreview() {
        if (previewEntries == null || previewEntries.isEmpty()) {
            loadEntries();
            if (previewEntries == null || previewEntries.isEmpty()) return;
        }
        try {
            draftBill = new Bill();
            draftBill.setCompanyId(selectedCompanyId());
            draftBill.setFromDate(fromDate.getValue());
            draftBill.setToDate(toDate.getValue());
            previewPdfPath = billingService.generatePreviewPdf(draftBill, previewEntries);
            previewStatusLabel.setText("Preview generated: " + previewPdfPath.getFileName());
            Dialogs.showInfo("Preview Ready", "Bill preview generated. Review the PDF before confirming.");
        } catch (Exception e) {
            Dialogs.showError("Preview Failed", e.getMessage());
        }
    }

    private void openPreviewPdf() {
        if (previewPdfPath == null || !Files.exists(previewPdfPath)) {
            Dialogs.showInfo("No Preview", "Generate a preview first.");
            return;
        }
        openPdf(previewPdfPath);
    }

    private void finalizeBill() {
        if (previewEntries == null || previewEntries.isEmpty()) {
            Dialogs.showError("No Entries", "Load billable entries first.");
            return;
        }
        if (!Dialogs.confirmWarning("Finalize Bill",
                "You are about to create a final bill",
                "Included entries will be marked as Billed and cannot be billed again.\n\n" +
                "Company: " + companyCombo.getValue() + "\n" +
                "Period: " + fromDate.getValue().format(AppPaths.DISPLAY_DATE) +
                " to " + toDate.getValue().format(AppPaths.DISPLAY_DATE) + "\n" +
                "Grand Total: " + grandTotalLabel.getText().replace("Grand Total: ", "") + "\n\n" +
                "Continue?")) {
            return;
        }
        try {
            Bill bill = billingService.finalizeBill(
                    selectedCompanyId(), fromDate.getValue(), toDate.getValue(), previewEntries);
            Dialogs.showInfo("Bill Created",
                    "Bill " + bill.getBillNumber() + " created.\nGrand Total: " + UiFormat.currency(bill.getGrandTotal()));
            if (bill.getPdfPath() != null) {
                openPdf(Path.of(bill.getPdfPath()));
            }
            previewEntries = null;
            previewTable.getItems().clear();
            previewPdfPath = null;
            grandTotalLabel.setText("Grand Total: ₹0.00");
            previewStatusLabel.setText("Bill finalized successfully.");
            refreshBills();
        } catch (Exception e) {
            Dialogs.showError("Finalize Failed", e.getMessage());
        }
    }

    private void openPdf(Path path) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            } else {
                Dialogs.showInfo("PDF Saved", "PDF saved at: " + path);
            }
        } catch (Exception e) {
            Dialogs.showError("Open Failed", "Could not open PDF: " + e.getMessage());
        }
    }

    public BorderPane getContent() {
        return content;
    }
}
