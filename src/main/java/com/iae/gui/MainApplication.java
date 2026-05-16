package com.iae.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main entry point for the Integrated Assignment Environment (IAE).
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        URL fxmlLocation = getClass().getResource("/fxml/main.fxml");
        
        // Fallback for testing if main isn't ready yet
        if (fxmlLocation == null) {
            fxmlLocation = getClass().getResource("/fxml/Results.fxml");
            System.out.println("Warning: main.fxml not found, loading Results.fxml directly.");
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        // Setup the primary Scene
        Scene scene = new Scene(root, 1000, 700);

        // Configure Stage
        primaryStage.setTitle("Integrated Assignment Environment (IAE)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}