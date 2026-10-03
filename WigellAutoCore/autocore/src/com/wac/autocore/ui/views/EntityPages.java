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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import com.wac.autocore.seed.SeedText;

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
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, Customer::getName),
                TableFactory.sizeCol(I18n.get("table.col.phone"), TableFactory.W_PHONE, Customer::getPhone),
                TableFactory.textCol(I18n.get("table.col.email"), TableFactory.W_EMAIL_MIN, TableFactory.W_EMAIL_MAX, Customer::getEmail),
                TableFactory.sizeBadge(I18n.get("table.col.vip"), TableFactory.W_FLAG, c -> c.isVip() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.customers.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateCustomerDialog(garage, () -> router.navigate("customers")));

        Button editBtn = UiComponents.secondaryButton(I18n.get("entity.customers.action_edit"));
        Button deleteBtn = UiComponents.secondaryButton(I18n.get("entity.customers.action_delete"));
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
        });

        editBtn.setOnAction(e -> {
            Customer sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showEditCustomerDialog(garage, sel, () -> router.navigate("customers"));
            }
        });

        deleteBtn.setOnAction(e -> {
            Customer sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showDeleteCustomerConfirmation(garage, sel, () -> router.navigate("customers"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<Customer>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditCustomerDialog(garage, row.getItem(), () -> router.navigate("customers"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.customers.title"),
                I18n.get("entity.customers.meta", garage.getCustomers().size()),
                I18n.get("entity.customers.subtitle"),
                t, deleteBtn, editBtn, addBtn);
    }

    public static VBox buildVehiclesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Vehicle> table = TableFactory.create(garage.getVehicles());
        TableView<Vehicle> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.reg_nr"), TableFactory.W_REG_NR, Vehicle::getRegistrationNumber),
                TableFactory.sizeCol(I18n.get("table.col.brand"), TableFactory.W_BRAND, Vehicle::getBrand),
                TableFactory.sizeCol(I18n.get("table.col.model"), TableFactory.W_MODEL, Vehicle::getModel),
                TableFactory.sizeCol(I18n.get("table.col.year"), TableFactory.W_YEAR, c -> String.valueOf(c.getYear())),
                TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.customerName(garage, c.getCustomerId())));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.vehicles.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateVehicleDialog(garage, () -> router.navigate("vehicles")));

        Button editBtn = UiComponents.secondaryButton(I18n.get("entity.vehicles.action_edit"));
        Button deleteBtn = UiComponents.secondaryButton(I18n.get("entity.vehicles.action_delete"));
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
        });

        editBtn.setOnAction(e -> {
            Vehicle sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showEditVehicleDialog(garage, sel, () -> router.navigate("vehicles"));
            }
        });

        deleteBtn.setOnAction(e -> {
            Vehicle sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showDeleteVehicleConfirmation(garage, sel, () -> router.navigate("vehicles"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<Vehicle>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditVehicleDialog(garage, row.getItem(), () -> router.navigate("vehicles"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.vehicles.title"),
                I18n.get("entity.vehicles.meta", garage.getVehicles().size()),
                I18n.get("entity.vehicles.subtitle"),
                t, deleteBtn, editBtn, addBtn);
    }

    public static VBox buildBookingsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Booking> table = TableFactory.create(garage.getBookings());
        TableView<Booking> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, c -> EntityLookup.vehicleReg(garage, c.getVehicleId())),
                TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, c -> String.valueOf(c.getDate())),
                TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_TIME, c -> c.getStartTime() != null ? (c.getEndTime() != null ? c.getStartTime() + " - " + c.getEndTime() : c.getStartTime().toString()) : "-"),
                TableFactory.textCol(I18n.get("table.col.services"), TableFactory.W_SERVICES_MIN, TableFactory.W_SERVICES_MAX, c -> EntityLookup.bookingServices(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.estimated_time"), TableFactory.W_MINUTES, c -> {
                    int min = EntityLookup.bookingTotalMinutes(garage, c);
                    return min > 0 ? min + " min" : "-";
                }),
                TableFactory.sizeCol(I18n.get("table.col.estimated_cost"), TableFactory.W_MONEY, c -> {
                    double cost = EntityLookup.bookingTotalPrice(garage, c);
                    return cost > 0 ? UiFormatters.formatMoney(cost) : "-";
                }),
                TableFactory.textCol(I18n.get("table.col.mechanic"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> c.getMechanicId() > 0 ? EntityLookup.mechanicName(garage, c.getMechanicId()) : "-"),
                TableFactory.textCol(I18n.get("table.col.description"), TableFactory.W_TEXT_MIN, TableFactory.W_TEXT_MAX, c -> SeedText.resolve(c.getDescription())),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> UiFormatters.statusWord(c.getStatus())));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.bookings.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, () -> router.navigate("bookings")));

        Button cancelBtn = UiComponents.secondaryButton(I18n.get("entity.bookings.action_cancel"));
        Button editBtn = UiComponents.secondaryButton(I18n.get("entity.bookings.action_edit"));
        Button deleteBtn = UiComponents.secondaryButton(I18n.get("entity.bookings.action_delete"));
        cancelBtn.setDisable(true);
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            cancelBtn.setDisable(sel == null || "CANCELLED".equalsIgnoreCase(sel.getStatus()));
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
        });

        editBtn.setOnAction(e -> {
            Booking sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showEditBookingDialog(garage, sel, () -> router.navigate("bookings"));
            }
        });

        cancelBtn.setOnAction(e -> {
            Booking sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showCancelBookingConfirmation(garage, sel, () -> router.navigate("bookings"));
            }
        });

        deleteBtn.setOnAction(e -> {
            Booking sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showDeleteBookingConfirmation(garage, sel, () -> router.navigate("bookings"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<Booking> row = new TableRow<Booking>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditBookingDialog(garage, row.getItem(), () -> router.navigate("bookings"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.bookings.title"),
                I18n.get("entity.bookings.meta", garage.getBookings().size()),
                I18n.get("entity.bookings.subtitle"),
                t, cancelBtn, deleteBtn, editBtn, addBtn);
    }

    public static VBox buildWorkOrdersPage(GarageSystem garage, PageRouter router) {
        FilterableTable<WorkOrder> table = TableFactory.create(garage.getWorkOrders());
        TableView<WorkOrder> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.booking"), TableFactory.W_REF, c -> String.valueOf(c.getBookingId())),
                TableFactory.textCol(I18n.get("table.col.mechanic"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.mechanicName(garage, c.getMechanicId())),
                TableFactory.textCol(I18n.get("table.col.services"), TableFactory.W_SERVICES_MIN, TableFactory.W_SERVICES_MAX, c -> EntityLookup.workOrderServices(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.total"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(EntityLookup.workOrderTotal(garage, c))),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> UiFormatters.statusWord(c.getStatus())));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.workorders.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, () -> router.navigate("workorders")));

        Button detailsBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_details"));
        detailsBtn.setDisable(true);
        detailsBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showWorkOrderDetailsDialog(garage, sel);
            }
        });

        Button startBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_start"));
        Button markBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_mark_performed"));
        Button completeBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_complete"));
        Button invoiceBtn = UiComponents.secondaryButton(I18n.get("entity.workorders.action_invoice"));
        startBtn.setDisable(true);
        markBtn.setDisable(true);
        completeBtn.setDisable(true);
        invoiceBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            detailsBtn.setDisable(sel == null);
            startBtn.setDisable(sel == null || !"CREATED".equals(sel.getStatus()));
            markBtn.setDisable(sel == null || !"IN_PROGRESS".equals(sel.getStatus()));
            completeBtn.setDisable(sel == null || (!"IN_PROGRESS".equals(sel.getStatus()) && !"CREATED".equals(sel.getStatus())));

            boolean canInvoice = false;
            if (sel != null && "COMPLETED".equals(sel.getStatus())) {
                // En arbetsorder kan vara fakturerad av en faktura som gäller hela bokningen, och då
                // ligger den på en annan arbetsorder. Samma regel som i fakturavyn: en källa.
                canInvoice = EntityLookup.invoiceForWorkOrder(garage, sel.getId()) == null;
            }
            invoiceBtn.setDisable(!canInvoice);
        });

        startBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && "CREATED".equals(sel.getStatus())) {
                garage.startWorkOrder(sel.getId());
                router.navigate("workorders");
            }
        });

        markBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && "IN_PROGRESS".equals(sel.getStatus())) {
                ActionDialogs.showMarkPerformedDialog(garage, sel, () -> router.navigate("workorders"));
            }
        });

        completeBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && ("IN_PROGRESS".equals(sel.getStatus()) || "CREATED".equals(sel.getStatus()))) {
                if ("CREATED".equals(sel.getStatus())) {
                    garage.startWorkOrder(sel.getId());
                }
                garage.completeWorkOrder(sel.getId());
                router.navigate("workorders");
            }
        });

        invoiceBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && "COMPLETED".equals(sel.getStatus())) {
                ActionDialogs.showCreateInvoiceDialog(garage, sel, () -> router.navigate("workorders"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<WorkOrder> row = new TableRow<WorkOrder>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showWorkOrderDetailsDialog(garage, row.getItem());
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.workorders.title"),
                I18n.get("entity.workorders.meta", garage.getWorkOrders().size()),
                I18n.get("entity.workorders.subtitle"),
                t, detailsBtn, startBtn, markBtn, completeBtn, invoiceBtn, addBtn);
    }

    public static VBox buildServicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<ServiceItem> table = TableFactory.create(garage.getServiceItems());
        TableView<ServiceItem> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> SeedText.resolve(c.getName())),
                TableFactory.textCol(I18n.get("table.col.description"), TableFactory.W_TEXT_MIN, TableFactory.W_TEXT_MAX, c -> SeedText.resolve(c.getDescription())),
                TableFactory.sizeCol(I18n.get("table.col.price"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getPrice())),
                TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_MINUTES, c -> c.getEstimatedMinutes() + " min"));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.services.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateServiceItemDialog(garage, () -> router.navigate("services")));

        Button editBtn = UiComponents.secondaryButton(I18n.get("entity.services.action_edit"));
        Button deleteBtn = UiComponents.secondaryButton(I18n.get("entity.services.action_delete"));
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
        });

        editBtn.setOnAction(e -> {
            ServiceItem sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showEditServiceItemDialog(garage, sel, () -> router.navigate("services"));
            }
        });

        deleteBtn.setOnAction(e -> {
            ServiceItem sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showDeleteServiceItemConfirmation(garage, sel, () -> router.navigate("services"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<ServiceItem> row = new TableRow<ServiceItem>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditServiceItemDialog(garage, row.getItem(), () -> router.navigate("services"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.services.title"),
                I18n.get("entity.services.meta", garage.getServiceItems().size()),
                I18n.get("entity.services.subtitle"),
                t, deleteBtn, editBtn, addBtn);
    }

    public static VBox buildMechanicsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Mechanic> table = TableFactory.create(garage.getMechanics());
        TableView<Mechanic> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, Mechanic::getName),
                TableFactory.sizeCol(I18n.get("table.col.phone"), TableFactory.W_PHONE, Mechanic::getPhone),
                TableFactory.textCol(I18n.get("table.col.specialisation"), TableFactory.W_SPEC_MIN, TableFactory.W_SPEC_MAX, c -> SeedText.resolve(c.getSpecialization())),
                TableFactory.sizeBadge(I18n.get("table.col.available"), TableFactory.W_FLAG, c -> c.isAvailable() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.mechanics.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateMechanicDialog(garage, () -> router.navigate("mechanics")));

        Button editBtn = UiComponents.secondaryButton(I18n.get("entity.mechanics.action_edit"));
        Button deleteBtn = UiComponents.secondaryButton(I18n.get("entity.mechanics.action_delete"));
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
        });

        editBtn.setOnAction(e -> {
            Mechanic sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showEditMechanicDialog(garage, sel, () -> router.navigate("mechanics"));
            }
        });

        deleteBtn.setOnAction(e -> {
            Mechanic sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showDeleteMechanicConfirmation(garage, sel, () -> router.navigate("mechanics"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<Mechanic> row = new TableRow<Mechanic>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditMechanicDialog(garage, row.getItem(), () -> router.navigate("mechanics"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.mechanics.title"),
                I18n.get("entity.mechanics.meta", garage.getMechanics().size()),
                I18n.get("entity.mechanics.subtitle"),
                t, deleteBtn, editBtn, addBtn);
    }

    public static VBox buildInvoicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Invoice> table = TableFactory.create(garage.getInvoices());
        TableView<Invoice> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.booking"), TableFactory.W_REF, c -> String.valueOf(EntityLookup.bookingIdForWorkOrder(garage, c.getWorkOrderId()))),
                TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.invoiceCustomerName(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, c -> String.valueOf(c.getInvoiceDate())),
                TableFactory.sizeCol(I18n.get("table.col.amount"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.sizeCol(I18n.get("table.col.discount"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getDiscount())),
                TableFactory.sizeCol(I18n.get("table.col.total"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getTotalIncludingVat())),
                TableFactory.sizeBadge(I18n.get("table.col.paid"), TableFactory.W_FLAG, c -> c.isPaid() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addBtn = UiComponents.primaryButton(I18n.get("entity.invoices.action_create"));
        addBtn.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, () -> router.navigate("invoices")));

        Button payBtn = UiComponents.secondaryButton(I18n.get("entity.invoices.action_pay"));
        payBtn.setDisable(true);

        Button linesBtn = UiComponents.secondaryButton(I18n.get("entity.invoices.action_lines"));
        linesBtn.setDisable(true);
        linesBtn.setOnAction(e -> {
            Invoice sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showInvoiceLinesDialog(sel);
            }
        });

        Button printBtn = UiComponents.secondaryButton(I18n.get("entity.invoices.action_print"));
        printBtn.setDisable(true);
        printBtn.setOnAction(e -> {
            Invoice sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showInvoiceDocumentDialog(garage, sel);
            }
        });

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            payBtn.setDisable(sel == null || sel.isPaid());
            linesBtn.setDisable(sel == null);
            printBtn.setDisable(sel == null);
        });

        payBtn.setOnAction(e -> {
            Invoice sel = t.getSelectionModel().getSelectedItem();
            if (sel != null && !sel.isPaid()) {
                ActionDialogs.showProcessPaymentDialog(garage, sel, () -> router.navigate("invoices"));
            }
        });

        t.setRowFactory(tv -> {
            TableRow<Invoice> row = new TableRow<Invoice>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showInvoiceLinesDialog(row.getItem());
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.invoices.title"),
                I18n.get("entity.invoices.meta", garage.getInvoices().size()),
                I18n.get("entity.invoices.subtitle"),
                t, printBtn, linesBtn, payBtn, addBtn);
    }

    public static VBox buildPaymentsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Payment> table = TableFactory.create(garage.getPayments());
        TableView<Payment> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.invoice"), TableFactory.W_REF, c -> String.valueOf(c.getInvoiceId())),
                TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.paymentCustomerName(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.amount"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.sizeCol(I18n.get("table.col.type"), TableFactory.W_TYPE, c -> UiFormatters.paymentTypeWord(c.getPaymentType())),
                TableFactory.sizeCol(I18n.get("table.col.datetime"), TableFactory.W_DATETIME, c -> UiFormatters.formatDateTime(c.getPaymentDate())),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> c.isSuccessful() ? I18n.get("status.successful") : I18n.get("status.failed")));
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
