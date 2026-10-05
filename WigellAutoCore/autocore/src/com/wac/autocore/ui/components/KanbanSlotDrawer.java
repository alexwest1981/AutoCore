package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.TimeSlot;
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
import com.wac.autocore.seed.SeedText;

/** En del av mekanikerkortet. */
class KanbanSlotDrawer {

    private final MechanicKanbanCard card;

    KanbanSlotDrawer(MechanicKanbanCard card) {
        this.card = card;
    }

        VBox build(Mechanic mech, TimeSlot slot) {
        WorkOrder targetOrder = null;
        if (slot.getWorkOrderId() > 0) {
            for (WorkOrder wo : card.garage.getWorkOrders()) {
                if (wo.getId() == slot.getWorkOrderId()) {
                    targetOrder = wo;
                    break;
                }
            }
        }
        if (targetOrder == null && slot.getBookingId() > 0) {
            for (WorkOrder wo : card.garage.getWorkOrders()) {
                if (wo.getBookingId() == slot.getBookingId()) {
                    targetOrder = wo;
                    slot.setWorkOrderId(wo.getId());
                    break;
                }
            }
        }

        Booking booking = null;
        if (targetOrder != null) {
            for (Booking bk : card.garage.getBookings()) {
                if (bk.getId() == targetOrder.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }
        if (booking == null && slot.getBookingId() > 0) {
            for (Booking bk : card.garage.getBookings()) {
                if (bk.getId() == slot.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }

        final WorkOrder wo = targetOrder;
        final Booking b = booking;

        VBox drawer = new VBox(10);
        drawer.getStyleClass().add("kanban-inline-drawer");

        // Rubrikrad: tid till vänster, hänvisning och stängknapp till höger.
        String title = (wo != null)
                ? I18n.get("kanban.drawer.work_order", wo.getId())
                : I18n.get("kanban.drawer.booked");

        Label timeLbl = new Label(slot.getTimeRange());
        timeLbl.getStyleClass().add("kanban-drawer-time");

        Label refLbl = new Label(title);
        refLbl.getStyleClass().add("kanban-drawer-ref");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -wac-muted; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 2 6 2 6;");
        closeBtn.setOnAction(e -> {
            card.expandedSlot = null;
            card.renderBody();
        });

        HBox head = new HBox(8, timeLbl, spr, refLbl, closeBtn);
        head.setAlignment(Pos.CENTER_LEFT);

        // Detaljerna som etikett och värde, så att kund, fordon och arbete linjerar i en kolumn.
        String reg = slot.getVehicleReg() != null && !slot.getVehicleReg().isEmpty() ? slot.getVehicleReg() : (b != null ? EntityLookup.vehicleReg(card.garage, b.getVehicleId()) : "-");
        String cust = slot.getCustomerName() != null && !slot.getCustomerName().isEmpty() ? slot.getCustomerName() : "-";
        String desc = slot.getDescription() != null && !slot.getDescription().isEmpty() ? SeedText.resolve(slot.getDescription()) : (b != null ? SeedText.resolve(b.getDescription()) : "-");

        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(6);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(84);
        labelColumn.setPrefWidth(84);
        ColumnConstraints värdeKolumn = new ColumnConstraints();
        värdeKolumn.setHgrow(Priority.ALWAYS);
        fields.getColumnConstraints().addAll(labelColumn, värdeKolumn);

        String[][] rows = {
                {I18n.get("table.col.customer"), cust},
                {I18n.get("table.col.vehicle"), reg},
                {I18n.get("table.col.description"), desc}};
        for (int i = 0; i < rows.length; i++) {
            Label label = new Label(rows[i][0]);
            label.getStyleClass().add("kanban-drawer-field");
            label.setMinWidth(Region.USE_PREF_SIZE);

            Label värde = new Label(rows[i][1]);
            värde.getStyleClass().add("kanban-drawer-value");
            värde.setWrapText(true);
            värde.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(värde, Priority.ALWAYS);

            fields.add(label, 0, i);
            fields.add(värde, 1, i);
        }

        Region line1 = new Region();
        line1.getStyleClass().add("kanban-drawer-line");
        Region line2 = new Region();
        line2.getStyleClass().add("kanban-drawer-line");

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(2, 0, 0, 0));

        if (wo != null) {
            Label stBadge = new Label(UiFormatters.statusWord(wo.getStatus()));
            stBadge.getStyleClass().addAll("badge", UiFormatters.badgeClass(stBadge.getText()), "small");

            Region spr2 = new Region();
            HBox.setHgrow(spr2, Priority.ALWAYS);

            Button openBtn = new Button(I18n.get("kanban.drawer.open_order"));
            openBtn.getStyleClass().addAll("primary", "small");
            openBtn.setMinWidth(Region.USE_PREF_SIZE);
            openBtn.setOnAction(e -> {
                if (card.router != null) {
                    card.router.navigateToWorkOrder(wo.getId());
                }
            });

            actionRow.getChildren().addAll(stBadge, spr2, openBtn);
        } else {
            Region spr2 = new Region();
            HBox.setHgrow(spr2, Priority.ALWAYS);

            Button createBtn = new Button(I18n.get("kanban.drawer.create_order"));
            createBtn.getStyleClass().addAll("primary", "small");
            createBtn.setMinWidth(Region.USE_PREF_SIZE);
            createBtn.setOnAction(e -> {
                Booking targetBooking = b;
                if (targetBooking == null) {
                    int vehicleId = 1;
                    for (Vehicle v : card.garage.getVehicles()) {
                        if (v.getRegistrationNumber().equalsIgnoreCase(slot.getVehicleReg())) {
                            vehicleId = v.getId();
                            break;
                        }
                    }
                        targetBooking = card.garage.createBooking(vehicleId, slot.getDate(), slot.getDescription());

                    slot.setBookingId(targetBooking.getId());
                }
                if (targetBooking != null && targetBooking.getServiceItemIds().isEmpty()) {
                    // Bokningen saknar tjänster. Ge den en, så arbetsordern har något att utföra ().
                    try {
                        targetBooking.addServiceItem(card.garage.getServiceItems().get(0));
                        card.garage.updateBooking(targetBooking);
                    } catch (Exception ex) {
                        System.out.println("Could not give the booking a service: " + ex.getMessage());
                    }
                }
                // En arbetsorder per mekaniker, samma plan som arbetsorderdialogen använder,
                // så att en bokning med flera specialister får en order var även härifrån.
                java.util.LinkedHashMap<Integer, java.util.List<com.wac.autocore.model.ServiceItem>> plan =
                        com.wac.autocore.ui.WorkOrderDialogs.planWorkOrders(card.garage, targetBooking);
                WorkOrder createdWo = null;
                for (java.util.Map.Entry<Integer, java.util.List<com.wac.autocore.model.ServiceItem>> entry
                        : plan.entrySet()) {
                    java.util.List<Integer> ids = new java.util.ArrayList<Integer>();
                    for (com.wac.autocore.model.ServiceItem s : entry.getValue()) {
                        ids.add(Integer.valueOf(s.getId()));
                    }
                    WorkOrder newOrder = card.garage.createWorkOrder(targetBooking.getId(),
                            entry.getKey().intValue(), ids);
                    if (newOrder != null && entry.getKey().intValue() == slot.getMechanicId()) {
                        createdWo = newOrder;
                    }
                }
                if (createdWo == null) {
                    // Schemats mekaniker fick ingen egen post i planen. Då skapas ordern åt hen direkt.
                    createdWo = card.garage.createWorkOrder(targetBooking.getId(), slot.getMechanicId());
                }
                if (createdWo != null) {
                    slot.setWorkOrderId(createdWo.getId());
                    if (card.onRefresh != null) card.onRefresh.run();
                    if (card.router != null) {
                        card.router.navigateToWorkOrder(createdWo.getId());
                    }
                }
            });

            actionRow.getChildren().addAll(spr2, createBtn);
        }

        drawer.getChildren().addAll(head, line1, fields, line2, actionRow);
        return drawer;
    }


    // =========================================================================
    // 3. KOMPAKT MÅNADSVY (Kalenderraster)
    // =========================================================================
}
