package com.wac.autocore.gui.customers;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class customerController {

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
