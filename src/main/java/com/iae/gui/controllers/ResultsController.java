package com.iae.gui.controllers;

import com.iae.domain.StudentSubmission;
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
    private TableView<StudentSubmission> resultsTable;

    @FXML
    private TableColumn<StudentSubmission, String> studentIdCol;

    @FXML
    private TableColumn<StudentSubmission, String> statusCol;

    @FXML
    private TableColumn<StudentSubmission, String> matchPercentageCol;

    @FXML
    private TableColumn<StudentSubmission, Void> actionCol;

    @FXML
    private TextArea errorDisplayArea; // Area at bottom/side for detailed errors

    private final ObservableList<StudentSubmission> resultsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Setup Table Columns
        
        studentIdCol.setCellValueFactory(cellData -> {
            String id = cellData.getValue().getStudentId();
            return new SimpleStringProperty(id != null ? id : "Unknown");
        });

        statusCol.setCellValueFactory(cellData -> {
            Status status = cellData.getValue().getSubmissionStatus();
            return new SimpleStringProperty(status != null ? status.name() : "N/A");
        });

        matchPercentageCol.setCellValueFactory(cellData -> {
            Status status = cellData.getValue().getSubmissionStatus();
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
        Callback<TableColumn<StudentSubmission, Void>, TableCell<StudentSubmission, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btn = new Button("Details");

            {
                btn.setOnAction(event -> {
                    StudentSubmission result = getTableView().getItems().get(getIndex());
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
    public void loadResults(List<StudentSubmission> results) {
        resultsList.clear();
        if (results != null) {
            resultsList.addAll(results);
        }
        errorDisplayArea.clear();
    }

    /**
     * Updates the lower text area to quickly show compilation or run errors for the selected row.
     */
    private void updateErrorDisplay(StudentSubmission result) {
        Status status = result.getSubmissionStatus();
        
        String logs = result.getProgramOutput() != null ? result.getProgramOutput() : "No output/logs available.";

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
    private void showDetailsDialog(StudentSubmission result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detailed Evaluation Results");
        
        String studentId = result.getStudentId() != null ? result.getStudentId() : "Unknown";
        alert.setHeaderText("Execution Details for Student: " + studentId);
        
        String content = "Status: " + (result.getSubmissionStatus() != null ? result.getSubmissionStatus().name() : "N/A") + "\n\n"
                       + "System Output/Error Logs:\n"
                       + (result.getProgramOutput() != null ? result.getProgramOutput() : "None");
                       
        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefSize(400, 200);
        
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}