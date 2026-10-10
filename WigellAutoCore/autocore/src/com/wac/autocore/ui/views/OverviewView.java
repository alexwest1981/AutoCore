package com.wac.autocore.ui.views;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
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

import java.util.List;

/** The overview: figures, status breakdown and the most recent records. */
public final class OverviewView {

    private OverviewView() {}

    public static VBox build(GarageSystem garage, Runnable onRefresh, com.wac.autocore.ui.navigation.PageRouter router) {
        List<Booking> bookings = garage.getBookings();
        List<WorkOrder> workOrders = garage.getWorkOrders();

        VBox head = UiComponents.pageHead(I18n.get("overview.title"), I18n.get("overview.meta"),
                "AutoCore \u00b7 " + UiFormatters.todayFormatted());

        Button quickBooking = UiComponents.primaryButton(I18n.get("overview.action.booking"));
        quickBooking.setOnAction(e -> ActionDialogs.showCreateBookingDialog(garage, onRefresh));

        Button quickOrder = UiComponents.secondaryButton(I18n.get("overview.action.workorder"));
        quickOrder.setOnAction(e -> ActionDialogs.showCreateWorkOrderDialog(garage, onRefresh));

        Button quickInvoice = UiComponents.secondaryButton(I18n.get("overview.action.invoice"));
        quickInvoice.setOnAction(e -> ActionDialogs.showCreateInvoiceDialog(garage, onRefresh));

        Button quickPay = UiComponents.secondaryButton(I18n.get("overview.action.payment"));
        quickPay.setOnAction(e -> ActionDialogs.showProcessPaymentDialog(garage, null, onRefresh));

        HBox quickBar = new HBox(10, quickBooking, quickOrder, quickInvoice, quickPay);
        quickBar.setAlignment(Pos.CENTER_LEFT);
        quickBar.setMinWidth(Region.USE_PREF_SIZE);

        VBox kanbanBoard = com.wac.autocore.ui.components.KanbanBoard.build(garage, router, onRefresh);

        TableFactory.FilterableTable<WorkOrder> recent = buildRecentOrdersTable(garage, workOrders, router);
        VBox recentPanel = UiComponents.panel(I18n.get("overview.section.recent_workorders"),
                I18n.get("overview.section.recent_workorders_sub"),
                new VBox(0, recent.getTableView(), TableFactory.buildPager(recent)));

        // The bookings get a fixed width and the latest list takes the rest of the space.
        VBox bookingsCard = bookingsPanel(garage, bookings);
        bookingsCard.setMinWidth(420);
        bookingsCard.setPrefWidth(460);
        bookingsCard.setMaxWidth(460);
        HBox.setHgrow(recentPanel, Priority.ALWAYS);
        HBox bottomRow = new HBox(18, bookingsCard, recentPanel);
        bottomRow.setAlignment(Pos.TOP_LEFT);

        return new VBox(18, head, quickBar, kanbanBoard, bottomRow);
    }

    private static VBox bookingsPanel(GarageSystem garage, List<Booking> bookings) {
        Label title = new Label(I18n.get("overview.section.upcoming_bookings"));
        title.getStyleClass().add("panel-title");
        Label sub = new Label(I18n.get("overview.section.upcoming_bookings_sub"));
        sub.getStyleClass().add("panel-sub");

        VBox list = new VBox(9);
        int shown = 0;
        for (Booking b : bookings) {
            if (shown >= 5) {
                break;
            }
            Label dot = new Label();
            dot.getStyleClass().addAll("sdot", UiFormatters.dotClass(b.getStatus()));
            Label vehicle = new Label(EntityLookup.vehicleReg(garage, b.getVehicleId()) + " \u00b7 " + b.getDate());
            vehicle.getStyleClass().add("srow-title");
            Label desc = new Label(UiFormatters.truncate(SeedText.resolve(b.getDescription()), 42));
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
            list.getChildren().add(UiComponents.mutedNote(I18n.get("overview.empty.bookings")));
        }

        VBox box = new VBox(12, title, sub, list);
        box.getStyleClass().add("panel");
        box.setMinWidth(280);
        box.setPrefWidth(450);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    @SuppressWarnings("unchecked")
    private static TableFactory.FilterableTable<WorkOrder> buildRecentOrdersTable(GarageSystem garage, List<WorkOrder> orders, com.wac.autocore.ui.navigation.PageRouter router) {
        TableFactory.FilterableTable<WorkOrder> table = TableFactory.create(orders);
        TableView<WorkOrder> t = table.getTableView();
        t.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.booking"), TableFactory.W_REF, c -> String.valueOf(c.getBookingId())),
                TableFactory.sizeCol(I18n.get("table.col.date"), TableFactory.W_DATE, c -> EntityLookup.workOrderDate(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_TIME, c -> EntityLookup.workOrderTime(garage, c)),
                TableFactory.textCol(I18n.get("table.col.mechanic"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> EntityLookup.mechanicName(garage, c.getMechanicId())),
                TableFactory.textCol(I18n.get("table.col.services"), TableFactory.W_SERVICES_MIN, TableFactory.W_SERVICES_MAX, c -> EntityLookup.workOrderServices(garage, c)),
                TableFactory.sizeCol(I18n.get("table.col.total"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(EntityLookup.workOrderTotal(garage, c))),
                TableFactory.sizeBadge(I18n.get("table.col.status"), TableFactory.W_STATUS, c -> UiFormatters.statusWord(c.getStatus())));
        t.setRowFactory(tv -> {
            javafx.scene.control.TableRow<WorkOrder> row = new javafx.scene.control.TableRow<WorkOrder>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showWorkOrderDetailsDialog(garage, row.getItem(), () -> router.navigate("overview"));
                }
            });
            return row;
        });
        if (router != null) {
            router.setActiveTable(table);
        }
        // The same height as the list pages, so the panel does not jump between views here either.
        UiComponents.fixTableHeight(t);
        // The overview has to fit without scrolling, so the table gets fewer rows than a full page.
        double overviewHeight = 36 + (10 * 32) + 2;
        t.setPrefHeight(overviewHeight);
        t.setMinHeight(overviewHeight);
        t.setMaxHeight(overviewHeight);
        return table;
    }
}
