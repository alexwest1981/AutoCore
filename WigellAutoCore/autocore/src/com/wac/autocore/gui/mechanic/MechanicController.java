package com.wac.autocore.gui.mechanic;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Mechanic;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class MechanicController {

    @FXML
    private TableView<Mechanic> mechanicTable;

    @FXML
    private TableColumn<Mechanic, Integer> idColumn;

    @FXML
    private TableColumn<Mechanic, String> nameColumn;

    @FXML
    private TableColumn<Mechanic, String> phoneColumn;

    @FXML
    private TableColumn<Mechanic, String> specializationColumn;

    @FXML
    private TableColumn<Mechanic, Boolean> availableColumn;

    @FXML
    private CheckBox availableOnlyCheckBox;

    private FilteredList<Mechanic> filteredMechanics;

    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        specializationColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

        availableColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Mechanic, Boolean>() {
            @Override
            protected void updateItem(Boolean available, boolean empty) {
                super.updateItem(available, empty);
                if (empty || available == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(available ? "Available" : "Busy");
                    setStyle(available ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });

        ObservableList<Mechanic> allMechanics = FXCollections.observableArrayList(Database.getMechanics());
        filteredMechanics = new FilteredList<>(allMechanics, mechanic -> true);
        mechanicTable.setItems(filteredMechanics);

        availableOnlyCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                filteredMechanics.setPredicate(Mechanic::isAvailable);
            } else {
                filteredMechanics.setPredicate(mechanic -> true);
            }
        });
    }

    public void refreshTable() {
        mechanicTable.refresh();
    }
}
