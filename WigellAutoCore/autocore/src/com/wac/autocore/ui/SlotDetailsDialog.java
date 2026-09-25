package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import com.wac.autocore.seed.SeedText;

/**
 * Modala dialoger för att inspektera schemalagda tidsluckor och detaljer kring arbetsordrar.
 */
public final class SlotDetailsDialog {

    private SlotDetailsDialog() {}

    public static void showSlotDetailsDialog(GarageSystem garage,
                                             MechanicSchedule.TimeSlot slot,
                                             PageRouter router,
                                             Runnable onRefresh) {
        if (slot == null || !slot.isBooked()) {
            return;
        }

        WorkOrder targetOrder = null;
        if (slot.getWorkOrderId() > 0) {
            for (WorkOrder wo : garage.getWorkOrders()) {
                if (wo.getId() == slot.getWorkOrderId()) {
                    targetOrder = wo;
                    break;
                }
            }
        }
        if (targetOrder == null && slot.getBookingId() > 0) {
            for (WorkOrder wo : garage.getWorkOrders()) {
                if (wo.getBookingId() == slot.getBookingId()) {
                    targetOrder = wo;
                    slot.setWorkOrderId(wo.getId());
                    break;
                }
            }
        }

        Booking booking = null;
        if (targetOrder != null) {
            for (Booking bk : garage.getBookings()) {
                if (bk.getId() == targetOrder.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }
        if (booking == null && slot.getBookingId() > 0) {
            for (Booking bk : garage.getBookings()) {
                if (bk.getId() == slot.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }

        final WorkOrder wo = targetOrder;
        final Booking b = booking;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        boolean hasWorkOrder = (wo != null);
        String title = hasWorkOrder
                ? I18n.get("kanban.drawer.work_order", wo.getId())
                : I18n.get("kanban.drawer.booked");
        dialog.setTitle(title);
        dialog.setHeaderText(I18n.get("dialog.slot.header", slot.getDate().toString() + " (" + slot.getTimeRange() + ")"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();
        int rowIdx = 0;

        grid.add(new Label(I18n.get("dialog.slot.time_date")), 0, rowIdx);
        Label timeLabel = new Label(slot.getDate() + "  |  " + slot.getTimeRange());
        timeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent;");
        grid.add(timeLabel, 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, rowIdx);
        grid.add(new Label(EntityLookup.mechanicName(garage, slot.getMechanicId())), 1, rowIdx++);

        String reg = slot.getVehicleReg() != null && !slot.getVehicleReg().isEmpty() ? slot.getVehicleReg() : "-";
        if (b != null) {
            reg = EntityLookup.vehicleReg(garage, b.getVehicleId());
        }
        grid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, rowIdx);
        grid.add(new Label(reg), 1, rowIdx++);

        String cust = slot.getCustomerName() != null && !slot.getCustomerName().isEmpty() ? slot.getCustomerName() : "-";
        grid.add(new Label(I18n.get("table.col.customer") + ":"), 0, rowIdx);
        grid.add(new Label(cust), 1, rowIdx++);

        String desc = slot.getDescription() != null && !slot.getDescription().isEmpty() ? SeedText.resolve(slot.getDescription()) : "-";
        if (b != null && b.getDescription() != null && !b.getDescription().isEmpty()) {
            desc = SeedText.resolve(b.getDescription());
        }
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        grid.add(new Label(desc), 1, rowIdx++);

        if (hasWorkOrder) {
            grid.add(new Label(I18n.get("table.col.services") + ":"), 0, rowIdx);
            grid.add(new Label(EntityLookup.serviceNames(garage, wo.getServiceItemIds())), 1, rowIdx++);

            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            Label stLabel = new Label(UiFormatters.statusWord(wo.getStatus()));
            stLabel.getStyleClass().addAll("badge", UiFormatters.badgeClass(stLabel.getText()));
            grid.add(stLabel, 1, rowIdx++);
        } else {
            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            Label stLabel = new Label(I18n.get("dialog.slot.booked_status"));
            stLabel.getStyleClass().addAll("badge", "yellow");
            grid.add(stLabel, 1, rowIdx++);
        }

        ButtonType actionBtnType = new ButtonType(
                hasWorkOrder ? I18n.get("dialog.slot.open_order")
                             : I18n.get("dialog.slot.create_order"),
                javafx.scene.control.ButtonBar.ButtonData.OTHER
        );
        ButtonType closeType = ButtonType.CLOSE;

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(actionBtnType, closeType);

        dialog.showAndWait().ifPresent(response -> {
            if (response == actionBtnType && router != null) {
                if (wo != null) {
                    router.navigateToWorkOrder(wo.getId());
                } else {
                    Booking targetBooking = b;
                    if (targetBooking == null) {
                        int vehicleId = 1;
                        for (Vehicle v : garage.getVehicles()) {
                            if (v.getRegistrationNumber().equalsIgnoreCase(slot.getVehicleReg())) {
                                vehicleId = v.getId();
                                break;
                            }
                        }
                        targetBooking = garage.createBooking(vehicleId, slot.getDate(), slot.getDescription());
                        slot.setBookingId(targetBooking.getId());
                    }
                    int sId = targetBooking != null && targetBooking.getServiceItemId() > 0 ? targetBooking.getServiceItemId() : 1;
                    WorkOrder createdWo = garage.createWorkOrder(targetBooking.getId(), slot.getMechanicId(), sId);
                    if (createdWo != null) {
                        slot.setWorkOrderId(createdWo.getId());
                        if (onRefresh != null) onRefresh.run();
                        router.navigateToWorkOrder(createdWo.getId());
                    }
                }
            }
        });
    }

    public static void showWorkOrderDetailsDialog(GarageSystem garage, int workOrderId,
                                                  PageRouter router, Runnable onRefresh) {
        WorkOrder targetOrder = null;
        for (WorkOrder wo : garage.getWorkOrders()) {
            if (wo.getId() == workOrderId) {
                targetOrder = wo;
                break;
            }
        }

        if (targetOrder == null && !garage.getWorkOrders().isEmpty()) {
            targetOrder = garage.getWorkOrders().get(0);
        }

        if (targetOrder == null) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("overview.empty.workorders"));
            return;
        }

        final WorkOrder wo = targetOrder;
        Booking b = null;
        for (Booking bk : garage.getBookings()) {
            if (bk.getId() == wo.getBookingId()) {
                b = bk;
                break;
            }
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("kanban.drawer.work_order", wo.getId()));
        dialog.setHeaderText(I18n.get("dialog.workorder.details_header", wo.getId()));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        int rowIdx = 0;
        grid.add(new Label(I18n.get("table.col.id") + ":"), 0, rowIdx);
        Label idLbl = new Label("#" + wo.getId());
        idLbl.setStyle("-fx-font-weight: bold;");
        grid.add(idLbl, 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.mechanic") + ":"), 0, rowIdx);
        grid.add(new Label(EntityLookup.mechanicName(garage, wo.getMechanicId())), 1, rowIdx++);

        if (b != null) {
            grid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, rowIdx);
            grid.add(new Label(EntityLookup.vehicleReg(garage, b.getVehicleId())), 1, rowIdx++);

            grid.add(new Label(I18n.get("table.col.date") + ":"), 0, rowIdx);
            grid.add(new Label(b.getDate().toString()), 1, rowIdx++);

            grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
            grid.add(new Label(SeedText.resolve(b.getDescription())), 1, rowIdx++);
        }

        grid.add(new Label(I18n.get("table.col.services") + ":"), 0, rowIdx);
        grid.add(new Label(EntityLookup.serviceNames(garage, wo.getServiceItemIds())), 1, rowIdx++);

        grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
        Label stLabel = new Label(UiFormatters.statusWord(wo.getStatus()));
        stLabel.getStyleClass().addAll("badge", UiFormatters.badgeClass(stLabel.getText()));
        grid.add(stLabel, 1, rowIdx++);

        ButtonType gotoType = new ButtonType(I18n.get("dialog.workorder.open_in_orders"), javafx.scene.control.ButtonBar.ButtonData.OTHER);
        ButtonType closeType = ButtonType.CLOSE;

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(gotoType, closeType);

        dialog.showAndWait().ifPresent(response -> {
            if (response == gotoType && router != null) {
                router.navigateToWorkOrder(wo.getId());
            }
        });
    }
}
