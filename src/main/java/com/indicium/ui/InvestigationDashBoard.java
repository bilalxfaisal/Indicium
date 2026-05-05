package com.indicium.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class InvestigationDashBoard extends Application {

    @Override
    public void start(Stage primaryStage) {

        // DashBoardController loads DashBoard.fxml internally in its constructor
        // DO NOT use a second FXMLLoader here — that causes the double-load crash
        DashBoardController dashboard = new DashBoardController();

        // Wrap in StackPane so the search overlay sits on top of everything
        StackPane root = new StackPane(dashboard, dashboard.getGlobalSearch());

        Scene scene = new Scene(root, 1000, 650);

        primaryStage.setTitle("INDICIUM");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(550);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
