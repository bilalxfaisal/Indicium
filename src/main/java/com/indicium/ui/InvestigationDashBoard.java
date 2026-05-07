package com.indicium.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class InvestigationDashBoard extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Always boot into Login first
        Parent loginRoot = FXMLLoader.load(
                getClass().getResource("/com/indicium/ui/Login.fxml")
        );

        Scene scene = new Scene(loginRoot);
        primaryStage.setTitle("INDICIUM - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/com/indicium/ui/Assets/Indicium_Icon.png"))
        );
    }

    /**
     * Called by LoginController after successful authentication.
     * Swaps the Login scene out and loads the full Dashboard.
     */
    public static void launchDashboard(Stage stage) {
        try {
            DashBoardController dashboard = new DashBoardController();

            // Wrap in StackPane so the search overlay sits on top
            javafx.scene.layout.StackPane root =
                    new javafx.scene.layout.StackPane(dashboard, dashboard.getGlobalSearch());

            Scene scene = new Scene(root, 1000, 650);
            stage.setTitle("INDICIUM");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(550);
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {
            throw new RuntimeException("Failed to launch dashboard: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
