package com.wac.autocore.gui.workorder;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class WorkOrderController {

    @FXML
    private TableView<WorkOrder> workOrderTable;

    @FXML
    private TableColumn<WorkOrder, Integer> idColumn;

    @FXML
    private TableColumn<WorkOrder, Integer> bookingIdColumn;

    @FXML
    private TableColumn<WorkOrder, Integer> mechanicIdColumn;

    @FXML
    private TableColumn<WorkOrder, List<Integer>> serviceItemIdsColumn;

    @FXML
    private TableColumn<WorkOrder, String> statusColumn;

    @FXML
    private TextField bookingIdField;

    @FXML
    private TextField mechanicIdField;

    @FXML
    private TextField serviceItemIdsField;

    @FXML
    private TextField workOrderIdField;

    private final GarageSystem garageSystem = new GarageSystem();

    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        bookingIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        mechanicIdColumn.setCellValueFactory(new PropertyValueFactory<>("mechanicId"));
        serviceItemIdsColumn.setCellValueFactory(new PropertyValueFactory<>("serviceItemIds"));

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new TableCell<WorkOrder, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if (status.equals("COMPLETED")) {
                        setStyle("-fx-text-fill: green;");
                    } else if (status.equals("IN_PROGRESS")) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });

        workOrderTable.getItems().addAll(garageSystem.getWorkOrders());
    }

    @FXML
    private void handleCreateWorkOrder() {

        int bookingId;

        try {
            bookingId = Integer.parseInt(bookingIdField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid booking ID, please enter a number.");
            return;
        }

        int mechanicId;

        try {
            mechanicId = Integer.parseInt(mechanicIdField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid mechanic ID, please enter a number.");
            return;
        }

        // SCRUM-156 (C1): arbetsordern får bokningens tjänster, fältet för tjänste-id används inte längre.
        WorkOrder workOrder = garageSystem.createWorkOrder(bookingId, mechanicId);

        if (workOrder == null) {
            showAlert("Could not create work order. Check that the booking and mechanic exist, "
                    + "that the mechanic is available and that the booking has services.");
            return;
        }

        workOrderTable.getItems().add(workOrder);
        bookingIdField.clear();
        mechanicIdField.clear();
        serviceItemIdsField.clear();

        // Längden räknas på det arbetsordern faktiskt fick, alltså bokningens tjänster.
        int[] bookingServiceIds = new int[workOrder.getServiceItemIds().size()];
        for (int i = 0; i < bookingServiceIds.length; i++) {
            bookingServiceIds[i] = workOrder.getServiceItemIds().get(i);
        }
        int duration = garageSystem.getEstimatedDuration(bookingServiceIds);
        System.out.println("Estimated duration for this work order: " + duration + " minutes");
    }

    @FXML
    private void handleStartWorkOrder() {

        WorkOrder workOrder = findWorkOrderFromField();

        if (workOrder == null) {
            return;
        }

        garageSystem.startWorkOrder(workOrder.getId());
        workOrderTable.refresh();
    }

    @FXML
    private void handleCompleteWorkOrder() {

        WorkOrder workOrder = findWorkOrderFromField();

        if (workOrder == null) {
            return;
        }

        garageSystem.completeWorkOrder(workOrder.getId());
        workOrderTable.refresh();
    }

    private WorkOrder findWorkOrderFromField() {

        int workOrderId;

        try {
            workOrderId = Integer.parseInt(workOrderIdField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid work order ID, please enter a number.");
            return null;
        }

        for (WorkOrder workOrder : garageSystem.getWorkOrders()) {
            if (workOrder.getId() == workOrderId) {
                return workOrder;
            }
        }

        showAlert("Work order with ID " + workOrderId + " does not exist.");
        return null;
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }

}