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
        
        studentIdCol.setCellValueFactory(cellData -> {
            String id = cellData.getValue().getStudentId();
            return new SimpleStringProperty(id != null ? id : "Unknown");
        });

        statusCol.setCellValueFactory(cellData -> {
            Status status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.name() : "N/A");
        });

        matchPercentageCol.setCellValueFactory(cellData -> {
            Status status = cellData.getValue().getStatus();
            String match = (status == Status.PASS) ? "100.0%" : "0.0%";
            return new SimpleStringProperty(match);
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
        
        String logs = result.getProgramOutput() != null ? result.getProgramOutput() : (result.getErrorLog() != null ? result.getErrorLog() : "No output/logs available.");

        if (status != null && status == Status.ERROR) {
            errorDisplayArea.setText("FAILURE LOGS:\n" + logs);
            errorDisplayArea.setStyle("-fx-text-fill: red; -fx-font-family: monospace;");
        } else if (status != null && status == Status.FAIL) {
            errorDisplayArea.setText("OUTPUT MISMATCH:\n" + logs);
            errorDisplayArea.setStyle("-fx-text-fill: orange; -fx-font-family: monospace;");
        } else {
            errorDisplayArea.setText("SUCCESS:\nExecution completed successfully.\n\n" + logs);
            errorDisplayArea.setStyle("-fx-text-fill: green; -fx-font-family: monospace;");
        }
    }

    /**
     * Opens a popup alert showing the full detailed view of that specific student's run.
     */
    private void showDetailsDialog(EvaluationResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detailed Evaluation Results");
        
        String studentId = result.getStudentId() != null ? result.getStudentId() : "Unknown";
        alert.setHeaderText("Execution Details for Student: " + studentId);
        
        String content = "Status: " + (result.getStatus() != null ? result.getStatus().name() : "N/A") + "\n\n"
                       + "System Output/Error Logs:\n"
                       + (result.getProgramOutput() != null ? result.getProgramOutput() : (result.getErrorLog() != null ? result.getErrorLog() : "None"));
                       
        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefSize(400, 200);
        
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}