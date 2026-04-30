package com.iae.gui.controllers;

import com.iae.domain.EvaluationResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ResultsController {

    @FXML
    private TableView<EvaluationResult> resultsTable;

    @FXML
    private TableColumn<EvaluationResult, String> studentIdColumn;

    @FXML
    private TextArea errorDisplayArea;

    private final ObservableList<EvaluationResult> resultsData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Bind the "studentId" column to the studentId property in EvaluationResult
        studentIdColumn.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        
        resultsTable.setItems(resultsData);
        
        // Add a listener to update the errorDisplayArea when a new row is selected
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // TODO: Update this when a getter for the error log is added to EvaluationResult
                // errorDisplayArea.setText(newSelection.getErrorLog());
            }
        });
    }

    public void setResults(List<EvaluationResult> results) {
        resultsData.setAll(results);
    }
}
