package com.iae.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.iae.domain.Configuration;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.gui.SharedState;
import com.iae.service.ConfigurationManager;
import com.iae.service.EvaluationService;
import com.iae.service.ProjectService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
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
    @FXML private Button btnRunEvaluation;
    @FXML private Label lblStatus;

    private ConfigurationManager configManager;
    private ProjectService projectService;
    private EvaluationService evaluationService;

    private Project lastCreatedProject;

    @FXML
    public void initialize() {
        configManager = ConfigurationManager.getInstance();
        projectService = ProjectService.getInstance();
        evaluationService = new EvaluationService();
        loadConfigurationsIntoComboBox();

        btnBrowse.setOnAction(event -> browseDirectory());
        btnCreateProject.setOnAction(event -> createProject());
        btnRunEvaluation.setOnAction(event -> runEvaluation());

        btnOpenProject.setOnAction(event ->
                System.out.println("Open Project not yet implemented."));
    }

    private void loadConfigurationsIntoComboBox() {
        List<Configuration> configs = configManager.getAllConfigurations();
        if (configs != null && !configs.isEmpty()) {
            for (Configuration config : configs) {
                cmbConfigurations.getItems().add(config.getName());
            }
        }
    }

    private void browseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Submissions Directory");
        Stage stage = (Stage) btnBrowse.getScene().getWindow();
        File selected = chooser.showDialog(stage);
        if (selected != null) {
            txtDirectoryPath.setText(selected.getAbsolutePath());
        }
    }

    private void createProject() {
        try {
            String dirPath = txtDirectoryPath.getText();
            String selectedConfigName = cmbConfigurations.getValue();
            String argumentsStr = txtArguments.getText();
            String expectedOutput = txtExpectedOutput.getText();

            if (dirPath == null || dirPath.trim().isEmpty() || selectedConfigName == null) {
                showStatus("Directory and Configuration are required!", Color.RED);
                return;
            }

            File directory = new File(dirPath);
            if (!directory.exists() || !directory.isDirectory()) {
                showStatus("Error: Selected path is not a valid directory!", Color.RED);
                return;
            }

            String[] argsArray = new String[0];
            if (argumentsStr != null && !argumentsStr.trim().isEmpty()) {
                argsArray = argumentsStr.trim().split("\\s+");
            }

            if (expectedOutput == null) expectedOutput = "";

            Configuration selectedConfig = configManager.getConfiguration(selectedConfigName);
            Project newProject = new Project(selectedConfig, dirPath, argsArray, expectedOutput);
            newProject.setName(txtProjectName.getText());
            projectService.addProject(newProject);

            lastCreatedProject = newProject;
            btnRunEvaluation.setDisable(false);

            showStatus("Project created! Click 'Run & Evaluate' to start.", Color.GREEN);

        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), Color.RED);
        }
    }

    private void runEvaluation() {
        if (lastCreatedProject == null) {
            showStatus("Create a project first.", Color.RED);
            return;
        }

        btnRunEvaluation.setDisable(true);
        showStatus("Running evaluation, please wait...", Color.BLUE);

        // Run on a background thread so the UI stays responsive
        Thread evalThread = new Thread(() -> {
            try {
                List<EvaluationResult> results = evaluationService.evaluateProject(lastCreatedProject);
                Platform.runLater(() -> {
                    SharedState.pendingResults = results;
                    navigateToResults();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnRunEvaluation.setDisable(false);
                    showStatus("Evaluation error: " + e.getMessage(), Color.RED);
                });
            }
        });
        evalThread.setDaemon(true);
        evalThread.start();
    }

    private void navigateToResults() {
        try {
            Parent screen = FXMLLoader.load(getClass().getResource("/fxml/Results.fxml"));
            StackPane contentArea = (StackPane) btnBrowse.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(screen);
            }
        } catch (IOException e) {
            showStatus("Failed to load Results screen: " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    private void showStatus(String message, Color color) {
        lblStatus.setText(message);
        lblStatus.setTextFill(color);
        lblStatus.setVisible(true);
    }
}
