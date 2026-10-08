package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.ui.components.BookingFormPane;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

/** The dialogs for creating, changing, cancelling and deleting a booking. */
public final class BookingDialogs {

    private BookingDialogs() {}

    public static void showCreateBookingDialog(GarageSystem garage, Runnable onSuccess) {
        showCreateBookingDialog(garage, null, null, null, onSuccess);
    }

    public static void showCreateBookingDialog(GarageSystem garage, LocalDate defaultDate,
                                               Mechanic defaultMechanic, Integer defaultHour, Runnable onSuccess) {
        List<Vehicle> vehicles = garage.getVehicles();
        if (vehicles.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.booking.create.title"));
        dialog.setHeaderText(I18n.get("dialog.booking.create.header"));
        ActionDialogs.styleDialog(dialog);
        dialog.setResizable(true);
        // A slightly bigger window: the form is 820 wide, and the height gets a lower bound so the
        // calendar and the time are not squeezed together.
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setMinWidth(760);
        dialog.getDialogPane().setMinHeight(720);

        BookingFormPane form = new BookingFormPane(garage, null, defaultDate, defaultMechanic, defaultHour);
        form.setOnContentGrown(() -> ActionDialogs.growToFitContent(dialog));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, form.requiredFieldsFilledBinding(0));

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!form.validate(garage, 0)) {
                    return;
                }

