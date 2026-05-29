package com.iae.gui;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

        URL cssLocation = getClass().getResource("/css/styles.css");
        if (cssLocation != null) {
            scene.getStylesheets().add(cssLocation.toExternalForm());
        } else {
            System.out.println("Warning: /css/styles.css not found! Make sure the file exists in src/main/resources/css/");
        }

        // Configure Stage
        primaryStage.setTitle("Integrated Assignment Environment (IAE)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Force completely close the application and all background threads
        primaryStage.setOnCloseRequest(event -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}