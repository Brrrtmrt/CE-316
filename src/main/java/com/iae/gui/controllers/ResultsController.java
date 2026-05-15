package com.iae.gui.controllers;

import com.iae.domain.EvaluationResult;
import com.iae.domain.Status;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;

public class ResultsController {

    @FXML
    private TableView<EvaluationResult> resultsTable;

    @FXML
    private TableColumn<EvaluationResult, String> studentIdCol;

    @FXML
    private TableColumn<EvaluationResult, String> statusCol;

    @FXML
    private TableColumn<EvaluationResult, String> matchPercentageCol;

    @FXML
    private TableColumn<EvaluationResult, Void> actionCol;

    @FXML
    private TextArea errorDisplayArea; // Area at bottom/side for detailed errors

    private final ObservableList<EvaluationResult> resultsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Setup Table Columns
        // Note: Replace the getter methods inside the lambda with the exact method names in your EvaluationResult class
        
        studentIdCol.setCellValueFactory(cellData -> {
            // Assuming EvaluationResult has a reference to StudentSubmission
            String id = cellData.getValue().getStudentSubmission() != null 
                        ? cellData.getValue().getStudentSubmission().getStudentId() 
                        : "Unknown";
            return new SimpleStringProperty(id);
        });

        statusCol.setCellValueFactory(cellData -> {
            Status status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.name() : "N/A");
        });

        matchPercentageCol.setCellValueFactory(cellData -> {
            // Assuming there is a getScore() or getMatchPercentage() method returning a double
            double score = cellData.getValue().getScore(); // Or similar
            return new SimpleStringProperty(String.format("%.1f%%", score));
        });

        // 2. Add the dynamic "Details" Button to the Action Column
        setupDetailsButtonColumn();

        resultsTable.setItems(resultsList);

        // 3. Selection Listener for updating the Error Display Area
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateErrorDisplay(newSelection);
            }
        });
    }

    private void setupDetailsButtonColumn() {
        Callback<TableColumn<EvaluationResult, Void>, TableCell<EvaluationResult, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btn = new Button("Details");

            {
                btn.setOnAction(event -> {
                    EvaluationResult result = getTableView().getItems().get(getIndex());
                    showDetailsDialog(result);
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        };
        actionCol.setCellFactory(cellFactory);
    }

    /**
     * Call this method from the EvaluationFacade or MainController when evaluation finishes.
     */
    public void loadResults(List<EvaluationResult> results) {
        resultsList.clear();
        if (results != null) {
            resultsList.addAll(results);
        }
        errorDisplayArea.clear();
    }

    /**
     * Updates the lower text area to quickly show compilation or run errors for the selected row.
     */
    private void updateErrorDisplay(EvaluationResult result) {
        Status status = result.getStatus();
        
        // Assuming EvaluationResult has a getErrorMessage() or getConsoleOutput()
        String logs = result.getErrorMessage() != null ? result.getErrorMessage() : "No error output.";

        if (status != null && (status == Status.COMPILE_ERROR || status == Status.RUNTIME_ERROR)) {
            errorDisplayArea.setText("FAILURE LOGS:\n" + logs);
            errorDisplayArea.setStyle("-fx-text-fill: red; -fx-font-family: monospace;");
        } else {
            errorDisplayArea.setText("SUCCESS:\nExecution completed successfully.");
            errorDisplayArea.setStyle("-fx-text-fill: green; -fx-font-family: monospace;");
        }
    }

    /**
     * Opens a popup alert showing the full detailed view of that specific student's run.
     */
    private void showDetailsDialog(EvaluationResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detailed Evaluation Results");
        
        String studentId = result.getStudentSubmission() != null 
                ? result.getStudentSubmission().getStudentId() : "Unknown";
        alert.setHeaderText("Execution Details for Student: " + studentId);
        
        String content = "Status: " + (result.getStatus() != null ? result.getStatus().name() : "N/A") + "\n"
                       + "Match: " + String.format("%.2f%%", result.getScore()) + "\n\n"
                       + "System Output/Error Logs:\n"
                       + (result.getErrorMessage() != null ? result.getErrorMessage() : "None");
                       
        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefSize(400, 200);
        
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}