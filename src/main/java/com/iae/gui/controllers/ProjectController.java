package com.iae.gui.controllers;

import java.io.File;
import java.util.List;

import com.iae.domain.Configuration;
import com.iae.domain.Project;
import com.iae.service.ConfigurationManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class ProjectController {

    @FXML private TextField txtProjectName; 
    @FXML private TextField txtDirectoryPath;
    @FXML private ComboBox<String> cmbConfigurations;
    @FXML private TextField txtArguments;
    @FXML private TextArea txtExpectedOutput;

    @FXML private Button btnBrowse;
    @FXML private Button btnCreateProject;
    @FXML private Button btnOpenProject;
    @FXML private Label lblStatus;

    private ConfigurationManager configManager;

    @FXML
    public void initialize() {
        configManager = ConfigurationManager.getInstance();
        loadConfigurationsIntoComboBox();

        btnBrowse.setOnAction(event -> browseDirectory());
        btnCreateProject.setOnAction(event -> createProject());
    }

    private void loadConfigurationsIntoComboBox() {
        List<Configuration> configs = configManager.getAllConfigurations();
        
        if (configs != null && !configs.isEmpty()) {
            for (Configuration config : configs) {
                cmbConfigurations.getItems().add(config.getName());
            }
        } else {
            System.out.println("Warning: No saved configuration found or the JSON file could not be read.");
        }
    }

    private void browseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Submissions Directory");
        
        Stage stage = (Stage) btnBrowse.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            txtDirectoryPath.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void createProject() {
        try {
            String dirPath = txtDirectoryPath.getText();
            String selectedConfigName = cmbConfigurations.getValue();
            String argumentsStr = txtArguments.getText();
            String expectedOutput = txtExpectedOutput.getText();

            if (dirPath == null || dirPath.trim().isEmpty() || selectedConfigName == null) {
                lblStatus.setText("Directory and Configuration are required!");
                lblStatus.setTextFill(Color.RED);
                lblStatus.setVisible(true);
                return;
            }

            java.io.File directory = new java.io.File(dirPath);
            if (!directory.exists() || !directory.isDirectory()) {
                lblStatus.setText("Error: Selected path is not a valid directory!");
                lblStatus.setTextFill(Color.RED);
                lblStatus.setVisible(true);
                return;
            }

            String[] argsArray = new String[0];
            if (argumentsStr != null && !argumentsStr.trim().isEmpty()) {
                argsArray = argumentsStr.trim().split("\\s+");
            }

            if (expectedOutput == null) {
                expectedOutput = "";
            }

            Configuration selectedConfig = configManager.getConfiguration(selectedConfigName);

            Project newProject = new Project(selectedConfig, dirPath, argsArray, expectedOutput);
            newProject.setName(txtProjectName.getText());

            lblStatus.setText("Project created successfully!");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);

        } catch (Exception e) {
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }
}