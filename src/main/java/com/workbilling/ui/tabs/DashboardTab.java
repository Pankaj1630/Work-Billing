package com.workbilling.ui.tabs;

import com.workbilling.model.DashboardSummary;
import com.workbilling.service.DashboardService;
import com.workbilling.ui.util.Dialogs;
import com.workbilling.ui.util.UiFormat;
import com.workbilling.util.AppPaths;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;

public class DashboardTab {
    private final DashboardService dashboardService = new DashboardService();
    private final BorderPane content = new BorderPane();

    private final DatePicker fromDate = new DatePicker(DashboardService.defaultFromDate());
    private final DatePicker toDate = new DatePicker(DashboardService.defaultToDate());
    private final Label periodLabel = new Label();
    private final Label company1NameLabel = new Label();
    private final Label company1BilledLabel = new Label();
    private final Label company1PendingLabel = new Label();
    private final Label company1TotalLabel = new Label();
    private final Label company2NameLabel = new Label();
    private final Label company2BilledLabel = new Label();
    private final Label company2PendingLabel = new Label();
    private final Label company2TotalLabel = new Label();
    private final Label billedGrandTotalLabel = new Label();
    private final Label pendingGrandTotalLabel = new Label();
    private final Label monthGrandTotalLabel = new Label();
    private final Label unbilledLabel = new Label();
    private final Label lastBillLabel = new Label();

    public DashboardTab() {
        buildUi();
        refresh();
    }

    private void buildUi() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        header.getChildren().add(title);

        HBox dateFilter = new HBox(10);
        dateFilter.setAlignment(Pos.CENTER_LEFT);
        fromDate.setPrefWidth(140);
        toDate.setPrefWidth(140);

        Button thisMonthBtn = new Button("This Month");
        Button lastMonthBtn = new Button("Last Month");
        Button applyBtn = new Button("Show Totals");

        thisMonthBtn.setOnAction(e -> setThisMonth());
        lastMonthBtn.setOnAction(e -> setLastMonth());
        applyBtn.setOnAction(e -> refresh());

        fromDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && toDate.getValue() != null && !newVal.equals(oldVal)) {
                refresh();
            }
        });
        toDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && fromDate.getValue() != null && !newVal.equals(oldVal)) {
                refresh();
            }
        });

        dateFilter.getChildren().addAll(
                new Label("From:"), fromDate,
                new Label("To:"), toDate,
                thisMonthBtn, lastMonthBtn, applyBtn
        );

        periodLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

        VBox totalsCard = createCard("Period Totals");
        GridPane monthGrid = new GridPane();
        monthGrid.setHgap(16);
        monthGrid.setVgap(8);
        monthGrid.add(new Label("Company"), 0, 0);
        monthGrid.add(new Label("Billed (Bill History)"), 1, 0);
        monthGrid.add(new Label("Pending (Generate Bill)"), 2, 0);
        monthGrid.add(new Label("Total"), 3, 0);
        monthGrid.add(company1NameLabel, 0, 1);
        monthGrid.add(company1BilledLabel, 1, 1);
        monthGrid.add(company1PendingLabel, 2, 1);
        monthGrid.add(company1TotalLabel, 3, 1);
        monthGrid.add(company2NameLabel, 0, 2);
        monthGrid.add(company2BilledLabel, 1, 2);
        monthGrid.add(company2PendingLabel, 2, 2);
        monthGrid.add(company2TotalLabel, 3, 2);
        monthGrid.add(new Label("Combined"), 0, 3);
        monthGrid.add(billedGrandTotalLabel, 1, 3);
        monthGrid.add(pendingGrandTotalLabel, 2, 3);
        monthGrid.add(monthGrandTotalLabel, 3, 3);
        company1TotalLabel.setStyle("-fx-font-weight: bold;");
        company2TotalLabel.setStyle("-fx-font-weight: bold;");
        monthGrandTotalLabel.setStyle("-fx-font-weight: bold;");
        totalsCard.getChildren().add(monthGrid);

        VBox unbilledCard = createCard("Ready to Bill (in selected range)");
        unbilledLabel.setWrapText(true);
        unbilledCard.getChildren().add(unbilledLabel);

        VBox lastBillCard = createCard("Last Bill");
        lastBillLabel.setWrapText(true);
        lastBillCard.getChildren().add(lastBillLabel);

        root.getChildren().addAll(header, dateFilter, periodLabel, totalsCard, unbilledCard, lastBillCard);
        content.setCenter(root);
    }

    private void setThisMonth() {
        YearMonth month = YearMonth.now();
        fromDate.setValue(month.atDay(1));
        toDate.setValue(month.atEndOfMonth());
        refresh();
    }

    private void setLastMonth() {
        YearMonth month = YearMonth.now().minusMonths(1);
        fromDate.setValue(month.atDay(1));
        toDate.setValue(month.atEndOfMonth());
        refresh();
    }

    private VBox createCard(String title) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-card");
        card.setPadding(new Insets(16));
        Label cardTitle = new Label(title);
        cardTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        card.getChildren().add(cardTitle);
        return card;
    }

    public void refresh() {
        try {
            LocalDate from = fromDate.getValue();
            LocalDate to = toDate.getValue();
            if (from == null || to == null) {
                Dialogs.showError("Validation", "Please select both From and To dates.");
                return;
            }
            if (from.isAfter(to)) {
                Dialogs.showError("Validation", "From date cannot be after To date.");
                return;
            }

            DashboardSummary summary = dashboardService.loadSummary(from, to);
            periodLabel.setText("Showing: " + summary.getPeriodLabel());

            company1NameLabel.setText(summary.getCompany1Name());
            company1BilledLabel.setText(UiFormat.currency(summary.getCompany1BilledTotal()));
            company1PendingLabel.setText(UiFormat.currency(summary.getCompany1PendingTotal()));
            company1TotalLabel.setText(UiFormat.currency(summary.getCompany1MonthTotal()));
            company2NameLabel.setText(summary.getCompany2Name());
            company2BilledLabel.setText(UiFormat.currency(summary.getCompany2BilledTotal()));
            company2PendingLabel.setText(UiFormat.currency(summary.getCompany2PendingTotal()));
            company2TotalLabel.setText(UiFormat.currency(summary.getCompany2MonthTotal()));
            billedGrandTotalLabel.setText(UiFormat.currency(summary.getBilledGrandTotal()));
            pendingGrandTotalLabel.setText(UiFormat.currency(summary.getPendingGrandTotal()));
            monthGrandTotalLabel.setText(UiFormat.currency(summary.getMonthGrandTotal()));

            if (summary.getUnbilledSavedCount() == 0) {
                unbilledLabel.setText("No saved entries waiting to be billed in this date range.");
            } else {
                unbilledLabel.setText(summary.getUnbilledSavedCount() + " saved " +
                        (summary.getUnbilledSavedCount() == 1 ? "entry" : "entries") +
                        " not yet billed — Total: " + UiFormat.currency(summary.getUnbilledSavedTotal()));
            }

            if (summary.isHasLastBill()) {
                String dateText = summary.getLastBillDate() != null
                        ? summary.getLastBillDate().format(AppPaths.DISPLAY_DATE)
                        : "—";
                lastBillLabel.setText(
                        summary.getLastBillNumber() + "  |  " + summary.getLastBillCompany() +
                        "\nDate: " + dateText +
                        "  |  Total: " + UiFormat.currency(summary.getLastBillTotal()));
            } else {
                lastBillLabel.setText("No bills generated yet.");
            }
        } catch (Exception e) {
            Dialogs.showError("Dashboard Error", e.getMessage());
        }
    }

    public BorderPane getContent() {
        return content;
    }
}
