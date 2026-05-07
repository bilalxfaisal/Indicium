package com.indicium.ui;

import com.indicium.models.SystemUser;
import com.indicium.repository.CaseRepository;
import com.indicium.services.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HomeController extends AnchorPane {

    @FXML private Label welcomeLabel;
    @FXML private Label statCases;
    @FXML private Label statEvidence;
    @FXML private Label statTimeline;
    @FXML private Label statAudit;
    @FXML private VBox  recentActivityBox;

    public HomeController() {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/indicium/ui/HomeBoard.fxml")
        );
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load HomeBoard.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void initialize() {
        loadWelcome();
        loadStats();
    }

    // ── Pull first name from session ──
    private void loadWelcome() {
        SystemUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            welcomeLabel.setText("Welcome back!");
            return;
        }

        String fullName  = user.getName() != null ? user.getName().trim() : "";
        String firstName = fullName.contains(" ")
                ? fullName.substring(0, fullName.indexOf(' '))
                : fullName;

        welcomeLabel.setText("Welcome back, " +
                (firstName.isEmpty() ? "User" : firstName) + "!");
    }

    // ── Load real stats from DB ──
    private void loadStats() {
        setStat(statCases,    CaseRepository.countActiveCases());
        setStat(statEvidence, CaseRepository.countEvidenceItems());
        setStat(statTimeline, CaseRepository.countTimelineEvents());
        setStat(statAudit,    CaseRepository.countAuditEntries());
    }

    // Safe setter — shows "—" if query returned 0 due to an error
    private void setStat(Label label, int value) {
        label.setText(String.valueOf(value));
    }
}
