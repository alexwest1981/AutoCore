package com.wac.autocore.gui.customers;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
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

public class customerController {

    GarageSystem garageSystem;

    @FXML
    private TableView<Customer> customerListView;

    @FXML
    private TableColumn<Customer, Integer> customerIdColumn;

    @FXML
    private TableColumn<Customer, String> customerNameColumn;

    @FXML
    private TableColumn<Customer, String> customerPhoneColumn;

    @FXML
    private TableColumn<Customer, String> customerEmailColumn;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;
    @FXML
    private Label statusLabel;

    @FXML
    private void handleCreateCustomer() {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        try {
            Customer customer = garageSystem.createCustomer(name, phone, email);
            statusLabel.setText("Customer "+ customer + " created!");
            clearFields();
        }
        catch (Exception e) {
            statusLabel.setText("Customer "+ name + " could not be created!");
        }
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
    }

    @FXML
    public void initialize() {

        customerIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        customerPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        customerEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        List<Customer> customersList = Database.getCustomers();

        if(customersList.isEmpty()) {
            customerListView.setPlaceholder(new Label("Hittade inga kunder"));
        }
        else {
            ObservableList<Customer> customerObservableList = FXCollections.observableArrayList(customersList);
            customerListView.setItems(customerObservableList);
        }
    }
}
