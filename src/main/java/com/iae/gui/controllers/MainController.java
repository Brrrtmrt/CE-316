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

    private ResultsController resultsController;

    @FXML
    public void initialize() {
        
        btnCreateAssessment.setOnAction(e -> loadScreen("/fxml/Project.fxml"));
        
        btnConfigSettings.setOnAction(e -> loadScreen("/fxml/Configuration.fxml"));
        
        btnProjectResults.setOnAction(e -> loadScreen("/fxml/Results.fxml"));
    }

    private void loadScreen(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent screen = loader.load();
            
            if (fxmlPath.contains("Results.fxml")) {
                resultsController = loader.getController();
            } else if (fxmlPath.contains("Project.fxml")) {
                ProjectController projectController = loader.getController();
                projectController.setMainController(this);
            }
            
            contentArea.getChildren().clear();
            contentArea.getChildren().add(screen); 
        } catch (IOException e) {
            System.out.println("An error occurred while loading the screen." + fxmlPath);
            e.printStackTrace();
        }
    }
    
    public ResultsController getResultsController() {
        return resultsController;
    }
}