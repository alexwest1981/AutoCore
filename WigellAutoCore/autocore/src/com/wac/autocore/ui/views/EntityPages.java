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
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * Fabrik för att bygga enhetliga sidor och tabeller för varje domänentitet med i18n-stöd.
 */
@SuppressWarnings("unchecked")
public final class EntityPages {

    private EntityPages() {}

    public static VBox buildCustomersPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Customer> table = TableFactory.create(garage.getCustomers());
        TableView<Customer> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 60, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.name"), 200, Customer::getName),
                TableFactory.col(I18n.get("table.col.phone"), 150, Customer::getPhone),
                TableFactory.col(I18n.get("table.col.email"), 280, Customer::getEmail),
                TableFactory.badgeCol(I18n.get("table.col.vip"), 100, c -> c.isVip() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.customers.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateCustomerDialog(garage, () -> router.navigate("customers")));

        return UiComponents.buildEntityPage(
                I18n.get("entity.customers.title"),
                I18n.get("entity.customers.meta", garage.getCustomers().size()),
                I18n.get("entity.customers.subtitle"),
                t, addBtn);
    }

    public static VBox buildVehiclesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Vehicle> table = TableFactory.create(garage.getVehicles());
        TableView<Vehicle> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 60, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.reg_nr"), 120, Vehicle::getRegistrationNumber),
                TableFactory.col(I18n.get("table.col.brand"), 140, Vehicle::getBrand),
                TableFactory.col(I18n.get("table.col.model"), 160, Vehicle::getModel),
                TableFactory.col(I18n.get("table.col.year"), 100, c -> String.valueOf(c.getYear())),
                TableFactory.col(I18n.get("table.col.customer"), 220, c -> EntityLookup.customerName(garage, c.getCustomerId())));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.vehicles.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateVehicleDialog(garage, () -> router.navigate("vehicles")));

        return UiComponents.buildEntityPage(
                I18n.get("entity.vehicles.title"),
                I18n.get("entity.vehicles.meta", garage.getVehicles().size()),
                I18n.get("entity.vehicles.subtitle"),
                t, addBtn);
    }

    public static VBox buildBookingsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Booking> table = TableFactory.create(garage.getBookings());
        TableView<Booking> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 60, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.vehicle"), 140, c -> EntityLookup.vehicleReg(garage, c.getVehicleId())),
                TableFactory.col(I18n.get("table.col.date"), 130, c -> String.valueOf(c.getDate())),
                TableFactory.col(I18n.get("table.col.description"), 320, Booking::getDescription),
                TableFactory.badgeCol(I18n.get("table.col.status"), 140, c -> UiFormatters.statusWord(c.getStatus())));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.bookings.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> router.navigate("bookings")));

        return UiComponents.buildEntityPage(
                I18n.get("entity.bookings.title"),
                I18n.get("entity.bookings.meta", garage.getBookings().size()),
                I18n.get("entity.bookings.subtitle"),
                t, addBtn);
    }

    public static VBox buildWorkOrdersPage(GarageSystem garage, PageRouter router) {
        FilterableTable<WorkOrder> table = TableFactory.create(garage.getWorkOrders());
        TableView<WorkOrder> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 70, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.booking"), 90, c -> String.valueOf(c.getBookingId())),
                TableFactory.col(I18n.get("table.col.mechanic"), 180, c -> EntityLookup.mechanicName(garage, c.getMechanicId())),
                TableFactory.col(I18n.get("table.col.services"), 300, c -> EntityLookup.serviceNames(garage, c.getServiceItemIds())),
                TableFactory.badgeCol(I18n.get("table.col.status"), 140, c -> UiFormatters.statusWord(c.getStatus())));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.workorders.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> router.navigate("workorders")));

        Button startBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_start"));
        Button completeBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_complete"));
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
                I18n.get("entity.workorders.title"),
                I18n.get("entity.workorders.meta", garage.getWorkOrders().size()),
                I18n.get("entity.workorders.subtitle"),
                t, startBtn, completeBtn, addBtn);
    }

    public static VBox buildServicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<ServiceItem> table = TableFactory.create(garage.getServiceItems());
        TableView<ServiceItem> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 70, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.name"), 220, ServiceItem::getName),
                TableFactory.col(I18n.get("table.col.description"), 360, ServiceItem::getDescription),
                TableFactory.col(I18n.get("table.col.price"), 120, c -> UiFormatters.formatMoney(c.getPrice())),
                TableFactory.col(I18n.get("table.col.time"), 100, c -> c.getEstimatedMinutes() + " min"));
        router.setActiveTable(table);

        return UiComponents.buildEntityPage(
                I18n.get("entity.services.title"),
                I18n.get("entity.services.meta", garage.getServiceItems().size()),
                I18n.get("entity.services.subtitle"),
                t);
    }

    public static VBox buildMechanicsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Mechanic> table = TableFactory.create(garage.getMechanics());
        TableView<Mechanic> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 70, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.name"), 220, Mechanic::getName),
                TableFactory.col(I18n.get("table.col.phone"), 160, Mechanic::getPhone),
                TableFactory.col(I18n.get("table.col.specialisation"), 260, Mechanic::getSpecialization),
                TableFactory.badgeCol(I18n.get("table.col.available"), 130, c -> c.isAvailable() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        return UiComponents.buildEntityPage(
                I18n.get("entity.mechanics.title"),
                I18n.get("entity.mechanics.meta", garage.getMechanics().size()),
                I18n.get("entity.mechanics.subtitle"),
                t);
    }

    public static VBox buildInvoicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Invoice> table = TableFactory.create(garage.getInvoices());
        TableView<Invoice> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 70, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.workorder"), 110, c -> String.valueOf(c.getWorkOrderId())),
                TableFactory.col(I18n.get("table.col.date"), 130, c -> String.valueOf(c.getInvoiceDate())),
                TableFactory.col(I18n.get("table.col.amount"), 110, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.col(I18n.get("table.col.discount"), 100, c -> UiFormatters.formatMoney(c.getDiscount())),
                TableFactory.col(I18n.get("table.col.total"), 110, c -> UiFormatters.formatMoney(c.getTotalAmount())),
                TableFactory.badgeCol(I18n.get("table.col.paid"), 110, c -> c.isPaid() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.invoices.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> router.navigate("invoices")));

        Button payBtn = UiComponents.secondaryButton(I18n.get("entity.invoices.action_pay"));
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
                I18n.get("entity.invoices.title"),
                I18n.get("entity.invoices.meta", garage.getInvoices().size()),
                I18n.get("entity.invoices.subtitle"),
                t, payBtn, addBtn);
    }

    public static VBox buildPaymentsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Payment> table = TableFactory.create(garage.getPayments());
        TableView<Payment> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col(I18n.get("table.col.id"), 70, c -> String.valueOf(c.getId())),
                TableFactory.col(I18n.get("table.col.invoice"), 100, c -> String.valueOf(c.getInvoiceId())),
                TableFactory.col(I18n.get("table.col.amount"), 120, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.col(I18n.get("table.col.type"), 130, Payment::getPaymentType),
                TableFactory.col(I18n.get("table.col.datetime"), 220, c -> String.valueOf(c.getPaymentDate())),
                TableFactory.badgeCol(I18n.get("table.col.status"), 110, c -> c.isSuccessful() ? I18n.get("status.successful") : I18n.get("status.failed")));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.payments.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, () -> router.navigate("payments")));

        return UiComponents.buildEntityPage(
                I18n.get("entity.payments.title"),
                I18n.get("entity.payments.meta", garage.getPayments().size()),
                I18n.get("entity.payments.subtitle"),
                t, addBtn);
    }
}
