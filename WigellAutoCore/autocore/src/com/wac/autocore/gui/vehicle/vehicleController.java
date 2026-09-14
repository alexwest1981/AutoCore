package com.wac.autocore.gui.vehicle;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class vehicleController {

    GarageSystem garageSystem;
    @FXML
    private TableView<Vehicle> vehicleTableView;

    @FXML
    private TableColumn<Vehicle, Integer> vehicleIdColumn;
    @FXML
    private TableColumn<Vehicle, String> vehicleRegistrationNumberColumn;
    @FXML
    private TableColumn<Vehicle, String> vehicleBrandColumn;
    @FXML
    private TableColumn<Vehicle, String> vehicleModelColumn;
    @FXML
    private TableColumn<Vehicle, Integer> vehicleYearColumn;
    @FXML
    private TableColumn<Vehicle, Integer> vehicleCustomerIdColumn;

    @FXML
    private TextField registrationNumberField;
    @FXML
    private TextField brandField;
    @FXML
    private TextField modelField;
    @FXML
    private TextField yearField;
    @FXML
    private TextField customerIdField;
    @FXML
    private Label statusLabel;

    @FXML
    private void handleCreateVehicle(){
        String registrationNumber = registrationNumberField.getText().trim();
        String brand = brandField.getText().trim();
        String model = modelField.getText().trim();
        int year = Integer.parseInt(yearField.getText().trim());
        int customerId = Integer.parseInt(customerIdField.getText().trim());

        try {
            Vehicle vehicle = garageSystem.createVehicle(registrationNumber, brand, model, year, customerId);
            statusLabel.setText("Vehicle " + vehicle  + " created!");
            clearFields();
        }
        catch(Exception e){
            statusLabel.setText("Could not create vehicle!");
        }
    }

    private void clearFields() {
        registrationNumberField.clear();
        brandField.clear();
        modelField.clear();
        yearField.clear();
        customerIdField.clear();
    }

    @FXML
    public void initialize() {
        vehicleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        vehicleRegistrationNumberColumn.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));
        vehicleBrandColumn.setCellValueFactory(new PropertyValueFactory<>("brand"));
        vehicleModelColumn.setCellValueFactory(new PropertyValueFactory<>("model"));
        vehicleYearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        vehicleCustomerIdColumn.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        List<Vehicle> vehicleList = Database.getVehicles();

        if (vehicleList.isEmpty()) {
            vehicleTableView.setPlaceholder(new Label("Hittade inga fordon"));
        }
        else {
            ObservableList<Vehicle> vehicleObservableList = FXCollections.observableArrayList(vehicleList);
            vehicleTableView.setItems(vehicleObservableList);
        }
    }
}
