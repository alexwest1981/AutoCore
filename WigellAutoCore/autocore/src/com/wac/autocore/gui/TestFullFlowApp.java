package com.wac.autocore.gui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class TestFullFlowApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        WorkOrder testOrder = new WorkOrder(200, 1, 1);
        testOrder.addServiceItem(1);
        testOrder.addServiceItem(2);
        testOrder.setStatus("COMPLETED");
        Database.getWorkOrders().add(testOrder);

        Parent invoiceView = FXMLLoader.load(getClass().getResource("/com/wac/autocore/gui/invoice/InvoiceView.fxml"));
        Parent paymentView = FXMLLoader.load(
                getClass().getResource("/com/wac/autocore/gui/payment/PaymentView.fxml")
        );

        Tab invoiceTab = new Tab("Invoices", invoiceView);
        Tab paymentTab = new Tab("Payments", paymentView);
        invoiceTab.setClosable(false);
        paymentTab.setClosable(false);

        TabPane tabPane = new TabPane(invoiceTab, paymentTab);
        stage.setScene(new Scene(tabPane, 700, 500));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}