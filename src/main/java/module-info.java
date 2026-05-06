module com.indicium {
    // Libraries
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires mysql.connector.j;
    requires itextpdf;

    // JavaFX needs reflective access to ALL controller/UI packages
    opens com.indicium.ui to javafx.fxml;
    opens com.indicium.models to javafx.base;     // ← for ObservableList / PropertyValueFactory
    opens com.indicium.services to javafx.fxml;   // ← if any service is referenced in FXML

    // Exports
    exports com.indicium;
    exports com.indicium.ui;
    exports com.indicium.services;
    exports com.indicium.models;                  // ← add this
    exports com.indicium.repository;              // ← add this
}
