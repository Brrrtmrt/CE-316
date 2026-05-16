package com.iae.gui.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private Button btnCreateAssessment;
    @FXML private Button btnConfigSettings;
    @FXML private Button btnProjectResults;
    
    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        
        btnCreateAssessment.setOnAction(e -> loadScreen("/fxml/project.fxml"));
        
        btnConfigSettings.setOnAction(e -> loadScreen("/fxml/configuration.fxml"));
        
        btnProjectResults.setOnAction(e -> loadScreen("/fxml/Results.fxml"));
    }

    private void loadScreen(String fxmlPath) {
        try {
            Parent screen = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(screen); 
        } catch (IOException e) {
            System.out.println("An error occurred while loading the screen." + fxmlPath);
            e.printStackTrace();
        }
    }
}