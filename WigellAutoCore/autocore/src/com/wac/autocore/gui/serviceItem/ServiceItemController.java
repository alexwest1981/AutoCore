package com.wac.autocore.gui.serviceItem;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.GarageSystem;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ServiceItemController {

    private final GarageSystem garageSystem = new GarageSystem();

    @FXML
    private TableView<ServiceItem> serviceItemTable;

    @FXML
    private TableColumn<ServiceItem, Integer> idColumn;

    @FXML
    private TableColumn<ServiceItem, String> nameColumn;

    @FXML
    private TableColumn<ServiceItem, String> descriptionColumn;

    @FXML
    private TableColumn<ServiceItem, Double> priceColumn;

    @FXML
    private TableColumn<ServiceItem, Integer> estimatedMinutesColumn;

    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(column -> new javafx.scene.control.TableCell<ServiceItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("%.2f", price));
            }
        });
        estimatedMinutesColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedMinutes"));
        serviceItemTable.getItems().addAll(garageSystem.getServiceItems());
    }

    public void refreshTable() {
        serviceItemTable.refresh();
    }
}
