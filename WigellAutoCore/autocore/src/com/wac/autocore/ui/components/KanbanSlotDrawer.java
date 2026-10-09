package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.WorkOrderDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The drawer that opens under a slot in the kanban card, with the booking behind it and the
 * button that opens or creates its work order.
 */
class KanbanSlotDrawer {

    private final MechanicKanbanCard card;

    KanbanSlotDrawer(MechanicKanbanCard card) {
        this.card = card;
    }

    VBox build(Mechanic mech, TimeSlot slot) {
        WorkOrder order = findWorkOrder(slot);
        Booking booking = findBooking(slot, order);

        VBox drawer = new VBox(10);
        drawer.getStyleClass().add("kanban-inline-drawer");
        drawer.getChildren().addAll(
                buildHead(slot, order),
                divider(),
                buildFields(slot, booking),
                divider(),
                buildActionRow(slot, order, booking));
        return drawer;
    }

    /** The work order the slot points at, directly or through its booking. */
    private WorkOrder findWorkOrder(TimeSlot slot) {

        if (slot.getWorkOrderId() > 0) {

            for (WorkOrder wo : card.garage.getWorkOrders()) {

                if (wo.getId() == slot.getWorkOrderId()) {
                    return wo;
                }
            }
        }
        if (slot.getBookingId() > 0) {

            for (WorkOrder wo : card.garage.getWorkOrders()) {

                if (wo.getBookingId() == slot.getBookingId()) {
                    slot.setWorkOrderId(wo.getId());
                    return wo;
                }
            }
        }
        return null;
    }

    /** The booking behind the slot, first through the work order and then by the slot itself. */
    private Booking findBooking(TimeSlot slot, WorkOrder order) {

        if (order != null) {
            Booking fromOrder = findBookingById(order.getBookingId());
            if (fromOrder != null) {
                return fromOrder;
            }
        }

        if (slot.getBookingId() > 0) {
            return findBookingById(slot.getBookingId());
        }

        return null;
    }

    private Booking findBookingById(int bookingId) {
        for (Booking bk : card.garage.getBookings()) {
            if (bk.getId() == bookingId) {
                return bk;
            }
        }
        return null;
    }

    /** Time on the left, reference and the close button on the right. */
    private HBox buildHead(TimeSlot slot, WorkOrder order) {
        Label timeLabel = new Label(slot.getTimeRange());
        timeLabel.getStyleClass().add("kanban-drawer-time");

        String title = order != null
                ? I18n.get("kanban.drawer.work_order", order.getId())
                : I18n.get("kanban.drawer.booked");
        Label refLabel = new Label(title);
        refLabel.getStyleClass().add("kanban-drawer-ref");

        Button closeButton = new Button("×");
        closeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: -wac-muted;"
                + " -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 2 6 2 6;");
        closeButton.setOnAction(e -> {
            card.expandedSlot = null;
            card.renderBody();
        });