                Vehicle v = form.getSelectedVehicle();
                LocalDate date = form.getSelectedDate();
                List<ServiceItem> chosenServices = form.getSelectedServices();
                ServiceItem chosenService = form.getSelectedService();
                Mechanic chosenMech = form.getSelectedMechanic();
                LocalTime startTime = form.getSelectedStartTime();
                String desc = form.getDescription();
                if (desc.isEmpty() && !chosenServices.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (ServiceItem s : chosenServices) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(com.wac.autocore.seed.SeedText.resolve(s.getName()));
                    }
                    desc = sb.toString();
                } else if (desc.isEmpty() && chosenService != null) {
                    desc = com.wac.autocore.seed.SeedText.resolve(chosenService.getName());
                }

                // Create a clean booking in the system (NO work order is created or started automatically)
                Booking b = garage.createBooking(v.getId(), date, desc);
                if (b != null) {
                    b.setStatus("BOOKED");
                    if (!chosenServices.isEmpty()) {
                        b.setServiceItems(chosenServices);
                    } else if (chosenService != null && !b.addServiceItem(chosenService)) {
                        ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                                I18n.get("dialog.booking.services_locked_work_started"));
                        return;
                    }
                    if (chosenMech != null) {
                        b.setMechanicId(chosenMech.getId());
                        b.setMechanicIds(mechanicIdsFrom(form.getSelectedMechanics()));
                    }
                    if (startTime != null) {
                        b.setStartTime(startTime);
                        int estMin = garage.busyMinutes(b.getServiceItems(), form.getSelectedMechanics());
                        b.setEndTime(startTime.plusMinutes(estMin));
                    }

                    if (!saveBookingOrReport(garage, b)) {
                        return;
                    }

                    // Reserve a slot in the schedule if a mechanic was picked (workOrderId = 0)
                    if (chosenMech != null) {
                        int hour = startTime != null ? startTime.getHour() : (defaultHour != null ? defaultHour : 8);
                        String custName = EntityLookup.customerName(garage, v.getCustomerId());
                        MechanicSchedule.getInstance().bookSlot(
                                chosenMech.getId(), date, hour, b.getId(), 0,
                                custName, v.getRegistrationNumber(), desc
                        );
                    }
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showCopyBookingDialog(GarageSystem garage, Booking source, Runnable onSuccess) {

        List<Vehicle> vehicles = garage.getVehicles();

        if (vehicles.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();

        dialog.setTitle(I18n.get("dialog.copy.booking.title"));
        dialog.setHeaderText(I18n.get("dialog.copy.booking.header"));
        ActionDialogs.styleDialog(dialog);
        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setMinWidth(760);
        dialog.getDialogPane().setMinHeight(720);

        // The date is a new value; the requirement says it must not be inherited from the old booking.
        BookingFormPane form = new BookingFormPane(garage, source, LocalDate.now(), null, null, false, true);
        form.setOnContentGrown(() -> ActionDialogs.growToFitContent(dialog));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, form.requiredFieldsFilledBinding(0));
        dialog.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {

                if (!form.validate(garage, 0)) {
                    return;
                }

                Vehicle v = form.getSelectedVehicle();
                LocalDate date = form.getSelectedDate();
                List<ServiceItem> chosenServices = form.getSelectedServices();
                ServiceItem chosenService = form.getSelectedService();
                Mechanic chosenMech = form.getSelectedMechanic();
                LocalTime startTime = form.getSelectedStartTime();
                String desc = form.getDescription();

                if (desc.isEmpty() && !chosenServices.isEmpty()) {

                    StringBuilder sb = new StringBuilder();

                    for (ServiceItem s : chosenServices) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(com.wac.autocore.seed.SeedText.resolve(s.getName()));
                    }
                    desc = sb.toString();
                } else if (desc.isEmpty() && chosenService != null) {
                    desc = com.wac.autocore.seed.SeedText.resolve(chosenService.getName());
                }

                // Create a clean booking in the system (NO work order is created or started automatically)
                Booking b = garage.createBooking(v.getId(), date, desc);
                if (b != null) {
                    b.setStatus("BOOKED");
                    if (!chosenServices.isEmpty()) {
                        b.setServiceItems(chosenServices);
                    } else if (chosenService != null && !b.addServiceItem(chosenService)) {
                        ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                                I18n.get("dialog.booking.services_locked_work_started"));
                        return;
                    }

                    b.setMechanicId(chosenMech != null ? chosenMech.getId() : 0);
                    b.setMechanicIds(mechanicIdsFrom(form.getSelectedMechanics()));

                    if (startTime != null) {
                        b.setStartTime(startTime);
                        int estMin = garage.busyMinutes(b.getServiceItems(), form.getSelectedMechanics());
                        b.setEndTime(startTime.plusMinutes(estMin));
                    }

                    if (!saveBookingOrReport(garage, b)) {
                        return;
                    }

                    // Reserve a slot in the schedule if a mechanic was picked (workOrderId = 0)
                    if (chosenMech != null) {

                        int hour = startTime != null ? startTime.getHour() : 8;

                        String custName = EntityLookup.customerName(garage, v.getCustomerId());

                        MechanicSchedule.getInstance().bookSlot(
                                chosenMech.getId(), date, hour, b.getId(), 0,
                                custName, v.getRegistrationNumber(), desc
                        );
                    }
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditBookingDialog(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.booking.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.booking.edit.header"));
        ActionDialogs.styleDialog(dialog);
        dialog.setResizable(true);
        // A slightly bigger window: the form is 820 wide, and the height gets a lower bound so the
        // calendar and the time are not squeezed together.
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setMinWidth(760);
        dialog.getDialogPane().setMinHeight(720);

        BookingFormPane form = new BookingFormPane(garage, booking, booking.getDate(), null, null);
        form.setOnContentGrown(() -> ActionDialogs.growToFitContent(dialog));

        // The button sits on a row of its own above the form, inside the styled area.
        HBox actionRow = new HBox();
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        Button copyButton = new Button(I18n.get("dialog.copy.booking.action"));
        copyButton.setOnAction(e -> {
            dialog.close();
            Platform.runLater(() -> showCopyBookingDialog(garage, booking, onSuccess));
        });
        actionRow.getChildren().add(copyButton);

        VBox content = new VBox(10);
        VBox.setVgrow(form, Priority.ALWAYS);
        content.getChildren().addAll(actionRow, form);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, form.requiredFieldsFilledBinding(booking.getId()));

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!form.validate(garage, booking.getId())) {
                    return;
                }

                Vehicle v = form.getSelectedVehicle();
                LocalDate date = form.getSelectedDate();
                List<ServiceItem> chosenServices = form.getSelectedServices();
                ServiceItem chosenService = form.getSelectedService();
                Mechanic chosenMech = form.getSelectedMechanic();
                LocalTime startTime = form.getSelectedStartTime();
                String desc = form.getDescription();
                String status = form.getStatus();

                // Clear any earlier schedule slot for this booking
                MechanicSchedule.getInstance().cancelSlotForBooking(booking.getId());

                booking.setVehicleId(v.getId());
                booking.setDate(date);
                booking.setDescription(desc);
                if (!chosenServices.isEmpty()) {
                    booking.setServiceItems(chosenServices);
                } else if (chosenService != null && !booking.addServiceItem(chosenService)) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.booking.services_locked_work_started"));
                    return;
                } else if (chosenServices.isEmpty() && chosenService == null) {
                    booking.setServiceItems(java.util.Collections.emptyList());
                }

                // The status is set last, because a change back to Booked unlocks the services
                // again. Once the job has started it cannot go back.
                if (booking.isWorkStarted() && !statusTillaten(status)
                        && !status.equalsIgnoreCase(booking.getStatus())) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.booking.status_locked_work_started"));
                    return;
                }
                booking.setStatus(status);
                booking.setMechanicId(chosenMech != null ? chosenMech.getId() : 0);
                booking.setMechanicIds(mechanicIdsFrom(form.getSelectedMechanics()));
                if (startTime != null) {
                    booking.setStartTime(startTime);
                    int estMin = garage.busyMinutes(booking.getServiceItems(), form.getSelectedMechanics());
                    booking.setEndTime(startTime.plusMinutes(estMin));
                }

                if (!saveBookingOrReport(garage, booking)) {
                    return;
                }

                // Rebook in the schedule if a mechanic is assigned and the booking is not cancelled
                if (chosenMech != null && !"CANCELLED".equalsIgnoreCase(status)) {
                    int hour = startTime != null ? startTime.getHour() : 8;
                    String custName = EntityLookup.customerName(garage, v.getCustomerId());
                    MechanicSchedule.getInstance().bookSlot(
                            chosenMech.getId(), date, hour, booking.getId(), 0,
                            custName, v.getRegistrationNumber(), desc
                    );
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showCancelBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        if (!garage.canCancelOrDeleteBooking(booking.getId())) {
            ActionDialogs.showError(I18n.get("dialog.booking.cancel.title"),
                    I18n.get("dialog.booking.cancel.has_active_orders", booking.getId()));
            return;
        }

        Alert confirm = ActionDialogs.confirm(I18n.get("dialog.booking.cancel.title"),
                I18n.get("dialog.booking.cancel.header"),
                I18n.get("dialog.booking.cancel.confirm", booking.getId()));

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    garage.cancelBooking(booking.getId());
                } catch (Exception e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteBookingConfirmation(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        if (!garage.canCancelOrDeleteBooking(booking.getId())) {
            ActionDialogs.showError(I18n.get("dialog.booking.delete.title"),
                    I18n.get("dialog.booking.delete.has_active_orders", booking.getId()));
            return;
        }

        Alert confirm = ActionDialogs.confirm(I18n.get("dialog.booking.delete.title"),
                I18n.get("dialog.booking.delete.header"),
                I18n.get("dialog.booking.delete.confirm", booking.getId()));

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                try {
                    garage.deleteBooking(booking.getId());
                } catch (Exception e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    /** Saves the booking and shows the error instead of swallowing it. */
    private static boolean saveBookingOrReport(GarageSystem garage, Booking booking) {
        try {
            garage.updateBooking(booking);
            return true;
        } catch (Exception e) {
            String reason = e.getMessage();
            ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                    I18n.get("dialog.booking.save_failed")
                            + (reason == null || reason.trim().isEmpty() ? "" : " (" + reason + ")"));
            return false;
        }
    }

    /** The statuses that may be set once the work has already started. */
    private static boolean statusTillaten(String status) {
        return "IN_PROGRESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status);
    }

    /** The ids of the mechanics picked in the form, in the order they were picked. */
    private static List<Integer> mechanicIdsFrom(List<Mechanic> chosen) {
        List<Integer> ids = new ArrayList<Integer>();
        if (chosen != null) {
            for (Mechanic m : chosen) {
                if (m != null && m.getId() > 0) {
                    Integer id = Integer.valueOf(m.getId());
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }
}
