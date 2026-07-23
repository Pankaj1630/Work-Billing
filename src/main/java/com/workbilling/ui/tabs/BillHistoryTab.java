package com.workbilling.ui.tabs;

import com.workbilling.model.Bill;
import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.service.BillingService;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.LongConsumer;

public class BillHistoryTab {
    private final BillingService billingService = new BillingService();
    private final Stage stage;
    private final LongConsumer openWorkEntry;

    private final BorderPane content = new BorderPane();
    private final TableView<Bill> table = new TableView<>();
    private final TextArea detailsArea = new TextArea();
    private final Button deleteBillBtn = new Button("Delete Bill");
    private final Button updateRecordsBtn = new Button("Update Records");

    public BillHistoryTab(Stage stage, LongConsumer openWorkEntry) {
        this.stage = stage;
        this.openWorkEntry = openWorkEntry;
        buildUi();
        refresh();
    }

    private void buildUi() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(12));
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("Refresh");
        Button openPdfBtn = new Button("Open PDF");
        Button printBtn = new Button("Print PDF");
        Button viewDetailsBtn = new Button("View Details");

        refreshBtn.setOnAction(e -> refresh());
        openPdfBtn.setOnAction(e -> openSelectedPdf(false));
        printBtn.setOnAction(e -> printSelectedPdf());
        viewDetailsBtn.setOnAction(e -> showDetails());
        deleteBillBtn.setOnAction(e -> deleteBill());
        updateRecordsBtn.setOnAction(e -> updateRecords());
        deleteBillBtn.getStyleClass().add("danger-button");

        toolbar.getChildren().addAll(
                refreshBtn, openPdfBtn, printBtn, viewDetailsBtn,
                updateRecordsBtn, deleteBillBtn
        );

        TableColumn<Bill, String> numberCol = new TableColumn<>("Bill Number");
        numberCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBillNumber()));
        numberCol.setPrefWidth(120);

        TableColumn<Bill, String> companyCol = new TableColumn<>("Company");
        companyCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCompanyName()));

        TableColumn<Bill, String> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFromDate().format(AppPaths.DISPLAY_DATE)));

        TableColumn<Bill, String> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getToDate().format(AppPaths.DISPLAY_DATE)));

        TableColumn<Bill, String> totalCol = new TableColumn<>("Grand Total");
        totalCol.setCellValueFactory(c -> new SimpleStringProperty(UiFormat.currency(c.getValue().getGrandTotal())));

        TableColumn<Bill, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        table.getColumns().addAll(numberCol, companyCol, fromCol, toCol, totalCol, statusCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, bill) -> {
            boolean selected = bill != null;
            deleteBillBtn.setDisable(!selected);
            updateRecordsBtn.setDisable(!selected);
            if (bill != null) {
                showDetailsFor(bill);
            }
        });

        deleteBillBtn.setDisable(true);
        updateRecordsBtn.setDisable(true);

        detailsArea.setEditable(false);
        detailsArea.setPrefRowCount(8);
        detailsArea.setPromptText("Select a bill to view details");

        VBox bottom = new VBox(detailsArea);
        bottom.setPadding(new Insets(0, 12, 12, 12));

        content.setTop(toolbar);
        content.setCenter(table);
        content.setBottom(bottom);
    }

    private void refresh() {
        try {
            table.setItems(FXCollections.observableArrayList(billingService.getBillHistory()));
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void showDetails() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Dialogs.showInfo("Select Bill", "Please select a bill.");
            return;
        }
        showDetailsFor(selected);
    }

    private void showDetailsFor(Bill bill) {
        try {
            Bill full = billingService.getBillDetails(bill.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("Bill Number: ").append(full.getBillNumber()).append("\n");
            sb.append("Company: ").append(full.getCompanyName()).append("\n");
            sb.append("Status: ").append(full.getStatus()).append("\n");
            sb.append("Grand Total: ").append(UiFormat.currency(full.getGrandTotal())).append("\n");
            sb.append("PDF: ").append(full.getPdfPath()).append("\n\n");

            for (WorkEntry entry : full.getWorkEntries()) {
                sb.append("Date: ").append(entry.getWorkDate().format(AppPaths.DISPLAY_DATE)).append("\n");
                for (WorkArea area : entry.getAreas()) {
                    sb.append("  Area: ").append(area.getName()).append("\n");
                    for (WorkItem item : area.getItems()) {
                        sb.append("    ").append(item.getDescription())
                                .append(" | ").append(item.getSqFtDisplay())
                                .append(" x ").append(item.getNos())
                                .append(" x ").append(item.getRate())
                                .append(" = ").append(UiFormat.currency(item.getAmount()))
                                .append("\n");
                    }
                    sb.append("  Area Total: ").append(UiFormat.currency(area.getTotal())).append("\n");
                }
                sb.append("Date Total: ").append(UiFormat.currency(entry.getDateTotal())).append("\n");
                sb.append("--------------------------------\n");
            }
            detailsArea.setText(sb.toString());
        } catch (Exception e) {
            detailsArea.setText("Error loading details: " + e.getMessage());
        }
    }

    private void openSelectedPdf(boolean print) {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getPdfPath() == null) {
            Dialogs.showInfo("No PDF", "Select a bill with a PDF file.");
            return;
        }
        Path path = Path.of(selected.getPdfPath());
        if (!Files.exists(path)) {
            Dialogs.showError("Not Found", "PDF file not found: " + path);
            return;
        }
        try {
            if (print) {
                Desktop.getDesktop().print(path.toFile());
            } else {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (Exception e) {
            Dialogs.showError("Error", e.getMessage());
        }
    }

    private void printSelectedPdf() {
        openSelectedPdf(true);
    }

    private void deleteBill() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        BillActionHandler.deleteBill(selected, this::refresh);
    }

    private void updateRecords() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        BillActionHandler.updateBillRecords(selected, openWorkEntry, this::refresh);
    }

    public BorderPane getContent() {
        return content;
    }
}
