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
    private TableColumn<Booking, Integer> serviceItemIdColumn;

    @FXML
    private TextField vehicleIdField;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField startTimeField;

    @FXML
    private TextField mechanicIdField;

    @FXML
    private TextField serviceItemField;

    private final GarageSystem garageSystem = new GarageSystem();

    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        vehicleIdColumn.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        mechanicIdColumn.setCellValueFactory(new PropertyValueFactory<>("mechanicId"));
        serviceItemIdColumn.setCellValueFactory(new PropertyValueFactory<>("serviceItemId"));

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

        // 1. Hämta och validera starttid (HH:mm)
        java.time.LocalTime startTime;
        try {
            startTime = java.time.LocalTime.parse(startTimeField.getText());
        } catch (Exception e) {
            showAlert("Please enter a valid time (format HH:mm, e.g., 08:30).");
            return;
        }

        // 2. Hämta ID för mekaniker och tjänst från textfälten
        int mechanicId;
        int serviceItemId;
        try {
            mechanicId = Integer.parseInt(mechanicIdField.getText());
            serviceItemId = Integer.parseInt(serviceItemField.getText());
        } catch (NumberFormatException e) {
            showAlert("Mechanic ID and Service Item ID must be numbers.");
            return;
        }

        String description = descriptionField.getText();

        // 3. Omslut med try-catch för att fånga krockar och databasfel
        try {
            // Skicka med alla 6 parametrar till din Facade (GarageSystem)
            // Ordning: vehicleId, date, description, startTime, mechanicId, serviceItemId
            Booking booking = garageSystem.createBooking(
                    vehicleId,
                    date,
                    description,
                    startTime,
                    mechanicId,
                    serviceItemId
            );

            if (booking == null) {
                showAlert("Could not create booking. Check that the vehicle exists.");
                return;
            }

            // Om allt gick bra, lägg till i tabellen och rensa alla fält
            bookingTable.getItems().add(booking);

            vehicleIdField.clear();
            datePicker.setValue(null);
            descriptionField.clear();
            startTimeField.clear();
            mechanicIdField.clear();
            serviceItemField.clear();

        } catch (IllegalArgumentException e) {
            // 4. Här fångas felmeddelandet om mekanikern är upptagen!
            showAlert(e.getMessage());
        } catch (java.sql.SQLException e) {
            // Fångar upp eventuella databasfel
            showAlert("Database error: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }

}