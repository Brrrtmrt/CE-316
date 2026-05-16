package com.iae.gui.controllers;

import java.io.File;

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
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class ConfigurationController {

    @FXML private TextField txtConfigName;
    @FXML private TextField txtFileExtension;
    @FXML private TextField txtCompilerPath;
    @FXML private TextField txtCompileCommand;
    @FXML private TextField txtRunCommand;
    @FXML private ComboBox<String> cmbComparisonStrategy;
    
    @FXML private ListView<String> listViewConfigs;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    
    @FXML private Button btnSave;
    @FXML private Label lblStatus;

    private ConfigurationManager configManager;
    
    private String currentLoadedDescription = "";

    @FXML
    public void initialize() {
        configManager = ConfigurationManager.getInstance();
        cmbComparisonStrategy.getItems().addAll("Exact Match", "Ignore Whitespace", "Trim Lines");

        btnSave.setOnAction(event -> saveConfiguration());
        btnUpdate.setOnAction(event -> updateConfiguration());
        btnDelete.setOnAction(event -> deleteConfiguration());

        refreshList();

        listViewConfigs.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadConfigIntoForm(newVal);
            }
        });
    }

    private void refreshList() {
        listViewConfigs.getItems().clear();
        for (Configuration c : configManager.getAllConfigurations()) {
            listViewConfigs.getItems().add(c.getName());
        }
    }

    private void loadConfigIntoForm(String configName) {
        Configuration config = configManager.getConfiguration(configName);
        if (config != null) {
            txtConfigName.setText(config.getName());
            txtFileExtension.setText(config.getFileExtension());
            
            String desc = config.getDescription();
            if (desc != null) {
                int idx = desc.indexOf("Compiler Path: ");
                if (idx != -1) {
                    txtCompilerPath.setText(desc.substring(idx + "Compiler Path: ".length()).trim());
                    currentLoadedDescription = desc.substring(0, idx).trim(); 
                } else {
                    txtCompilerPath.setText("");
                    currentLoadedDescription = desc; // İçinde path yoksa tamamını yedeğe al
                }
            } else {
                txtCompilerPath.setText("");
                currentLoadedDescription = "";
            }
            
            txtCompileCommand.setText(config.getCompileCommand() != null ? config.getCompileCommand() : "");
            txtRunCommand.setText(config.getRunCommand() != null ? config.getRunCommand() : "");
            
            if (config.getComparisonStrategy() instanceof TrimLinesStrategy) {
                cmbComparisonStrategy.setValue("Trim Lines");
            } else if (config.getComparisonStrategy() instanceof IgnoreWhitespaceStrategy) {
                cmbComparisonStrategy.setValue("Ignore Whitespace");
            } else {
                cmbComparisonStrategy.setValue("Exact Match");
            }
        }
    }

    private void updateConfiguration() {
        String selected = listViewConfigs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Please select a config from the list to update!");
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            return;
        }

        String name = txtConfigName.getText();
        String extension = txtFileExtension.getText();
        String runCmd = txtRunCommand.getText();

        if(name == null || name.trim().isEmpty() || 
           extension == null || extension.trim().isEmpty() || 
           runCmd == null || runCmd.trim().isEmpty()) {
            lblStatus.setText("Please fill required fields (Name, Extension, Run Command)!");
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            return; 
        }

        Configuration backup = configManager.getConfiguration(selected);
        configManager.removeConfiguration(selected);
        
        saveConfiguration();

        if (lblStatus.getTextFill().equals(Color.RED)) {
            if (backup != null) {
                try { configManager.addConfiguration(backup); } catch(Exception ex) {}
            }
        } else {
            String updatedName = txtConfigName.getText().trim();
            if (!selected.equals(updatedName)) {
                File oldFile = new File("config/" + selected + ".json");
                if (oldFile.exists()) oldFile.delete();
            }
            
            if (!updatedName.isEmpty() && !selected.equals(updatedName)) {
                listViewConfigs.getSelectionModel().select(updatedName);
            }
        }
    }

    private void deleteConfiguration() {
        String selected = listViewConfigs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Please select a config from the list to delete!");
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            return;
        }

        Configuration backup = configManager.getConfiguration(selected);

        try {
            configManager.removeConfiguration(selected);
            configManager.saveAllConfigurations(); 
            
            File fileToDelete = new File("config/" + selected + ".json");
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
            
            refreshList();
            
            txtConfigName.clear();
            txtFileExtension.clear();
            txtCompilerPath.clear();
            txtCompileCommand.clear();
            txtRunCommand.clear();
            cmbComparisonStrategy.setValue(null);
            currentLoadedDescription = ""; 

            lblStatus.setText("Configuration deleted successfully!");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);
        } catch (Exception e) {
            if (backup != null) {
                try { configManager.addConfiguration(backup); } catch(Exception ex) {}
            }
            lblStatus.setText("Error deleting config: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }

    private void saveConfiguration() {
        try {
            String name = txtConfigName.getText();
            String extension = txtFileExtension.getText();
            String compilerPath = txtCompilerPath.getText();
            String compileCmd = txtCompileCommand.getText();
            String runCmd = txtRunCommand.getText();
            String strategyStr = cmbComparisonStrategy.getValue();

            if(name == null || name.trim().isEmpty() || 
                extension == null || extension.trim().isEmpty() || 
                runCmd == null || runCmd.trim().isEmpty()) {
                lblStatus.setText("Please fill required fields (Name, Extension, Run Command)!");
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

            String finalDesc = currentLoadedDescription;
            if (compilerPath != null && !compilerPath.trim().isEmpty()) {
                if (!finalDesc.isEmpty()) finalDesc += "\n";
                finalDesc += "Compiler Path: " + compilerPath;
            }

            Configuration newConfig = new ConfigurationBuilder()
                    .setName(name)
                    .setLanguage(name)
                    .setFileExtension(extension)
                    .setCompileCommand(compileCmd)
                    .setRunCommand(runCmd)
                    .setComparisonStrategy(strategyObj)
                    .setDescription(finalDesc) 
                    .build();

            try {
                configManager.addConfiguration(newConfig);
                configManager.saveAllConfigurations();

                lblStatus.setText("Configuration saved successfully!");
                lblStatus.setTextFill(Color.GREEN);
                lblStatus.setVisible(true);
                
                refreshList();
                
            } catch (Exception e) {
                configManager.removeConfiguration(newConfig.getName()); 
                lblStatus.setText("Error saving config: " + e.getMessage());
                lblStatus.setTextFill(Color.RED);
                lblStatus.setVisible(true);
            }

        } catch (Exception e) {
            lblStatus.setText("Error: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            e.printStackTrace(); 
        }
    }
}