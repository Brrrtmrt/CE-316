module com.iae {
    // JavaFX dependencies
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    
    // External dependencies
    requires com.google.gson;

    // Open controllers so FXMLLoader can inject @FXML fields
    opens com.iae.gui.controllers to javafx.fxml;
    
    // Open domain to javafx.base so TableView PropertyValueFactory can read the fields
    opens com.iae.domain to javafx.base;

    // Export primary packages
    exports com.iae.gui;
    exports com.iae.domain;
    exports com.iae.service;
}