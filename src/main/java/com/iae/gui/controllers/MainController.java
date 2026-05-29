package com.iae.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.iae.domain.Configuration;
import com.iae.service.ConfigurationManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

public class MainController {

    private static MainController instance;
    @FXML private MenuItem menuItemUserManual;
    @FXML private MenuItem menuItemExport;
    @FXML private MenuItem menuItemImport;

    @FXML private Button btnCreateAssessment;
    @FXML private Button btnConfigSettings;
    @FXML private Button btnProjectResults;
    
    @FXML private StackPane contentArea;

    private final Map<String, FXMLLoader> screenCache = new HashMap<>();

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        
        btnCreateAssessment.setOnAction(e -> activateScreen("/fxml/Project.fxml"));
        btnConfigSettings.setOnAction(e -> activateScreen("/fxml/Configuration.fxml"));
        btnProjectResults.setOnAction(e -> activateScreen("/fxml/Results.fxml"));
        if (menuItemExport != null) {
            menuItemExport.setOnAction(e -> handleExport());
        }
        if (menuItemImport != null) {
            menuItemImport.setOnAction(e -> handleImport());
        }
        if (menuItemUserManual != null) {
            menuItemUserManual.setOnAction(e -> openUserManual());
        }
           
    }

    public FXMLLoader loadOrGetScreen(String fxmlPath) {
        if (!screenCache.containsKey(fxmlPath)) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.load(); 
                screenCache.put(fxmlPath, loader);
            } catch (IOException e) {
                System.out.println("An error occurred while loading the screen: " + fxmlPath);
                e.printStackTrace();
                return null;
            }
        }
        return screenCache.get(fxmlPath);
    }

    public void activateScreen(String fxmlPath) {
        FXMLLoader loader = loadOrGetScreen(fxmlPath);
        if (loader != null && contentArea != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.getRoot());
        }
    }

    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Configuration");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        
        File selectedFile = fileChooser.showOpenDialog(contentArea.getScene().getWindow());
        
        if (selectedFile != null) {
            try {
                ConfigurationManager.getInstance().importConfiguration(selectedFile.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import Successful");
                alert.setHeaderText(null);
                alert.setContentText("Configuration imported successfully from:\n" + selectedFile.getName() + 
                                     "\n\n(If you are on the Configurations screen, please switch tabs to refresh the list.)");
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Import Failed");
                alert.setHeaderText("Could not import configuration.");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void handleExport() {
        List<Configuration> configs = ConfigurationManager.getInstance().getAllConfigurations();
        
        if (configs.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Configurations");
            alert.setHeaderText(null);
            alert.setContentText("There are no configurations available to export.");
            alert.showAndWait();
            return;
        }

        List<String> configNames = configs.stream().map(Configuration::getName).collect(Collectors.toList());
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(configNames.get(0), configNames);
        dialog.setTitle("Export Configuration");
        dialog.setHeaderText("Select a configuration to export:");
        dialog.setContentText("Configuration:");
        
        Optional<String> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            String selectedConfigName = result.get();
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Configuration");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            fileChooser.setInitialFileName(selectedConfigName.replaceAll("\\s+", "_") + ".json");
            
            File selectedFile = fileChooser.showSaveDialog(contentArea.getScene().getWindow());
            
            if (selectedFile != null) {
                try {
                    ConfigurationManager.getInstance().exportConfiguration(selectedConfigName, selectedFile.getAbsolutePath());
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("Configuration '" + selectedConfigName + "' exported successfully to:\n" + selectedFile.getName());
                    alert.showAndWait();
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Export Failed");
                    alert.setHeaderText("Could not export configuration.");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        }
    }

    private void openUserManual() {
        try {
            java.net.URL url = getClass().getResource("/help/manual.html");
            if (url != null) {
                java.awt.Desktop.getDesktop().browse(url.toURI());
            } else {
                System.out.println("Error: manual.html not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}