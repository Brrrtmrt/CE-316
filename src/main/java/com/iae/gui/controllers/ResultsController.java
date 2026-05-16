package com.iae.gui.controllers;

import com.iae.domain.EvaluationResult;
import com.iae.domain.Status;
import com.iae.gui.SharedState;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.util.List;

public class ResultsController {

    @FXML private TableView<EvaluationResult> resultsTable;
    @FXML private TableColumn<EvaluationResult, String> studentIdCol;
    @FXML private TableColumn<EvaluationResult, String> statusCol;
    @FXML private TableColumn<EvaluationResult, String> matchPercentageCol;
    @FXML private TableColumn<EvaluationResult, Void> actionCol;
    @FXML private TextArea errorDisplayArea;

    private final ObservableList<EvaluationResult> resultsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
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
            String match = (status == Status.PASS) ? "100%" : "0%";
            return new SimpleStringProperty(match);
        });

        setupDetailsButtonColumn();
        resultsTable.setItems(resultsList);

        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateErrorDisplay(newVal);
        });

        // Load any results that were passed via SharedState
        if (SharedState.pendingResults != null && !SharedState.pendingResults.isEmpty()) {
            loadResults(SharedState.pendingResults);
            SharedState.pendingResults = null;
        }
    }

    private void setupDetailsButtonColumn() {
        Callback<TableColumn<EvaluationResult, Void>, TableCell<EvaluationResult, Void>> cellFactory =
                param -> new TableCell<>() {
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
                        setGraphic(empty ? null : btn);
                    }
                };
        actionCol.setCellFactory(cellFactory);
    }

    public void loadResults(List<EvaluationResult> results) {
        resultsList.clear();
        if (results != null) {
            resultsList.addAll(results);
        }
        errorDisplayArea.clear();
    }

    private void updateErrorDisplay(EvaluationResult result) {
        Status status = result.getStatus();
        String log = result.getErrorLog() != null ? result.getErrorLog() : "No errors.";

        if (status == Status.ERROR) {
            errorDisplayArea.setText("FAILURE LOGS:\n" + log);
            errorDisplayArea.setStyle("-fx-text-fill: red; -fx-font-family: monospace;");
        } else if (status == Status.FAIL) {
            errorDisplayArea.setText("OUTPUT MISMATCH:\n" + log);
            errorDisplayArea.setStyle("-fx-text-fill: orange; -fx-font-family: monospace;");
        } else {
            errorDisplayArea.setText("SUCCESS:\nAll steps passed.\n");
            errorDisplayArea.setStyle("-fx-text-fill: green; -fx-font-family: monospace;");
        }
    }

    private void showDetailsDialog(EvaluationResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detailed Evaluation Results");
        String studentId = result.getStudentId() != null ? result.getStudentId() : "Unknown";
        alert.setHeaderText("Execution Details for Student: " + studentId);

        String content = "Status:          " + result.getStatus() + "\n"
                + "Unzip:           " + (result.isUnzipSuccess()   ? "PASS" : "FAIL") + "\n"
                + "Compile:         " + (result.isCompileSuccess()  ? "PASS" : "FAIL") + "\n"
                + "Run:             " + (result.isRunSuccess()      ? "PASS" : "FAIL") + "\n"
                + "Output Match:    " + (result.isOutputMatch()     ? "PASS" : "FAIL") + "\n\n"
                + "Error Log:\n"
                + (result.getErrorLog() != null ? result.getErrorLog() : "None");

        TextArea area = new TextArea(content);
        area.setWrapText(true);
        area.setEditable(false);
        area.setPrefSize(400, 220);

        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
