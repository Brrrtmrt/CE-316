package com.iae.gui.controllers;

import com.iae.domain.Configuration;
import com.iae.domain.ConfigurationBuilder;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.service.ConfigurationManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class ConfigurationController {

    @FXML private TextField txtConfigName;
    @FXML private TextField txtFileExtension;
    @FXML private TextField txtCompilerPath;
    @FXML private TextField txtCompileCommand;
    @FXML private TextField txtRunCommand;
    @FXML private ComboBox<String> cmbComparisonStrategy;
    
    @FXML private Button btnSave;
    @FXML private Label lblStatus;

    private ConfigurationManager configManager;

    @FXML
    public void initialize() {
        configManager = ConfigurationManager.getInstance();

        cmbComparisonStrategy.getItems().addAll("Exact Match", "Ignore Whitespace", "Trim Lines");

        btnSave.setOnAction(event -> saveConfiguration());
    }

    private void saveConfiguration() {
        try {
            String name = txtConfigName.getText();
            String extension = txtFileExtension.getText();
            String compilerPath = txtCompilerPath.getText();
            String compileCmd = txtCompileCommand.getText();
            String runCmd = txtRunCommand.getText();
            String strategyStr = cmbComparisonStrategy.getValue();

            if(name == null || name.trim().isEmpty() || extension == null || extension.trim().isEmpty()) {
                lblStatus.setText("Please fill required fields!");
                lblStatus.setTextFill(Color.RED);
                lblStatus.setVisible(true);
                return;
            }

            ComparisonStrategy strategyObj;
            if ("Trim Lines".equals(strategyStr)) {
                strategyObj = new TrimLinesStrategy();
            } else if ("Ignore Whitespace".equals(strategyStr)) {
                strategyObj = new IgnoreWhitespaceStrategy();
            } else {
                strategyObj = new ExactMatchStrategy(); 
            }

            Configuration newConfig = new ConfigurationBuilder()
                    .setName(name)
                    .setLanguage(name)
                    .setFileExtension(extension)
                    .setCompileCommand(compileCmd)
                    .setRunCommand(runCmd)
                    .setComparisonStrategy(strategyObj)
                    .setDescription("Compiler Path: " + compilerPath) 
                    .build();

            configManager.addConfiguration(newConfig);
            configManager.saveAllConfigurations();

            lblStatus.setText("Configuration saved successfully!");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);

        } catch (Exception e) {
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            e.printStackTrace(); 
        }
    }
}