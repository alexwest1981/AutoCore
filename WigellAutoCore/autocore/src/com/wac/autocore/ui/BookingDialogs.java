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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

/** Dialogerna för att skapa, ändra, avboka och ta bort en bokning. */
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
        // Lite större fönster: formuläret är 820 brett, och höjden får en undre gräns så att
        // kalendern och tiden inte kläms ihop.
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

                // Skapa ren bokning i systemet (INGEN arbetsorder skapas eller startas automatiskt)
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
                        int estMin = b.getTotalEstimatedMinutes() > 0 ? b.getTotalEstimatedMinutes() : (chosenService != null ? chosenService.getEstimatedMinutes() : 60);
                        b.setEndTime(startTime.plusMinutes(estMin));
                    }

                    if (!saveBookingOrReport(garage, b)) {
                        return;
                    }

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
        dialog.setResizable(true);
        // Lite större fönster: formuläret är 820 brett, och höjden får en undre gräns så att
        // kalendern och tiden inte kläms ihop.
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setMinWidth(760);
        dialog.getDialogPane().setMinHeight(720);

        BookingFormPane form = new BookingFormPane(garage, booking, booking.getDate(), null, null);
        form.setOnContentGrown(() -> ActionDialogs.growToFitContent(dialog));
        dialog.getDialogPane().setContent(form);
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

                // Rensa eventuell tidigare schemaplats för denna bokning
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

                // Statusen sätts sist, för en statusändring tillbaka till Bokad öppnar
                // låset på tjänsterna igen. Är arbetet påbörjat får den inte gå tillbaka.
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
                    int estMin = booking.getTotalEstimatedMinutes() > 0 ? booking.getTotalEstimatedMinutes() : (chosenService != null ? chosenService.getEstimatedMinutes() : 60);
                    booking.setEndTime(startTime.plusMinutes(estMin));
                }

                if (!saveBookingOrReport(garage, booking)) {
                    return;
                }

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

/** Sparar bokningen och visar felet i stället för att svälja det. */
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


    /** Statusar som får sättas när arbetet redan har påbörjats. */
    private static boolean statusTillaten(String status) {
        return "IN_PROGRESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status);
    }

    /** Id på de mekaniker som är valda i formuläret, i den ordning de valdes. */
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
