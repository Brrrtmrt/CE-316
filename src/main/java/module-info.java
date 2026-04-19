module com.iae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.iae.gui to javafx.fxml;
    opens com.iae.gui.controllers to javafx.fxml;
    
    exports com.iae;
    exports com.iae.gui;
    exports com.iae.gui.controllers;
    exports com.iae.domain;
}