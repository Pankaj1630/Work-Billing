package com.workbilling.ui.util;

import com.workbilling.model.Bill;
import com.workbilling.model.WorkEntry;
import com.workbilling.service.BillingService;
import com.workbilling.util.AppPaths;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.LongConsumer;

public final class BillActionHandler {
    private static final BillingService billingService = new BillingService();

    private BillActionHandler() {
    }

    public static void deleteBill(Bill bill, Runnable onSuccess) {
        if (bill == null) {
            Dialogs.showInfo("Select Bill", "Please select a bill first.");
            return;
        }

        String message = "Bill: " + bill.getBillNumber() + "\n" +
                "Company: " + bill.getCompanyName() + "\n" +
                "Period: " + bill.getFromDate().format(AppPaths.DISPLAY_DATE) +
                " to " + bill.getToDate().format(AppPaths.DISPLAY_DATE) + "\n" +
                "Total: " + UiFormat.currency(bill.getGrandTotal()) + "\n\n" +
                "This will permanently remove the bill from history and unlock all linked work entries.\n" +
                "The PDF file on disk will NOT be deleted.\n\n" +
                "Are you sure you want to delete this bill?";

        if (!Dialogs.confirmWarning("Delete Bill — Confirm",
                "This action cannot be undone", message)) {
            return;
        }

        try {
            billingService.deleteBill(bill.getId());
            Dialogs.showInfo("Deleted", "Bill " + bill.getBillNumber() + " has been deleted. Work entries are unlocked for editing.");
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (Exception e) {
            Dialogs.showError("Delete Failed", e.getMessage());
        }
    }

    public static void updateBillRecords(Bill bill, LongConsumer openEntry, Runnable onSuccess) {
        if (bill == null) {
            Dialogs.showInfo("Select Bill", "Please select a bill first.");
            return;
        }

        String message = "Bill: " + bill.getBillNumber() + "\n" +
                "Company: " + bill.getCompanyName() + "\n" +
                "Period: " + bill.getFromDate().format(AppPaths.DISPLAY_DATE) +
                " to " + bill.getToDate().format(AppPaths.DISPLAY_DATE) + "\n\n" +
                "This will unlock work records from this bill so you can edit them.\n" +
                "The bill will be marked as Cancelled.\n" +
                "After editing, generate a new bill with a new bill number.\n\n" +
                "Do you want to proceed?";

        if (!Dialogs.confirmWarning("Update Records — Confirm",
                "You are about to modify billed records", message)) {
            return;
        }

        try {
            List<WorkEntry> entries = billingService.unlockBillForUpdate(bill.getId());
            if (onSuccess != null) {
                onSuccess.run();
            }
            showEntryPicker(bill.getBillNumber(), entries, openEntry);
        } catch (Exception e) {
            Dialogs.showError("Update Failed", e.getMessage());
        }
    }

    private static void showEntryPicker(String billNumber, List<WorkEntry> entries, LongConsumer openEntry) {
        if (entries.isEmpty()) {
            Dialogs.showInfo("No Entries", "No work entries found for this bill.");
            return;
        }

        Dialog<WorkEntry> dialog = new Dialog<>();
        dialog.setTitle("Select Record to Edit");
        dialog.setHeaderText("Bill " + billNumber + " — choose a work entry to open");

        ListView<WorkEntry> listView = new ListView<>();
        listView.setItems(javafx.collections.FXCollections.observableArrayList(entries));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WorkEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getWorkDate().format(AppPaths.DISPLAY_DATE) +
                            " — " + item.getCompanyName() +
                            " — " + UiFormat.currency(item.getDateTotal()));
                }
            }
        });
        listView.setPrefHeight(200);
        listView.setPrefWidth(420);

        if (!entries.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }

        VBox box = new VBox(10, new Label("Double-click or select and press Open:"), listView);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                WorkEntry selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    dialog.setResult(selected);
                }
            }
        });

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? listView.getSelectionModel().getSelectedItem() : null);
        dialog.showAndWait().ifPresent(entry -> {
            if (openEntry != null) {
                openEntry.accept(entry.getId());
            }
        });
    }
}
