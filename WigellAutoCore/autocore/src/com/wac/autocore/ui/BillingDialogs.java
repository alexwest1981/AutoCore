package com.wac.autocore.ui;

import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;
import com.wac.autocore.seed.SeedText;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
        grid.setPrefWidth(500);

        ComboBox<WorkOrder> orderBox = new ComboBox<WorkOrder>();
        orderBox.getItems().addAll(completedOrders);
        orderBox.setMaxWidth(Double.MAX_VALUE);
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
                Invoice invoice = garage.createInvoice(wo.getId(), code);
                if (invoice != null) {
                    showInvoiceLinesDialog(invoice);
                }
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
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.payment.no_unpaid_invoices"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.payment.create.title"));
        dialog.setHeaderText(I18n.get("dialog.payment.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();
        grid.setPrefWidth(540);

        ComboBox<Invoice> invoiceBox = new ComboBox<Invoice>();
        invoiceBox.getItems().addAll(unpaid);
        invoiceBox.setMaxWidth(Double.MAX_VALUE);
        if (preselected != null && unpaid.contains(preselected)) {
            invoiceBox.getSelectionModel().select(preselected);
        } else {
            invoiceBox.getSelectionModel().selectFirst();
        }
        invoiceBox.setConverter(new StringConverter<Invoice>() {
            @Override
            public String toString(Invoice inv) {
                return inv == null ? "" : I18n.get("table.col.invoice") + " #" + inv.getId() + " - " + UiFormatters.formatMoney(inv.getTotalAmount()) + " (" + I18n.get("table.col.workorder") + " #" + inv.getWorkOrderId() + ")";
            }
            @Override
            public Invoice fromString(String string) { return null; }
        });

        ComboBox<String> typeBox = new ComboBox<String>();
        typeBox.getItems().addAll("SWISH", "CARD", "CASH");
        typeBox.setMaxWidth(Double.MAX_VALUE);
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

    static void showInvoiceLinesDialog(Invoice invoice) {
        if (invoice == null) return;
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("table.col.invoice") + " #" + invoice.getId());
        dialog.setHeaderText(I18n.get("table.col.invoice") + " #" + invoice.getId()
                + " (" + I18n.get("table.col.workorder") + " #" + invoice.getWorkOrderId()
                + ", " + invoice.getInvoiceDate() + ")");
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(14));

        Label notice = new Label(I18n.get("dialog.workorder.historical_notice"));
        notice.getStyleClass().addAll("srow-sub", "small");

        GridPane grid = ActionDialogs.createGrid();
        Label h1 = new Label(I18n.get("table.col.service"));
        h1.setStyle("-fx-font-weight: bold;");
        Label h2 = new Label(I18n.get("table.col.price"));
        h2.setStyle("-fx-font-weight: bold;");
        Label h3 = new Label(I18n.get("table.col.discount"));
        h3.setStyle("-fx-font-weight: bold;");
        Label h4 = new Label(I18n.get("table.col.final_price"));
        h4.setStyle("-fx-font-weight: bold;");

        grid.add(h1, 0, 0);
        grid.add(h2, 1, 0);
        grid.add(h3, 2, 0);
        grid.add(h4, 3, 0);

        int row = 1;
        if (invoice.getLines() != null) {
            for (InvoiceLine line : invoice.getLines()) {
                grid.add(new Label(SeedText.resolve(line.getServiceName())), 0, row);
                grid.add(new Label(UiFormatters.formatMoney(line.getPrice())), 1, row);
                grid.add(new Label(UiFormatters.formatMoney(line.getDiscount())), 2, row);
                grid.add(new Label(UiFormatters.formatMoney(line.getFinalPrice())), 3, row);
                row++;
            }
        }

        // Summary row
        Label totalLabel = new Label(I18n.get("table.col.total") + ":");
        totalLabel.setStyle("-fx-font-weight: bold;");
        Label totalVal = new Label(UiFormatters.formatMoney(invoice.getTotalAmount()));
        totalVal.setStyle("-fx-font-weight: bold;");
        grid.add(totalLabel, 2, row);
        grid.add(totalVal, 3, row);

        content.getChildren().addAll(notice, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }
}
