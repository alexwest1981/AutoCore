package com.wac.autocore.gui.customers;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;

public class customerController {

    @FXML
    private TableView<Customer> customerListView;

    @FXML
    public void initialize() {
        List<Customer> customersList = Database.getCustomers();

        if(customersList == null || customersList.isEmpty()) {
            customerListView.setPlaceholder(new Label("Hittade inga kunder"));
        }
        else {
            ObservableList<Customer> customerObservableList = FXCollections.observableArrayList(customersList);
            customerListView.setItems(customerObservableList);
        }
    }
}
