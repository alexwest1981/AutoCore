package com.wac.autocore.gui.invoice;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.service.GarageSystem;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;

public class InvoiceController {

    @FXML
    private TableView<Invoice> invoiceTable;

    @FXML
    private TableColumn<Invoice, Integer> idColumn;

    @FXML
    private TableColumn<Invoice, Integer> workOrderIdColumn;

    @FXML
    private TableColumn<Invoice, LocalDate> dateColumn;

    @FXML
    private TableColumn<Invoice, Double> amountColumn;

    @FXML
    private TableColumn<Invoice, Double> discountColumn;

    @FXML
    private TableColumn<Invoice, Double> totalAmountColumn;

    @FXML
    private TableColumn<Invoice, Boolean> statusColumn;

    @FXML
    private TextField workOrderIdField;

    @FXML
    private TextField discountCodeField;

    private final GarageSystem garageSystem = new GarageSystem();

    @FXML
    private void handleCreateInvoice(){

        int workOrderId;

        try {
            workOrderId = Integer.parseInt(workOrderIdField.getText());
        } catch (NumberFormatException e) {
            showAlert("Invalid work order ID, please enter a number.");
            return;
        }

        String discountCode = discountCodeField.getText();
        Invoice invoice = garageSystem.createInvoice(workOrderId, discountCode);

        if (invoice == null) {
            showAlert("Could not create invoice. Check that the work order exists and is marked as completed.");
            return;
        }

        invoiceTable.getItems().add(invoice);
        workOrderIdField.clear();
        discountCodeField.clear();
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        workOrderIdColumn.setCellValueFactory(new PropertyValueFactory<>("workOrderId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("%.2f", amount));
            }
        });

        discountColumn.setCellValueFactory(new PropertyValueFactory<>("discount"));
        discountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double discount, boolean empty) {
                super.updateItem(discount, empty);
                setText(empty || discount == null ? null : String.format("%.2f", discount));
            }
        });

        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalAmountColumn.setCellFactory(column -> new javafx.scene.control.TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double totalAmount, boolean empty) {
                super.updateItem(totalAmount, empty);
                setText(empty || totalAmount == null ? null : String.format("%.2f", totalAmount));
            }
        });

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("paid"));
        invoiceTable.getItems().addAll(garageSystem.getInvoices());
    }

    public void refreshTable() {
        invoiceTable.refresh();
    }
}
