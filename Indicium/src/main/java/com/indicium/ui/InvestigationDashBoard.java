package com.indicium.ui;
import com.indicium.ui.DashBoardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class InvestigationDashBoard extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Load the FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/indicium/ui/DashBoard.fxml")
        );

        // Since DashBoard.fxml uses fx:root, we must set the root + controller manually
        DashBoardController controller = new DashBoardController();
        loader.setController(controller);
        loader.setRoot(controller);          // fx:root requires this

        // Load it
        javafx.scene.layout.BorderPane root = loader.load();

        // Optional: pass the logged-in username TO BE DONE MY DB SHI
        controller.setUsername("BEE");

        // Build the scene
        Scene scene = new Scene(root, 1000, 650);

        // Stage config
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