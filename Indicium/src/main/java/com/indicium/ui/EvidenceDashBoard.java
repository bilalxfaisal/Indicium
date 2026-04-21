package com.indicium.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class EvidenceDashBoard extends Application {

    @Override
    public void start(Stage stage) {
        Button btn = new Button("JavaFX is Working! ✅");
        btn.setOnAction(e -> System.out.println("Button clicked!"));

        StackPane root = new StackPane(btn);
        Scene scene = new Scene(root, 400, 250);

        stage.setTitle("JavaFX Test");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

