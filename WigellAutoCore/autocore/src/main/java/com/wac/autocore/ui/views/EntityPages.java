package com.wac.autocore.ui.views;

import com.wac.autocore.security.Operation;
import com.wac.autocore.model.*;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.Actions;
import com.wac.autocore.ui.motion.HoldToConfirmButton;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import com.wac.autocore.seed.SeedText;

/** The pages of the interface, one class per page. */
@SuppressWarnings("unchecked")
public final class EntityPages {

    private EntityPages() {}

    public static VBox buildCustomersPage(GarageSystem garage, PageRouter router) {
        return CustomerPage.build(garage, router);
    }

    public static VBox buildVehiclesPage(GarageSystem garage, PageRouter router) {
        return VehiclePage.build(garage, router);
    }

    public static VBox buildBookingsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Booking> table = TableFactory.create(garage.getBookings());
        TableView<Booking> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, c -> EntityLookup.vehicleReg(garage, c.getVehicleId())),
                TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, c -> String.valueOf(c.getDate())),
                TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_TIME, c -> EntityLookup.bookingTime(c)),
                TableFactory.textCol(I18n.get("table.col.services"), TableFactory.W_SERVICES_MIN, TableFactory.W_SERVICES_MAX, c -> EntityLookup.bookingServices(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.estimated_time"), TableFactory.W_MINUTES, c -> {
                    int min = EntityLookup.bookingTotalMinutes(garage, c);
                    return min > 0 ? min + " min" : "-";
                }),
                TableFactory.sizeCol(I18n.get("table.col.estimated_cost"), TableFactory.W_MONEY, c -> {
                    int cost = garage.bookingPrice(c);
                    return cost > 0 ? UiFormatters.formatMoney(cost) : "-";
                }),
                TableFactory.textCol(I18n.get("table.col.mechanic"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> PageFormatters.bookingMechanicNames(garage, c)),
                TableFactory.textCol(I18n.get("table.col.description"), TableFactory.W_TEXT_MIN, TableFactory.W_TEXT_MAX, c -> SeedText.resolve(c.getDescription())),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> UiFormatters.statusWord(c.getStatus())));
        router.setActiveTable(table);

        Button addBtn = Actions.button(
                UiComponents.primaryButton(I18n.get("entity.bookings.action_create")), garage, Operation.CREATE_BOOKING);
        addBtn.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, router::requestReload));

        Button cancelBtn = Actions.button(
                UiComponents.secondaryButton(I18n.get("entity.bookings.action_cancel")), garage, Operation.CANCEL_BOOKING);
        Button editBtn = Actions.button(
                UiComponents.secondaryButton(I18n.get("entity.bookings.action_edit")), garage, Operation.UPDATE_BOOKING);
        HoldToConfirmButton deleteBtn = new HoldToConfirmButton(I18n.get("entity.bookings.action_delete"))
                .hint(I18n.get("motion.hold"))
                .confirmText(I18n.get("motion.done"));
        cancelBtn.setDisable(true);
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) -> {
            cancelBtn.setDisable(sel == null || sel.getStatus() == BookingStatus.CANCELLED);
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
        });

        editBtn.setOnAction(e -> {
            Booking sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showEditBookingDialog(garage, sel, router::requestReload);
            }
        });

        cancelBtn.setOnAction(e -> {
            Booking sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showCancelBookingConfirmation(garage, sel, router::requestReload);
            }
        });

        deleteBtn.onConfirm(() -> {
            Booking sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showDeleteBookingConfirmation(garage, sel, router::requestReload);
            }
        });

        t.setRowFactory(tv -> {
            TableRow<Booking> row = new TableRow<Booking>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditBookingDialog(garage, row.getItem(), router::requestReload);
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.bookings.title"),
                PageFormatters.meta("entity.bookings.meta", garage.getBookings().size()),
                I18n.get("entity.bookings.subtitle"),
                table, cancelBtn, deleteBtn, editBtn, addBtn);
    }

    public static VBox buildWorkOrdersPage(GarageSystem garage, PageRouter router) {
        // The rows the page shows, and the count in the heading comes from the same list, so the
        // two never disagree about how much work is in hand.
        java.util.List<WorkOrder> orders = router.workOrdersForList();
        FilterableTable<WorkOrder> table = TableFactory.create(orders);
        TableView<WorkOrder> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.booking"), TableFactory.W_REF, c -> EntityLookup.workOrderBookingRef(c)),
                TableFactory.sizeCol(I18n.get("table.col.type"), TableFactory.W_TYPE, c -> UiFormatters.workOrderTypeWord(c.getType())),
                TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, c -> EntityLookup.workOrderVehicleReg(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, c -> EntityLookup.workOrderDate(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_TIME, c -> EntityLookup.workOrderTime(garage, c)),
                TableFactory.textCol(I18n.get("table.col.mechanic"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.mechanicName(garage, c.getMechanicId())),
                TableFactory.textCol(I18n.get("table.col.services"), TableFactory.W_SERVICES_MIN, TableFactory.W_SERVICES_MAX, c -> EntityLookup.workOrderServices(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.total"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(EntityLookup.workOrderTotal(garage, c))),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> UiFormatters.statusWord(c.getStatus())));
        router.setActiveTable(table);

        Button addBtn = Actions.button(
                UiComponents.primaryButton(I18n.get("entity.workorders.action_create")), garage, Operation.CREATE_WORK_ORDER);
        addBtn.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, router::requestReload));

        Button draftBtn = Actions.button(
                UiComponents.secondaryButton(I18n.get("entity.workorders.action_draft")), garage, Operation.CREATE_WORK_ORDER);
        draftBtn.setOnAction(e -> ActionDialogs.showCreateDraftDialog(garage, router::requestReload));

        // Looking at an order is a reading, so a role that only reads gets this button. What it may
        // do once the order is open is a separate question, answered inside the dialog.
        Button detailsBtn = Actions.button(
                UiComponents.secondaryButton(I18n.get("entity.workorders.action_details")), garage, Operation.READ);
        detailsBtn.setDisable(true);
        detailsBtn.setOnAction(e -> {
            WorkOrder sel = t.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ActionDialogs.showWorkOrderDetailsDialog(garage, sel, router::requestReload);
            }
        });

        t.getSelectionModel().selectedItemProperty().addListener((obs, oldV, sel) ->
                detailsBtn.setDisable(sel == null));

        t.setRowFactory(tv -> {
            TableRow<WorkOrder> row = new TableRow<WorkOrder>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showWorkOrderDetailsDialog(garage, row.getItem(), router::requestReload);
                }
            });
            return row;
        });

        VBox notices = new javafx.scene.layout.VBox(6,
                UiComponents.viewNotice(
                        PageRouter.countBookingsWithoutWorkOrder(garage.getBookings(), garage.getWorkOrders()),
                        I18n.get("view.notice.workorders.one"),
                        I18n.get("view.notice.workorders.many")),
                UiComponents.viewNotice(
                        PageRouter.countOpenReclamations(garage.getWorkOrders()),
                        I18n.get("view.notice.reclamations.one"),
                        I18n.get("view.notice.reclamations.many")));

        // The switch is only worth a row when there is something behind it, or when it is already
        // on and the last cancelled order was started again while it stood open.
        if (router.isShowingCancelledWorkOrders() || hasCancelled(garage)) {
            CheckBox showCancelled = new CheckBox(I18n.get("entity.workorders.show_cancelled"));
            showCancelled.setSelected(router.isShowingCancelledWorkOrders());
            showCancelled.setOnAction(e -> router.setShowCancelledWorkOrders(showCancelled.isSelected()));
            notices.getChildren().add(showCancelled);
        }

        return UiComponents.buildEntityPage(
                I18n.get("entity.workorders.title"),
                PageFormatters.meta("entity.workorders.meta", orders.size()),
                I18n.get("entity.workorders.subtitle"),
                notices,
                table, detailsBtn, addBtn, draftBtn);
    }

    /** True when there is at least one cancelled work order for the list to keep out. */
    private static boolean hasCancelled(GarageSystem garage) {
        for (WorkOrder order : garage.getWorkOrders()) {
            if (order.getStatus() == WorkOrderStatus.CANCELLED) {
                return true;
            }
        }
        return false;
    }

    public static VBox buildServicesPage(GarageSystem garage, PageRouter router) {
        return ServicePage.build(garage, router);
    }

    public static VBox buildMechanicsPage(GarageSystem garage, PageRouter router) {
        return MechanicPage.build(garage, router);
    }

    public static VBox buildInvoicesPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Invoice> table = TableFactory.create(garage.getInvoices());
        TableView<Invoice> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.booking"), TableFactory.W_REF, c -> String.valueOf(EntityLookup.bookingIdForWorkOrder(garage, c.getWorkOrderId()))),
                TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, c -> EntityLookup.invoiceVehicleReg(garage, c)),
                TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.invoiceCustomerName(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, c -> String.valueOf(c.getInvoiceDate())),
                TableFactory.sizeCol(I18n.get("table.col.amount"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.sizeCol(I18n.get("table.col.discount"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getDiscount())),
                TableFactory.sizeCol(I18n.get("table.col.total_incl_vat"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getTotalIncludingVat())),
                TableFactory.sizeBadge(I18n.get("table.col.paid"), TableFactory.W_FLAG, c -> c.isPaid() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addBtn = Actions.button(
                UiComponents.primaryButton(I18n.get("entity.invoices.action_create")), garage, Operation.CREATE_INVOICE);
        addBtn.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, router::requestReload));

        Button payBtn = Actions.button(
                UiComponents.secondaryButton(I18n.get("entity.invoices.action_pay")), garage, Operation.TAKE_PAYMENT);
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
                ActionDialogs.showProcessPaymentDialog(garage, sel, router::requestReload);
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
                PageFormatters.meta("entity.invoices.meta", garage.getInvoices().size()),
                I18n.get("entity.invoices.subtitle"),
                UiComponents.viewNotice(
                        PageRouter.countBookingsReadyForInvoice(garage.getBookings(), garage.getWorkOrders(),
                                garage.getInvoices()),
                        I18n.get("view.notice.invoices.one"),
                        I18n.get("view.notice.invoices.many")),
                table, printBtn, linesBtn, payBtn, addBtn);
    }

    public static VBox buildPaymentsPage(GarageSystem garage, PageRouter router) {
        FilterableTable<Payment> table = TableFactory.create(garage.getPayments());
        TableView<Payment> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.invoice"), TableFactory.W_REF, c -> String.valueOf(c.getInvoiceId())),
                TableFactory.sizeCol(I18n.get("table.col.vehicle"), TableFactory.W_REG_NR, c -> EntityLookup.paymentVehicleReg(garage, c)),
                TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.paymentCustomerName(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.total_incl_vat"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getAmount())),
                TableFactory.sizeCol(I18n.get("table.col.type"), TableFactory.W_TYPE, c -> UiFormatters.paymentTypeWord(c.getPaymentType())),
                TableFactory.sizeCol(I18n.get("table.col.datetime"), TableFactory.W_DATETIME, c -> UiFormatters.formatDateTime(c.getPaymentDate())),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> c.isSuccessful() ? I18n.get("status.successful") : I18n.get("status.failed")));
        router.setActiveTable(table);

        Button addBtn = Actions.button(
                UiComponents.primaryButton(I18n.get("entity.payments.action_create")), garage, Operation.TAKE_PAYMENT);
        addBtn.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, router::requestReload));

        return UiComponents.buildEntityPage(
                I18n.get("entity.payments.title"),
                PageFormatters.meta("entity.payments.meta", garage.getPayments().size()),
                I18n.get("entity.payments.subtitle"),
                UiComponents.viewNotice(
                        PageRouter.countUnpaidInvoices(garage.getInvoices()),
                        I18n.get("view.notice.payments.one"),
                        I18n.get("view.notice.payments.many")),
                table, addBtn);
    }
}
