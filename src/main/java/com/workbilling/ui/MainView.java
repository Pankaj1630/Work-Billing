package com.workbilling.ui;

import com.workbilling.ui.tabs.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.function.LongConsumer;

public class MainView {
    private final BorderPane root = new BorderPane();
    private final TabPane tabPane = new TabPane();
    private final ThemeManager themeManager = new ThemeManager();
    private final DashboardTab dashboardTab;
    private final WorkEntryTab workEntryTab;
    private final SearchTab searchTab;
    private final BillingTab billingTab;
    private final BillHistoryTab billHistoryTab;
    private final SettingsTab settingsTab;

    public MainView(Stage stage) {
        dashboardTab = new DashboardTab();
        workEntryTab = new WorkEntryTab();

        LongConsumer openWorkEntry = entryId -> {
            workEntryTab.loadEntry(entryId);
            tabPane.getSelectionModel().select(1);
        };

        searchTab = new SearchTab(openWorkEntry);
        billingTab = new BillingTab(stage, openWorkEntry);
        billHistoryTab = new BillHistoryTab(stage, openWorkEntry);
        settingsTab = new SettingsTab();

        tabPane.getTabs().addAll(
                createTab("Dashboard", dashboardTab.getContent()),
                createTab("Work Entry", workEntryTab.getContent()),
                createTab("Search", searchTab.getContent()),
                createTab("Generate Bill", billingTab.getContent()),
                createTab("Bill History", billHistoryTab.getContent()),
                createTab("Settings", settingsTab.getContent())
        );
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "Dashboard".equals(newTab.getText())) {
                dashboardTab.refresh();
            }
        });

        root.setCenter(tabPane);
        root.setTop(buildThemeBar());
    }

    private HBox buildThemeBar() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("theme-bar");
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(6, 12, 6, 12));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleButton darkModeBtn = new ToggleButton("Dark Mode");
        darkModeBtn.getStyleClass().add("theme-toggle");
        darkModeBtn.setSelected(themeManager.isDarkMode());
        darkModeBtn.selectedProperty().addListener((obs, oldVal, selected) ->
                themeManager.applyTheme(selected));

        bar.getChildren().addAll(spacer, darkModeBtn);
        return bar;
    }

    public void initializeTheme(javafx.scene.Scene scene) {
        themeManager.bindScene(scene);
        themeManager.applySavedTheme();
    }

    private Tab createTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public BorderPane getRoot() {
        return root;
    }
}