        HBox head = new HBox(8, timeLabel, spacer(), refLabel, closeButton);
        head.setAlignment(Pos.CENTER_LEFT);
        return head;
    }

    /** The details as label and value, so customer, vehicle and work line up in one column. */
    private GridPane buildFields(TimeSlot slot, Booking booking) {
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(84);
        labelColumn.setPrefWidth(84);

        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(6);
        fields.getColumnConstraints().addAll(labelColumn, valueColumn);

        String[][] rows = {
                {I18n.get("table.col.customer"), customerText(slot)},
                {I18n.get("table.col.vehicle"), vehicleText(slot, booking)},
                {I18n.get("table.col.description"), descriptionText(slot, booking)}};

        for (int i = 0; i < rows.length; i++) {
            Label name = new Label(rows[i][0]);
            name.getStyleClass().add("kanban-drawer-field");
            name.setMinWidth(Region.USE_PREF_SIZE);

            Label value = new Label(rows[i][1]);
            value.getStyleClass().add("kanban-drawer-value");
            value.setWrapText(true);
            value.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(value, Priority.ALWAYS);

            fields.add(name, 0, i);
            fields.add(value, 1, i);
        }
        return fields;
    }

    private String customerText(TimeSlot slot) {
        return textOrDash(slot.getCustomerName());
    }

    private String vehicleText(TimeSlot slot, Booking booking) {

        if (hasText(slot.getVehicleReg())) {
            return slot.getVehicleReg();
        }
        return booking != null ? EntityLookup.vehicleReg(card.garage, booking.getVehicleId()) : "-";
    }

    private String descriptionText(TimeSlot slot, Booking booking) {
        if (hasText(slot.getDescription())) {
            return SeedText.resolve(slot.getDescription());
        }
        return booking != null ? SeedText.resolve(booking.getDescription()) : "-";
    }

    private static boolean hasText(String s) {
        return s != null && !s.isEmpty();
    }

    private static String textOrDash(String s) {
        return hasText(s) ? s : "-";
    }

    /** The status and the open button, or the create button when the slot has no work order yet. */
    private HBox buildActionRow(TimeSlot slot, WorkOrder order, Booking booking) {

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(2, 0, 0, 0));

        if (order != null) {

            Label statusBadge = new Label(UiFormatters.statusWord(order.getStatus()));
            statusBadge.getStyleClass().addAll("badge", UiFormatters.badgeClass(statusBadge.getText()), "small");

            Button openButton = new Button(I18n.get("kanban.drawer.open_order"));
            openButton.getStyleClass().addAll("primary", "small");
            openButton.setMinWidth(Region.USE_PREF_SIZE);
            openButton.setOnAction(e -> navigateToOrder(order));

            actionRow.getChildren().addAll(statusBadge, spacer(), openButton);
        } else {
            Button createButton = new Button(I18n.get("kanban.drawer.create_order"));
            createButton.getStyleClass().addAll("primary", "small");
            createButton.setMinWidth(Region.USE_PREF_SIZE);
            createButton.setOnAction(e -> createOrderForSlot(slot, booking));

            actionRow.getChildren().addAll(spacer(), createButton);
        }
        return actionRow;
    }

    /**
     * Creates the work orders the slot needs and opens the one that belongs to this mechanic.
     */
    private void createOrderForSlot(TimeSlot slot, Booking booking) {

        Booking target = booking;

        if (target == null) {

            int vehicleId = vehicleIdForSlot(slot);

            if (vehicleId == 0) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("kanban.drawer.no_vehicle"));
                return;
            }

            target = card.garage.createBooking(vehicleId, slot.getDate(), slot.getDescription());
            slot.setBookingId(target.getId());
        }

        if (target.getServiceItemIds().isEmpty()) {
            giveFirstService(target);
        }

        WorkOrder created = createPlanOrders(slot, target);

        if (created == null) {
            created = card.garage.createWorkOrder(target.getId(), slot.getMechanicId());
        }

        if (created != null) {

            slot.setWorkOrderId(created.getId());

            if (card.onRefresh != null) {
                card.onRefresh.run();
            }
            navigateToOrder(created);
        }
    }

    /** The vehicle on the slot, or zero when the registration matches none. */
    private int vehicleIdForSlot(TimeSlot slot) {

        for (Vehicle v : card.garage.getVehicles()) {

            if (v.getRegistrationNumber().equalsIgnoreCase(slot.getVehicleReg())) {
                return v.getId();
            }
        }

        return 0;
    }

    /** A booking with no services would leave the work order with nothing to do. */
    private void giveFirstService(Booking booking) {
        try {
            booking.addServiceItem(card.garage.getServiceItems().get(0));
            card.garage.updateBooking(booking);
        } catch (Exception ex) {
            System.out.println("Could not give the booking a service: " + ex.getMessage());
        }
    }

    /**
     * One work order per mechanic, the same plan the work order dialog uses, so a booking with
     * several specialists gets one order each. Returns the one meant for this slot's mechanic.
     */
    private WorkOrder createPlanOrders(TimeSlot slot, Booking booking) {
        LinkedHashMap<Integer, List<ServiceItem>> plan = WorkOrderDialogs.planWorkOrders(card.garage, booking);
        WorkOrder forThisMechanic = null;

        for (Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {

            List<Integer> ids = new ArrayList<Integer>();

            for (ServiceItem service : entry.getValue()) {
                ids.add(Integer.valueOf(service.getId()));
            }

            WorkOrder created = card.garage.createWorkOrder(booking.getId(), entry.getKey().intValue(), ids);

            if (created != null && entry.getKey().intValue() == slot.getMechanicId()) {
                forThisMechanic = created;
            }
        }

        return forThisMechanic;
    }

    private void navigateToOrder(WorkOrder order) {
        if (card.router != null) {
            card.router.navigateToWorkOrder(order.getId());
        }
    }

    private static Region divider() {
        Region line = new Region();
        line.getStyleClass().add("kanban-drawer-line");
        return line;
    }

    private static Region spacer() {
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        return space;
    }
}
