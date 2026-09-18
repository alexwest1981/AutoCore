package com.wac.autocore.ui.views;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard-vy med KPI-kort, statusfördelning, kommande bokningar och senaste arbetsordrar.
 */
public final class OverviewView {

    private OverviewView() {}

    public static VBox build(GarageSystem garage, Runnable onRefresh) {
        return build(garage, onRefresh, null);
    }

    public static VBox build(GarageSystem garage, Runnable onRefresh, com.wac.autocore.ui.navigation.PageRouter router) {
        List<Booking> bookings = garage.getBookings();
        List<WorkOrder> workOrders = garage.getWorkOrders();
        List<Payment> payments = garage.getPayments();

        long active = 0;
        for (WorkOrder wo : workOrders) {
            if (!"COMPLETED".equals(wo.getStatus())) {
                active++;
            }
        }
        long revenue = 0;
        for (Payment p : payments) {
            if (p.isSuccessful()) {
                revenue += p.getAmount();
            }
        }
        int avail = 0;
        for (Mechanic m : garage.getMechanics()) {
            if (m.isAvailable()) {
                avail++;
            }
        }

        VBox head = UiComponents.pageHead("Overview", "Current status of the workshop",
                "AutoCore \u00b7 " + UiFormatters.todayFormatted());

        Button quickBooking = UiComponents.primaryButton("+ New booking");
        quickBooking.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, onRefresh));

        Button quickOrder = UiComponents.secondaryButton("+ New work order");
        quickOrder.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, onRefresh));

        Button quickInvoice = UiComponents.secondaryButton("+ Create invoice");
        quickInvoice.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, onRefresh));

        Button quickPay = UiComponents.secondaryButton("💳 Payment");
        quickPay.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, onRefresh));

        HBox quickBar = new HBox(10, quickBooking, quickOrder, quickInvoice, quickPay);
        quickBar.setAlignment(Pos.CENTER_LEFT);

        HBox kpis = new HBox(14);
        kpis.setAlignment(Pos.CENTER_LEFT);
        kpis.getChildren().addAll(
                UiComponents.kpi("Active work orders", String.valueOf(active)),
                UiComponents.kpi("Total revenue", UiFormatters.formatMoney(revenue)),
                UiComponents.kpi("Bookings", String.valueOf(bookings.size())),
                UiComponents.kpi("Mechanics on duty", avail + "/" + garage.getMechanics().size()));

        HBox panels = new HBox(14);
        panels.setAlignment(Pos.CENTER_LEFT);
        panels.getChildren().addAll(
                statusPanel(workOrders),
                bookingsPanel(garage, bookings));

        TableView<WorkOrder> recent = buildRecentOrdersTable(garage, workOrders, router);
        VBox recentPanel = UiComponents.panel("Recent work orders",
                "The latest jobs registered in the system", recent);

        return new VBox(18, head, quickBar, kpis, panels, recentPanel);
    }

    private static VBox statusPanel(List<WorkOrder> workOrders) {
        Label title = new Label("Work orders by status");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Distribution across all work orders");
        sub.getStyleClass().add("panel-sub");

        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        counts.put("Completed", 0);
        counts.put("In progress", 0);
        counts.put("Work order created", 0);
        counts.put("Created", 0);
        counts.put("Booked", 0);
        for (WorkOrder wo : workOrders) {
            String w = UiFormatters.statusWord(wo.getStatus());
            counts.put(w, counts.containsKey(w) ? counts.get(w) + 1 : 1);
        }

        VBox list = new VBox(9);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() == 0) {
                continue;
            }
            Label dot = new Label();
            dot.getStyleClass().addAll("sdot", UiFormatters.dotClass(e.getKey()));
            Label name = new Label(e.getKey());
            name.getStyleClass().add("srow-title");
            Label n = new Label(String.valueOf(e.getValue()));
            n.getStyleClass().add("srow-sub");
            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);
            HBox row = new HBox(10, dot, name, spr, n);
            row.getStyleClass().add("srow");
            list.getChildren().add(row);
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(UiComponents.mutedNote("No work orders yet"));
        }

        VBox box = new VBox(12, title, sub, list);
        box.getStyleClass().add("panel");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static VBox bookingsPanel(GarageSystem garage, List<Booking> bookings) {
        Label title = new Label("Upcoming bookings");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Next scheduled jobs");
        sub.getStyleClass().add("panel-sub");

        VBox list = new VBox(9);
        int shown = 0;
        for (Booking b : bookings) {
            if (shown >= 5) {
                break;
            }
            Label dot = new Label();
            dot.getStyleClass().addAll("sdot", UiFormatters.dotClass(b.getStatus()));
            Label vehicle = new Label(EntityLookup.vehicleReg(garage, b.getVehicleId()) + " · " + b.getDate());
            vehicle.getStyleClass().add("srow-title");
            Label desc = new Label(UiFormatters.truncate(b.getDescription(), 42));
            desc.getStyleClass().addAll("srow-sub", "small");
            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);
            Label st = new Label(UiFormatters.statusWord(b.getStatus()));
            st.getStyleClass().addAll("badge", UiFormatters.badgeClass(st.getText()));
            VBox text = new VBox(0, vehicle, desc);
            HBox row = new HBox(10, dot, text, spr, st);
            row.getStyleClass().add("srow");
            list.getChildren().add(row);
            shown++;
        }
        if (list.getChildren().isEmpty()) {
            list.getChildren().add(UiComponents.mutedNote("No bookings yet"));
        }

        VBox box = new VBox(12, title, sub, list);
        box.getStyleClass().add("panel");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    @SuppressWarnings("unchecked")
    private static TableView<WorkOrder> buildRecentOrdersTable(GarageSystem garage, List<WorkOrder> orders, com.wac.autocore.ui.navigation.PageRouter router) {
        TableFactory.FilterableTable<WorkOrder> table = TableFactory.create(orders);
        TableView<WorkOrder> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.col("ID", 70, c -> String.valueOf(c.getId())),
                TableFactory.col("Booking", 90, c -> String.valueOf(c.getBookingId())),
                TableFactory.col("Mechanic", 180, c -> EntityLookup.mechanicName(garage, c.getMechanicId())),
                TableFactory.col("Services", 300, c -> EntityLookup.serviceNames(garage, c.getServiceItemIds())),
                TableFactory.badgeCol("Status", 140, c -> UiFormatters.statusWord(c.getStatus())));
        if (router != null) {
            router.setActiveTable(table);
        }
        return t;
    }
}
