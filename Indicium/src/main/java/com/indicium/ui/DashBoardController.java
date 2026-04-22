package com.indicium.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import com.indicium.ui.CaseDashBoardController;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class DashBoardController extends BorderPane {  //  extends BorderPane

    // ── Top nav ──
    @FXML private Button navDashboard;
    @FXML private Button navNotes;
    @FXML private Button navVideos;
    @FXML private Button navTools;
    @FXML private Button navForum;

    // ── Sidebar ──
    @FXML private Button sideHome;
    @FXML private Button sideCases;
    @FXML private Button sideEvidence;
    @FXML private Button sideTimeline;
    @FXML private Button sideAuditLog;
    @FXML private Button sideReport;
    @FXML private Button sideSettings;
    @FXML private Button sideIntegrity;
    @FXML private Button sideUserMgr;

    // ── Content ──
    @FXML private Label welcomeLabel;
    @FXML private Label statCases;
    @FXML private Label statEvidence;
    @FXML private Label statTimeline;
    @FXML private Label statAudit;
    @FXML private VBox  recentActivityBox;

    private List<Button> topNavButtons;
    private List<Button> sideNavButtons;

    //  Constructor handles FXML loading
    public DashBoardController() {
        URL fxmlUrl = getClass().getResource("/com/indicium/ui/DashBoard.fxml");

        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "DashBoard.fxml not found! Check src/main/resources/com/indicium/ui/"
            );
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);        // this IS the BorderPane
        loader.setController(this);  // this IS the controller

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DashBoard.fxml: " + e.getMessage(), e);
        }
    }

    // Called automatically after FXML fields are injected
    @FXML
    public void initialize() {
        topNavButtons  = List.of(navDashboard, navNotes, navVideos, navTools, navForum);
        sideNavButtons = List.of(sideHome, sideCases, sideEvidence, sideTimeline,
                sideAuditLog, sideReport, sideSettings,
                sideIntegrity, sideUserMgr);

        setActiveTopNav(navDashboard);
        setActiveSideNav(sideHome);
        loadStats();
    }

    // ── Top Nav Handlers ──
    @FXML private void handleNavDashboard() { setActiveTopNav(navDashboard); setActiveSideNav(sideHome); }
    @FXML private void handleNavNotes()     { setActiveTopNav(navNotes); }
    @FXML private void handleNavVideos()    { setActiveTopNav(navVideos); }
    @FXML private void handleNavTools()     { setActiveTopNav(navTools); }
    @FXML private void handleNavForum()     { setActiveTopNav(navForum); }

    // ── Sidebar Handlers ──
    @FXML private void handleSideCases()
    { setActiveSideNav(sideCases);
        CaseDashBoardController caseDCon = new CaseDashBoardController();
        navigateTo(caseDCon);

    }
    @FXML private void handleSideEvidence()    { setActiveSideNav(sideEvidence); }
    @FXML private void handleSideTimeline()    { setActiveSideNav(sideTimeline); }
    @FXML private void handleSideAuditLog()    { setActiveSideNav(sideAuditLog); }
    @FXML private void handleSideReport()      { setActiveSideNav(sideReport); }
    @FXML private void handleSideSettings()    { setActiveSideNav(sideSettings); }
    @FXML private void handleSideIntegrity()   { setActiveSideNav(sideIntegrity); }
    @FXML private void handleSideUserManager() { setActiveSideNav(sideUserMgr); }

    // ── Active State Helpers ──
    private void setActiveTopNav(Button target) {
        topNavButtons.forEach(b -> b.getStyleClass().remove("active"));
        target.getStyleClass().add("active");
    }

    private void setActiveSideNav(Button target) {
        sideNavButtons.forEach(b -> b.getStyleClass().remove("side-active"));
        target.getStyleClass().add("side-active");
    }

    // ── Stats ──
    private void loadStats() {
        statCases.setText("12");
        statEvidence.setText("47");
        statTimeline.setText("8");
        statAudit.setText("67");
    }

    public void setUsername(String name) {
        welcomeLabel.setText("Welcome back, " + name + "!");
    }
    private void navigateTo(Node target)
    {
        this.setCenter(target);
    }
}
