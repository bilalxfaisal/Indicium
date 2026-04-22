package com.indicium.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;


import java.io.IOException;

public class HomeController extends AnchorPane {

    @FXML
    private Label welcomeLabel;
    @FXML private Label statCases;
    @FXML private Label statEvidence;
    @FXML private Label statTimeline;
    @FXML private Label statAudit;
    @FXML private VBox recentActivityBox;

    public HomeController() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/indicium/ui/Home.fxml")
        );
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Home.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        statCases.setText("12");
        statEvidence.setText("47");
        statTimeline.setText("8");
        statAudit.setText("67");
    }
}
