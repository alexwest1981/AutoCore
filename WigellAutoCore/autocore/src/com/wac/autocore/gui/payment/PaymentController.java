package com.wac.autocore.gui.payment;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.service.GarageSystem;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;

public class PaymentController {

    @FXML
    private TableView<Payment> paymentTable;

    @FXML
    private TableColumn<Payment, Integer> idColumn;

    @FXML
    private TableColumn<Payment, Integer> invoiceIdColumn;

    @FXML
    private TableColumn<Payment, Double> amountColumn;

    @FXML
    private TableColumn<Payment, String> paymentTypeColumn;

    @FXML
    private TableColumn<Payment, LocalDateTime> paymentDateColumn;

    @FXML
    private TableColumn<Payment, Boolean> successfulColumn;

    @FXML
    private TextField invoiceIdField;

    @FXML
    private ComboBox<String> paymentTypeComboBox;

    private final GarageSystem garageSystem = new GarageSystem();

    @FXML
    private void handleProcessPayment() {

        int invoiceId;

        try {
            invoiceId = Integer.parseInt(invoiceIdField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid invoice ID, please enter a number.");
            return;
        }

        Invoice invoice = null;
        for (Invoice inv : Database.getInvoices()) {
            if (inv.getId() == invoiceId) {
                invoice = inv;
                break;
            }
        }

        if (invoice == null) {
            showAlert("Invoice not found.");
            return;
        }

        if (invoice.isPaid()) {
            showAlert("This invoice has already been paid.");
            return;
        }

        String paymentType = paymentTypeComboBox.getValue();

        if (paymentType == null) {
            showAlert("Please select a payment type.");
            return;
        }

        Payment payment = garageSystem.processPayment(invoiceId, paymentType);

        if (payment == null) {
            showAlert("Could not process payment.");
            return;
        }

        paymentTable.getItems().add(payment);
        invoiceIdField.clear();
        paymentTypeComboBox.setValue(null);
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        invoiceIdColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Payment, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", amount));
                }
            }
        });

        paymentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        successfulColumn.setCellValueFactory(new PropertyValueFactory<>("successful"));
        paymentTypeComboBox.getItems().addAll("CARD", "SWISH", "CASH");
        paymentTable.getItems().addAll(Database.getPayments());
    }

    public void refreshTable() {
        paymentTable.refresh();
    }
}
