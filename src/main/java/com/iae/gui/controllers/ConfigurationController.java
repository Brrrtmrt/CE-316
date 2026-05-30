package com.iae.gui.controllers;

import com.iae.domain.Configuration;
import com.iae.evaluation.strategies.ComparisonStrategy;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import com.iae.evaluation.strategies.TrimLinesStrategy;
import com.iae.service.ConfigurationService;

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

    private ConfigurationService configService;

    @FXML
    public void initialize() {
        configService = new ConfigurationService();
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
        for (Configuration c : configService.getAllConfigurations()) {
            listViewConfigs.getItems().add(c.getName());
        }
    }

    private void loadConfigIntoForm(String configName) {
        Configuration config = configService.getConfiguration(configName);
        if (config != null) {
            txtConfigName.setText(config.getName());
            txtFileExtension.setText(config.getFileExtension());

            // Fix: trim first and guard for blank before splitting (Copilot #2)
            String compileCmd = config.getCompileCommand() != null ? config.getCompileCommand() : "";
            String trimmedCompileCmd = compileCmd.trim();
            String firstToken = trimmedCompileCmd.isBlank() ? "" : trimmedCompileCmd.split("\\s+")[0];
            txtCompilerPath.setText(firstToken.equals("{src}") ? "" : firstToken);

            txtCompileCommand.setText(compileCmd);
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

    private void saveConfiguration() {
        try {
            String name       = txtConfigName.getText();
            String extension  = txtFileExtension.getText();
            String compileCmd = txtCompileCommand.getText();
            String runCmd     = txtRunCommand.getText();
            String strategyStr = cmbComparisonStrategy.getValue();

            ComparisonStrategy strategyObj;
            if ("Trim Lines".equals(strategyStr)) {
                strategyObj = new TrimLinesStrategy();
            } else if ("Ignore Whitespace".equals(strategyStr)) {
                strategyObj = new IgnoreWhitespaceStrategy();
            } else {
                strategyObj = new ExactMatchStrategy();
            }

            String finalLang = deriveLanguageFromExtension(extension);

            configService.createConfiguration(
                    name,
                    finalLang,
                    extension,
                    compileCmd,
                    runCmd,
                    strategyObj,
                    ""
            );

            lblStatus.setText("Configuration saved successfully!");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);

            refreshList();

        } catch (Exception e) {
            lblStatus.setText("Error saving: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }

    private void updateConfiguration() {
        String selected = listViewConfigs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Please select a config to update!");
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            return;
        }

        try {
            String newName    = txtConfigName.getText();
            String extension  = txtFileExtension.getText();
            String compileCmd = txtCompileCommand.getText();
            String runCmd     = txtRunCommand.getText();
            String strategyStr = cmbComparisonStrategy.getValue();

            ComparisonStrategy strategyObj;
            if ("Trim Lines".equals(strategyStr)) {
                strategyObj = new TrimLinesStrategy();
            } else if ("Ignore Whitespace".equals(strategyStr)) {
                strategyObj = new IgnoreWhitespaceStrategy();
            } else {
                strategyObj = new ExactMatchStrategy();
            }

            String finalLang = deriveLanguageFromExtension(extension);
            Configuration existing = configService.getConfiguration(selected);
            if ((finalLang == null || finalLang.isEmpty()) && existing != null
                    && existing.getLanguage() != null) {
                finalLang = existing.getLanguage();
            }

            String finalDesc = "";
            if (existing != null && existing.getDescription() != null) {
                finalDesc = stripCompilerPathLines(existing.getDescription());
            }

            configService.updateConfiguration(
                    selected,
                    newName,
                    finalLang,
                    extension,
                    compileCmd,
                    runCmd,
                    strategyObj,
                    finalDesc
            );

            lblStatus.setText("Configuration updated successfully!");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);

            refreshList();
            if (newName != null && !newName.trim().isEmpty()) {
                listViewConfigs.getSelectionModel().select(newName.trim());
            }

        } catch (Exception e) {
            lblStatus.setText("Error updating: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }

    private void deleteConfiguration() {
        String selected = listViewConfigs.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Please select a config to delete!");
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
            return;
        }

        try {
            configService.deleteConfiguration(selected);

            refreshList();

            txtConfigName.clear();
            txtFileExtension.clear();
            txtCompilerPath.clear();
            txtCompileCommand.clear();
            txtRunCommand.clear();
            cmbComparisonStrategy.setValue(null);

            lblStatus.setText("Configuration deleted successfully!");
            lblStatus.setTextFill(Color.GREEN);
            lblStatus.setVisible(true);
        } catch (Exception e) {
            lblStatus.setText("Error deleting: " + e.getMessage());
            lblStatus.setTextFill(Color.RED);
            lblStatus.setVisible(true);
        }
    }

    private String stripCompilerPathLines(String description) {
        if (description == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : description.split("\\r?\\n")) {
            if (!line.trim().startsWith("Compiler Path:")) {
                if (sb.length() > 0) sb.append('\n'); // Fix: sb.isEmpty() -> sb.length() > 0 (Copilot #1)
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }

    private String deriveLanguageFromExtension(String extension) {
        if (extension == null || extension.isBlank()) return "";
        String ext = extension.toLowerCase().replace(".", "").trim();

        return switch (ext) {
            case "java"             -> "Java";
            case "py"               -> "Python";
            case "c"                -> "C";
            case "cpp", "cc", "cxx" -> "C++";
            case "js"               -> "JavaScript";
            case "ts"               -> "TypeScript";
            case "cs"               -> "C#";
            case "go"               -> "Go";
            case "rs"               -> "Rust";
            default -> ext.isEmpty() ? "" : ext.substring(0, 1).toUpperCase() + ext.substring(1);
        };
    }
}
