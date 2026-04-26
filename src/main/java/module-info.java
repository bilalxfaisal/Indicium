module com.indicium {
    // These tell Java which libraries we are using
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.j;

    // This allows JavaFX to "see" your UI classes to render them
    opens com.indicium.ui to javafx.fxml;

    // This allows other parts of the system to access your code
    exports com.indicium;
    exports com.indicium.ui;
    exports com.indicium.services;
}