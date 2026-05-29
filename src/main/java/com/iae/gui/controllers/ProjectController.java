package com.iae.gui.controllers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.iae.domain.Configuration;
import com.iae.domain.EvaluationResult;
import com.iae.domain.Project;
import com.iae.service.ConfigurationManager;
import com.iae.service.EvaluationService;
import com.iae.service.ProjectService;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
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
    @FXML private Button btnRunEvaluation;
    @FXML private Label lblStatus;

    private ConfigurationManager configManager;
    private ProjectService projectService;
    private EvaluationService evaluationService;
    private Project currentProject;
    private MainController mainController;

    @FXML
    public void initialize() {
        configManager = ConfigurationManager.getInstance();
        projectService = ProjectService.getInstance();
        evaluationService = new EvaluationService();
        loadConfigurationsIntoComboBox();

        btnBrowse.setOnAction(event -> browseDirectory());
        btnCreateProject.setOnAction(event -> createProject());
        if (btnRunEvaluation != null) {
            btnRunEvaluation.setDisable(true); 
            btnRunEvaluation.setOnAction(event -> runEvaluation());
        }

        btnOpenProject.setOnAction(event -> openExistingProject());
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
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
            String projectName = txtProjectName.getText();
            String dirPath = txtDirectoryPath.getText();
            String selectedConfigName = cmbConfigurations.getValue();
            String argumentsStr = txtArguments.getText();
            String expectedOutput = txtExpectedOutput.getText();

            if (projectName == null || projectName.trim().isEmpty()) {
                lblStatus.setText("Project name is required!");
                lblStatus.setTextFill(Color.RED);
                lblStatus.setVisible(true);
                return;
            }

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

            currentProject = new Project(selectedConfig, dirPath, argsArray, expectedOutput);
            currentProject.setName(projectName);
            
            projectService.createProject(currentProject);

            lblStatus.setText("Project created! Click 'Run Evaluation' to start.");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);
            
            if (btnRunEvaluation != null) {
                btnRunEvaluation.setDisable(false);
            }

        } catch (Exception e) {
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }

    private void runEvaluation() {
        if (currentProject == null) {
            lblStatus.setText("Please create a project first!");
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            return;
        }

        lblStatus.setText("Running evaluation...");
        lblStatus.setTextFill(Color.BLUE);
        lblStatus.setVisible(true);
        if (btnRunEvaluation != null) {
            btnRunEvaluation.setDisable(true);
        }

        Task<List<EvaluationResult>> task = new Task<>() {
            @Override
            protected List<EvaluationResult> call() throws Exception {
                return evaluationService.evaluateProject(currentProject);
            }
        };

        task.setOnSucceeded(event -> {
            List<EvaluationResult> results = task.getValue();
            lblStatus.setText("Evaluation completed! " + results.size() + " submissions processed.");
            lblStatus.setTextFill(Color.GREEN);
            if (btnRunEvaluation != null) {
                btnRunEvaluation.setDisable(false);
            }
    
            try {
                if (mainController != null) {
                    FXMLLoader loader = mainController.loadOrGetScreen("/fxml/Results.fxml");
                    if (loader != null) {
                        ResultsController resultsController = loader.getController();
                        resultsController.loadResults(results); 
                        mainController.activateScreen("/fxml/Results.fxml"); 
                    }
                }
        } catch (Exception ex) {
            lblStatus.setText("Error loading results screen: " + ex.getMessage());
            lblStatus.setTextFill(Color.RED);
        }
    });

        task.setOnFailed(event -> {
            lblStatus.setText("Evaluation failed: " + task.getException().getMessage());
            lblStatus.setTextFill(Color.RED);
            if (btnRunEvaluation != null) btnRunEvaluation.setDisable(false);
        });

        new Thread(task).start();
    }

    private void openExistingProject() {
        try {
            List<Project> projects = projectService.getAllProjects();
            
            if (projects == null || projects.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Open Project");
                alert.setHeaderText(null);
                alert.setContentText("No saved projects found in the database.");
                alert.showAndWait();
                return;
            }
            
            Map<String, Project> projectMap = new HashMap<>();
            for (Project p : projects) {
                String displayName = p.getName() + " (ID: " + p.getId() + ")";
                projectMap.put(displayName, p);
            }
            
            List<String> displayNames = projectMap.keySet().stream().sorted().collect(Collectors.toList());
            
            ChoiceDialog<String> dialog = new ChoiceDialog<>(displayNames.get(0), displayNames);
            dialog.setTitle("Open Existing Project");
            dialog.setHeaderText("Select a project to load:");
            dialog.setContentText("Project:");
            
            Optional<String> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                Project selectedProject = projectMap.get(result.get());
                
                txtProjectName.setText(selectedProject.getName());
                txtDirectoryPath.setText(selectedProject.getSubmissionsDirectory());
                
                if (selectedProject.getConfiguration() != null) {
                    cmbConfigurations.setValue(selectedProject.getConfiguration().getName());
                }
                
                if (selectedProject.getProgramArguments() != null && selectedProject.getProgramArguments().length > 0) {
                    txtArguments.setText(String.join(" ", selectedProject.getProgramArguments()));
                } else {
                    txtArguments.setText("");
                }
                
                txtExpectedOutput.setText(selectedProject.getExpectedOutput() != null ? selectedProject.getExpectedOutput() : "");
                
                currentProject = selectedProject;
                
                lblStatus.setText("Project '" + selectedProject.getName() + "' loaded successfully!");
                lblStatus.setTextFill(Color.GREEN);
                lblStatus.setVisible(true);
                
                if (btnRunEvaluation != null) {
                    btnRunEvaluation.setDisable(false);
                }
            }
        } catch (Exception e) {
            lblStatus.setText("Error loading projects: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }
}
