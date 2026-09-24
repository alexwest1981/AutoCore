package com.wac.autocore.gui.booking;

import com.wac.autocore.model.Booking;
import com.wac.autocore.service.GarageSystem;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalTime;

public class BookingController {

    @FXML
    private TableView<Booking> bookingTable;

    @FXML
    private TableColumn<Booking, Integer> idColumn;

    @FXML
    private TableColumn<Booking, Integer> vehicleIdColumn;

    @FXML
    private TableColumn<Booking, LocalDate> dateColumn;

    @FXML
    private TableColumn<Booking, String> descriptionColumn;

    @FXML
    private TableColumn<Booking, String> statusColumn;

    @FXML
    private TableColumn<Booking, LocalTime> startTimeColumn;

    @FXML
    private TableColumn<Booking, Integer> mechanicIdColumn;

    @FXML
    private TableColumn<Booking, Integer> serviceIdColumn;

    @FXML
    private TextField vehicleIdField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<String> hoursComboBox;

    @FXML
    private TextField mechanicIdField;

    @FXML
    private TextField serviceIdField;

    private final GarageSystem garageSystem = new GarageSystem();

    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        vehicleIdColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        mechanicIdColumn.setCellValueFactory(new PropertyValueFactory<>("mechanicId"));
        serviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("serviceId"));

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new TableCell<Booking, String>() {
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
                    } else if (status.equals("IN_PROGRESS") || status.equals("WORK_ORDER_CREATED")) {
                        setStyle("-fx-text-fill: orange;");
                    } else {
                        setStyle("-fx-text-fill: black;");
                    }
                }
            }
        });

        bookingTable.getItems().addAll(garageSystem.getBookings());
    }

    @FXML
    private void handleCreateBooking() {

        int vehicleId;

        try {
            vehicleId = Integer.parseInt(vehicleIdField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid vehicle ID, please enter a number.");
            return;
        }

        LocalDate date = datePicker.getValue();

        if (date == null) {
            showAlert("Please select a date.");
            return;
        }

        String description = descriptionField.getText();

        Booking booking = garageSystem.createBooking(vehicleId, date, description);

        if (booking == null) {
            showAlert("Could not create booking. Check that the vehicle exists.");
            return;
        }

        bookingTable.getItems().add(booking);
        vehicleIdField.clear();
        datePicker.setValue(null);
        descriptionField.clear();
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }

}