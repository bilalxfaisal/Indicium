package com.indicium.ui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashBoardController extends BorderPane {

    // ── Top nav ──
    @FXML private Button navDashboard;
    @FXML private Button navNotes;
    @FXML private Button navVideos;
    @FXML private Button navTools;
    @FXML private Button navForum;

    // ── Sidebar ──
    @FXML private VBox   sideNavBar;
    @FXML private Button sideHome;
    @FXML private Button sideCases;
    @FXML private Button sideEvidence;
    @FXML private Button sideTimeline;
    @FXML private Button sideAuditLog;
    @FXML private Button sideReport;
    @FXML private Button sideSettings;
    @FXML private Button sideIntegrity;
    @FXML private Button sideUserMgr;

    // ── Menu toggle ──
    @FXML private Button btnMenuToggle;

    private List<Button> topNavButtons;
    private List<Button> sideNavButtons;

    // ── Sidebar labels — stored for restore on expand ──
    private Map<Button, String> sideLabels;

    // ── Sidebar state ──
    private boolean sidebarExpanded    = true;
    private static final double EXPANDED   = 210.0;
    private static final double COLLAPSED  = 56.0;
    private static final double ANIM_MS    = 180.0;

    // ── Constructor ──
    public DashBoardController() {
        URL fxmlUrl = getClass().getResource("/com/indicium/ui/DashBoard.fxml");
        if (fxmlUrl == null)
            throw new RuntimeException(
                    "DashBoard.fxml not found! Check src/main/resources/com/indicium/ui/"
            );

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
    public void initialize()
    {
        topNavButtons = List.of(navDashboard, navNotes, navVideos, navTools, navForum);
        sideNavButtons = List.of(sideHome, sideCases, sideEvidence, sideTimeline,
                sideAuditLog, sideReport, sideSettings, sideIntegrity, sideUserMgr);

        // Store original labels so we can restore them after expand
        sideLabels = new LinkedHashMap<>();
        sideNavButtons.forEach(btn -> sideLabels.put(btn, btn.getText()));

        setActiveTopNav(navDashboard);
        setActiveSideNav(sideHome);
        navigateTo(new HomeController());
    }

    // ── Core swap ──
    private void navigateTo(Node target) {
        this.setCenter(target);
    }

    // ══════════════════════════════════════════
    //  MENU TOGGLE
    // ══════════════════════════════════════════

    @FXML
    private void handleMenuToggle() {
        if (sidebarExpanded) {
            collapseSidebar();
        } else {
            expandSidebar();
        }
        sidebarExpanded = !sidebarExpanded;
    }

    private void collapseSidebar() {
        // Clear text immediately — icons stay because they're graphics, not text
        sideNavButtons.forEach(btn -> btn.setText(""));
        sideNavBar.getStyleClass().add("side-nav-bar-collapsed");

        animateSidebar(EXPANDED, COLLAPSED);
    }

    private void expandSidebar() {
        sideNavBar.getStyleClass().remove("side-nav-bar-collapsed");
        animateSidebar(COLLAPSED, EXPANDED);

        // Restore text only after the animation finishes so it doesn't
        // flash in while the sidebar is still narrow
        PauseTransition wait = new PauseTransition(Duration.millis(ANIM_MS));
        wait.setOnFinished(e ->
                sideNavButtons.forEach(btn -> btn.setText(sideLabels.get(btn)))
        );
        wait.play();
    }

    private void animateSidebar(double from, double to) {
        Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,   new KeyValue(sideNavBar.prefWidthProperty() , from, Interpolator.EASE_BOTH) ),
                new KeyFrame(Duration.millis(ANIM_MS), new KeyValue(sideNavBar.prefWidthProperty(), to, Interpolator.EASE_BOTH))
        );
        anim.play();
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

    @FXML private void handleNavNotes()  { setActiveTopNav(navNotes);  /* TODO */ }
    @FXML private void handleNavVideos() { setActiveTopNav(navVideos); /* TODO */ }
    @FXML private void handleNavTools()  { setActiveTopNav(navTools);  /* TODO */ }
    @FXML private void handleNavForum()  { setActiveTopNav(navForum);  /* TODO */ }

    // ══════════════════════════════════════════
    //  SIDEBAR HANDLERS
    // ══════════════════════════════════════════

    @FXML
    private void handleSideCases() {
        setActiveSideNav(sideCases);
        navigateTo(new CaseDashBoardController());
    }

    @FXML
    private void handleSideEvidence() {
        setActiveSideNav(sideEvidence);
        navigateTo(new EvidenceDashBoardController());
    }

    @FXML
    private void handleSideTimeline() {
        setActiveSideNav(sideTimeline);
        navigateTo(new TimelineController());
    }

    // Navigate to timeline pre-loaded with a specific case
    // Called from CaseDashBoardController overflow menu
    public void navigateToTimeline(String caseId) {
        setActiveSideNav(sideTimeline);
        navigateTo(new TimelineController(caseId));
    }

    @FXML private void handleSideAuditLog()    { setActiveSideNav(sideAuditLog);  /* TODO */ }
    @FXML private void handleSideReport()      { setActiveSideNav(sideReport);    /* TODO */ }
    @FXML private void handleSideSettings()    { setActiveSideNav(sideSettings);  /* TODO */ }
    @FXML private void handleSideIntegrity()   { setActiveSideNav(sideIntegrity); /* TODO */ }
    @FXML private void handleSideUserManager() { setActiveSideNav(sideUserMgr);   /* TODO */ }

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
