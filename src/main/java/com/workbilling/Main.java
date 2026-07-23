package com.workbilling;

import com.workbilling.db.DatabaseManager;
import com.workbilling.ui.MainView;
import com.workbilling.util.AppPaths;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            AppPaths.init();
            DatabaseManager.initialize();

            MainView mainView = new MainView(primaryStage);
            Scene scene = new Scene(mainView.getRoot(), 1200, 800);
            var css = getClass().getResource("/css/app.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            mainView.initializeTheme(scene);

            primaryStage.setTitle("Work Management & Billing");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            javafx.application.Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
