module com.iae {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.google.gson;
    requires javafx.graphics;

    opens com.iae.gui to javafx.fxml;
    opens com.iae.gui.controllers to javafx.fxml;

    opens com.iae.domain to javafx.base;
    
    exports com.iae.gui;
    exports com.iae.gui.controllers;
    exports com.iae.domain;
    exports com.iae.service;
    exports com.iae.evaluation;
    exports com.iae.evaluation.steps;
    exports com.iae.evaluation.strategies;
}