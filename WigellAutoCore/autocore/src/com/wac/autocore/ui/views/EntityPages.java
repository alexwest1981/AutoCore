package com.wac.autocore.ui.views;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * Fabrik för att bygga enhetliga sidor och tabeller för varje domänentitet.
 */
public final class EntityPages {

    private EntityPages() {}

    public static VBox buildCustomersPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Customer> table = TableFactory.create(garage.getCustomers());
        router.setActiveTable(table);

        TableView<Customer> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 60, c -> String.valueOf(c.getId())),
                TableFactory.col("Name", 200, Customer::getName),
                TableFactory.col("Phone", 150, Customer::getPhone),
                TableFactory.col("Email", 280, Customer::getEmail),
                TableFactory.badgeCol("VIP", 100, c -> c.isVip() ? "Yes" : "No"));

        Button addBtn = UiComponents.primaryButton("+ New customer");
        addBtn.setOnAction(e -> ActionDialogs.showCreateCustomerDialog(garage, () -> router.navigate("customers")));

        return UiComponents.buildEntityPage(
                "Customers", garage.getCustomers().size() + " registered",
                "Names, contact details and VIP status",
                t, addBtn);
    }

    public static VBox buildVehiclesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Vehicle> table = TableFactory.create(garage.getVehicles());
        router.setActiveTable(table);

        TableView<Vehicle> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 60, c -> String.valueOf(c.getId())),
                TableFactory.col("Reg. no.", 120, Vehicle::getRegistrationNumber),
                TableFactory.col("Make", 140, Vehicle::getBrand),
                TableFactory.col("Model", 160, Vehicle::getModel),
                TableFactory.col("Year", 100, c -> String.valueOf(c.getYear())),
                TableFactory.col("Customer", 220, c -> EntityLookup.customerName(garage, c.getCustomerId())));

        Button addBtn = UiComponents.primaryButton("+ Register vehicle");
        addBtn.setOnAction(e -> ActionDialogs.showCreateVehicleDialog(garage, () -> router.navigate("vehicles")));

        return UiComponents.buildEntityPage(
                "Vehicles", garage.getVehicles().size() + " registered",
                "Vehicles registered at the workshop",
                t, addBtn);
    }

    public static VBox buildBookingsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Booking> table = TableFactory.create(garage.getBookings());
        router.setActiveTable(table);

        TableView<Booking> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 60, c -> String.valueOf(c.getId())),
                TableFactory.col("Vehicle", 140, c -> EntityLookup.vehicleReg(garage, c.getVehicleId())),
                TableFactory.col("Date", 130, c -> String.valueOf(c.getDate())),
                TableFactory.col("Description", 320, Booking::getDescription),
                TableFactory.badgeCol("Status", 140, c -> UiFormatters.statusWord(c.getStatus())));

        Button addBtn = UiComponents.primaryButton("+ New booking");
        addBtn.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> router.navigate("bookings")));

        return UiComponents.buildEntityPage(
                "Bookings", garage.getBookings().size() + " bookings",
                "Scheduled jobs and their status",
                t, addBtn);
    }

    public static VBox buildWorkOrdersPage(GarageSystem garage, PageRouter router) {
        FilterableTable<WorkOrder> table = TableFactory.create(garage.getWorkOrders());
        router.setActiveTable(table);

        TableView<WorkOrder> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 70, c -> String.valueOf(c.getId())),
                TableFactory.col("Booking", 90, c -> String.valueOf(c.getBookingId())),
                TableFactory.col("Mechanic", 180, c -> EntityLookup.mechanicName(garage, c.getMechanicId())),
                TableFactory.col("Services", 300, c -> EntityLookup.serviceNames(garage, c.getServiceItemIds())),
                TableFactory.badgeCol("Status", 140, c -> UiFormatters.statusWord(c.getStatus())));

        Button addBtn = UiComponents.primaryButton("+ New work order");
        addBtn.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> router.navigate("workorders")));

        Button startBtn = UiComponents.secondaryButton("▶ Start order");
        Button completeBtn = UiComponents.secondaryButton("✔ Complete order");
        startBtn.setDisable(true);
        completeBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            startBtn.setDisable(sel == null || !"CREATED".equals(sel.getStatus()));
            completeBtn.setDisable(sel == null || !"IN_PROGRESS".equals(sel.getStatus()));
        });

        startBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && "CREATED".equals(sel.getStatus())) {
                garage.startWorkOrder(sel.getId());
                router.navigate("workorders");
            }
        });

        completeBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && "IN_PROGRESS".equals(sel.getStatus())) {
                garage.completeWorkOrder(sel.getId());
                router.navigate("workorders");
            }
        });

        return UiComponents.buildEntityPage(
                "Work orders", garage.getWorkOrders().size() + " work orders",
                "Active and completed jobs (select a row to start / complete)",
                t, startBtn, completeBtn, addBtn);
    }

    public static VBox buildServicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<ServiceItem> table = TableFactory.create(garage.getServiceItems());
        router.setActiveTable(table);

        TableView<ServiceItem> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 70, c -> String.valueOf(c.getId())),
                TableFactory.col("Name", 220, ServiceItem::getName),
                TableFactory.col("Description", 360, ServiceItem::getDescription),
                TableFactory.col("Price", 120, c -> UiFormatters.formatMoney(c.getPrice())),
                TableFactory.col("Time", 100, c -> c.getEstimatedMinutes() + " min"));

        return UiComponents.buildEntityPage(
                "Services", garage.getServiceItems().size() + " services",
                "Price list for workshop services",
                t);
    }

    public static VBox buildMechanicsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Mechanic> table = TableFactory.create(garage.getMechanics());
        router.setActiveTable(table);

        TableView<Mechanic> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 70, c -> String.valueOf(c.getId())),
                TableFactory.col("Name", 220, Mechanic::getName),
                TableFactory.col("Phone", 160, Mechanic::getPhone),
                TableFactory.col("Specialisation", 260, Mechanic::getSpecialization),
                TableFactory.badgeCol("Available", 130, c -> c.isAvailable() ? "Yes" : "No"));

        return UiComponents.buildEntityPage(
                "Mechanics", garage.getMechanics().size() + " employees",
                "Team, specialisation and availability",
                t);
    }

    public static VBox buildInvoicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Invoice> table = TableFactory.create(garage.getInvoices());
        router.setActiveTable(table);

        TableView<Invoice> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 70, c -> String.valueOf(c.getId())),
                TableFactory.col("Work order", 110, c -> String.valueOf(c.getWorkOrderId())),
                TableFactory.col("Date", 130, c -> String.valueOf(c.getInvoiceDate())),
                TableFactory.col("Amount", 110, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.col("Discount", 100, c -> UiFormatters.formatMoney(c.getDiscount())),
                TableFactory.col("Total", 110, c -> UiFormatters.formatMoney(c.getTotalAmount())),
                TableFactory.badgeCol("Paid", 110, c -> c.isPaid() ? "Yes" : "No"));

        Button addBtn = UiComponents.primaryButton("+ Create invoice");
        addBtn.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> router.navigate("invoices")));

        Button payBtn = UiComponents.secondaryButton("💳 Pay selected invoice");
        payBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            payBtn.setDisable(sel == null || sel.isPaid());
        });

        payBtn.setOnAction(e -> {
            Invoice sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && !sel.isPaid()) {
                ActionDialogs.showProcessPaymentDialog(garage, sel, () -> router.navigate("invoices"));
            }
        });

        return UiComponents.buildEntityPage(
                "Invoices", garage.getInvoices().size() + " invoices",
                "Issued invoices and payment status",
                t, payBtn, addBtn);
    }

    public static VBox buildPaymentsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Payment> table = TableFactory.create(garage.getPayments());
        router.setActiveTable(table);

        TableView<Payment> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 70, c -> String.valueOf(c.getId())),
                TableFactory.col("Invoice", 100, c -> String.valueOf(c.getInvoiceId())),
                TableFactory.col("Amount", 120, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.col("Type", 130, Payment::getPaymentType),
                TableFactory.col("Date/time", 220, c -> String.valueOf(c.getPaymentDate())),
                TableFactory.badgeCol("Status", 110, c -> c.isSuccessful() ? "Successful" : "Failed"));

        Button addBtn = UiComponents.primaryButton("+ Register payment");
        addBtn.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, () -> router.navigate("payments")));

        return UiComponents.buildEntityPage(
                "Payments", garage.getPayments().size() + " payments",
                "Received payments and their status",
                t, addBtn);
    }
}
