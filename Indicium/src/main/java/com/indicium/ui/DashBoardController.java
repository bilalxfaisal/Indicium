package com.indicium.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class DashBoardController extends BorderPane {

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

    private List<Button> topNavButtons;
    private List<Button> sideNavButtons;

    // ── Constructor ──
    public DashBoardController() {
        URL fxmlUrl = getClass().getResource("/com/indicium/ui/DashBoard.fxml");

        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "DashBoard.fxml not found! Check src/main/resources/com/indicium/ui/"
            );
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DashBoard.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void initialize() {
        topNavButtons  = List.of(navDashboard, navNotes, navVideos, navTools, navForum);
        sideNavButtons = List.of(sideHome, sideCases, sideEvidence, sideTimeline,
                sideAuditLog, sideReport, sideSettings,
                sideIntegrity, sideUserMgr);

        setActiveTopNav(navDashboard);
        setActiveSideNav(sideHome);

        // ── Load home screen as default center on startup ──
        navigateTo(new HomeController());
    }

    // ── Core swap method ──
    private void navigateTo(Node target) {
        this.setCenter(target);
    }

    // ══════════════════════════════════════════
    //  TOP NAV HANDLERS
    // ══════════════════════════════════════════

    @FXML
    private void handleNavDashboard() {
        setActiveTopNav(navDashboard);
        setActiveSideNav(sideHome);
        navigateTo(new HomeController());
    }

    @FXML private void handleNavNotes()  { setActiveTopNav(navNotes);  /* TODO: NotesController */ }
    @FXML private void handleNavVideos() { setActiveTopNav(navVideos); /* TODO: SearchController */ }
    @FXML private void handleNavTools()  { setActiveTopNav(navTools);  /* TODO: ToolsController */ }
    @FXML private void handleNavForum()  { setActiveTopNav(navForum);  /* TODO: NotificationsController */ }

    // ══════════════════════════════════════════
    //  SIDEBAR HANDLERS
    // ══════════════════════════════════════════

    @FXML
    private void handleSideCases() {
        setActiveSideNav(sideCases);
        navigateTo(new CaseDashBoardController());
    }

    @FXML private void handleSideEvidence()
    { setActiveSideNav(sideEvidence);
       navigateTo(new EvidenceDashBoardController());
    }
    // ── TODO stubs ──
    @FXML private void handleSideTimeline()
    {
        setActiveSideNav(sideTimeline);
        this.navigateTo(new TimelineController());
    }
    @FXML private void handleSideAuditLog()    { setActiveSideNav(sideAuditLog);  /* TODO: AuditController */ }
    @FXML private void handleSideReport()      { setActiveSideNav(sideReport);    /* TODO: ReportController */ }
    @FXML private void handleSideSettings()    { setActiveSideNav(sideSettings);  /* TODO: SettingsController */ }
    @FXML private void handleSideIntegrity()   { setActiveSideNav(sideIntegrity); /* TODO: IntegrityController */ }
    @FXML private void handleSideUserManager() { setActiveSideNav(sideUserMgr);   /* TODO: UserManagerController */ }

    // ── Active state helpers ──
    private void setActiveTopNav(Button target) {
        topNavButtons.forEach(b -> b.getStyleClass().remove("active"));
        target.getStyleClass().add("active");
    }

    private void setActiveSideNav(Button target) {
        sideNavButtons.forEach(b -> b.getStyleClass().remove("side-active"));
        target.getStyleClass().add("side-active");
    }
}
