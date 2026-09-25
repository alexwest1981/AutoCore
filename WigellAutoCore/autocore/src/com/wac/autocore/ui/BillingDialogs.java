package com.wac.autocore.ui;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Modala dialoger för fakturering och betalningar (skapa faktura, genomföra betalning).
 */
public final class BillingDialogs {

    private BillingDialogs() {}

    public static void showCreateInvoiceDialog(GarageSystem garage, Runnable onSuccess) {
        List<WorkOrder> completedOrders = new ArrayList<WorkOrder>();
        for (WorkOrder wo : garage.getWorkOrders()) {
            if ("COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                boolean alreadyInvoiced = false;
                for (Invoice inv : garage.getInvoices()) {
                    if (inv.getWorkOrderId() == wo.getId()) {
                        alreadyInvoiced = true;
                        break;
                    }
                }
                if (!alreadyInvoiced) {
                    completedOrders.add(wo);
                }
            }
        }

        if (completedOrders.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("overview.empty.workorders"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.invoice.create.title"));
        dialog.setHeaderText(I18n.get("dialog.invoice.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<WorkOrder> orderBox = new ComboBox<WorkOrder>();
        orderBox.getItems().addAll(completedOrders);
        orderBox.getSelectionModel().selectFirst();
        orderBox.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder wo) {
                return wo == null ? "" : I18n.get("table.col.workorder") + " #" + wo.getId() + " (" + I18n.get("table.col.booking") + " #" + wo.getBookingId() + ")";
            }
            @Override
            public WorkOrder fromString(String string) { return null; }
        });

        TextField discountField = new TextField();
        discountField.setPromptText(I18n.get("dialog.invoice.discount_prompt"));

        grid.add(new Label(I18n.get("dialog.invoice.workorder_select") + ":"), 0, 0);
        grid.add(orderBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.invoice.discount") + ":"), 0, 1);
        grid.add(discountField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                WorkOrder wo = orderBox.getValue();
                String code = discountField.getText().trim();
                garage.createInvoice(wo.getId(), code);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showProcessPaymentDialog(GarageSystem garage, Invoice preselected, Runnable onSuccess) {
        List<Invoice> unpaid = new ArrayList<Invoice>();
        for (Invoice inv : garage.getInvoices()) {
            if (!inv.isPaid()) {
                unpaid.add(inv);
            }
        }

        if (unpaid.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("common.close"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.payment.create.title"));
        dialog.setHeaderText(I18n.get("dialog.payment.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<Invoice> invoiceBox = new ComboBox<Invoice>();
        invoiceBox.getItems().addAll(unpaid);
        if (preselected != null && unpaid.contains(preselected)) {
            invoiceBox.getSelectionModel().select(preselected);
        } else {
            invoiceBox.getSelectionModel().selectFirst();
        }
        invoiceBox.setConverter(new StringConverter<Invoice>() {
            @Override
            public String toString(Invoice inv) {
                return inv == null ? "" : I18n.get("table.col.invoice") + " #" + inv.getId() + " - " + inv.getTotalAmount() + " " + I18n.get("common.currency") + " (" + I18n.get("table.col.workorder") + " #" + inv.getWorkOrderId() + ")";
            }
            @Override
            public Invoice fromString(String string) { return null; }
        });

        ComboBox<String> typeBox = new ComboBox<String>();
        typeBox.getItems().addAll("SWISH", "CARD", "CASH");
        typeBox.getSelectionModel().select("SWISH");

        grid.add(new Label(I18n.get("dialog.payment.invoice_select") + ":"), 0, 0);
        grid.add(invoiceBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.payment.method") + ":"), 0, 1);
        grid.add(typeBox, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Invoice inv = invoiceBox.getValue();
                String paymentType = typeBox.getValue();

                garage.processPayment(inv.getId(), paymentType);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
