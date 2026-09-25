package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.ui.components.BookingFormPane;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.BookingAvailability;
import com.wac.autocore.ui.util.EntityLookup;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Modala dialoger för att skapa, redigera, avboka och ta bort bokningar.
 * Tunn koordinator som delegerar formulärvy till {@link BookingFormPane}
 * och tillgänglighetskontroll till {@link BookingAvailability}.
 */
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

        BookingFormPane form = new BookingFormPane(garage, null, defaultDate, defaultMechanic, defaultHour);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!form.validate(garage, 0)) {
                    return;
                }

                Vehicle v = form.getSelectedVehicle();
                LocalDate date = form.getSelectedDate();
                ServiceItem chosenService = form.getSelectedService();
                Mechanic chosenMech = form.getSelectedMechanic();
                LocalTime startTime = form.getSelectedStartTime();
                String desc = form.getDescription();
                if (desc.isEmpty() && chosenService != null) {
                    desc = chosenService.getName();
                }

                // Skapa ren bokning i systemet (INGEN arbetsorder skapas eller startas automatiskt)
                Booking b = garage.createBooking(v.getId(), date, desc);
                if (b != null) {
                    b.setStatus("BOOKED");
                    if (chosenService != null) {
                        b.setServiceItemId(chosenService.getId());
                    }
                    if (chosenMech != null) {
                        b.setMechanicId(chosenMech.getId());
                    }
                    if (startTime != null) {
                        b.setStartTime(startTime);
                        int estMin = chosenService != null ? chosenService.getEstimatedMinutes() : 60;
                        b.setEndTime(startTime.plusMinutes(estMin));
                    }

                    try {
                        garage.updateBooking(b);
                    } catch (Exception ignored) {}

                    // Reservera tid i schemat om mekaniker valts (workOrderId = 0)
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

    public static void showEditBookingDialog(GarageSystem garage, Booking booking, Runnable onSuccess) {
        if (booking == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.booking.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.booking.edit.header"));
        ActionDialogs.styleDialog(dialog);

        BookingFormPane form = new BookingFormPane(garage, booking, booking.getDate(), null, null);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!form.validate(garage, booking.getId())) {
                    return;
                }

                Vehicle v = form.getSelectedVehicle();
                LocalDate date = form.getSelectedDate();
                ServiceItem chosenService = form.getSelectedService();
                Mechanic chosenMech = form.getSelectedMechanic();
                LocalTime startTime = form.getSelectedStartTime();
                String desc = form.getDescription();
                String status = form.getStatus();

                // Rensa eventuell tidigare schemaplats för denna bokning
                MechanicSchedule.getInstance().cancelSlotForBooking(booking.getId());

                booking.setVehicleId(v.getId());
                booking.setDate(date);
                booking.setDescription(desc);
                booking.setStatus(status);
                booking.setServiceItemId(chosenService != null ? chosenService.getId() : 0);
                booking.setMechanicId(chosenMech != null ? chosenMech.getId() : 0);
                if (startTime != null) {
                    booking.setStartTime(startTime);
                    int estMin = chosenService != null ? chosenService.getEstimatedMinutes() : 60;
                    booking.setEndTime(startTime.plusMinutes(estMin));
                }

                try {
                    garage.updateBooking(booking);
                } catch (Exception ignored) {}

                // Återboka i schemat om mekaniker är tilldelad och bokningen ej är avbokad
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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("dialog.booking.cancel.title"));
        confirm.setHeaderText(I18n.get("dialog.booking.cancel.header"));
        confirm.setContentText(I18n.get("dialog.booking.cancel.confirm", booking.getId()));
        ActionDialogs.styleDialog(confirm);

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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("dialog.booking.delete.title"));
        confirm.setHeaderText(I18n.get("dialog.booking.delete.header"));
        confirm.setContentText(I18n.get("dialog.booking.delete.confirm", booking.getId()));
        ActionDialogs.styleDialog(confirm);

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

    /**
     * Delegerar till {@link BookingAvailability#isHourBooked}.
     */
    public static boolean isHourBooked(GarageSystem garage, Mechanic mechanic, LocalDate date, int hour, int excludeBookingId) {
        return BookingAvailability.isHourBooked(garage, mechanic, date, hour, excludeBookingId);
    }
}
