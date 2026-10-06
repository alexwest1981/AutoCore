package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.BookingFormPane;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Drop-in: arbetsorder för en kund som kommer in utan bokad tid. Formuläret är bokningens eget,
 *  utan Datum & Tid, så fälten och reglerna är desamma som i en vanlig bokning. */
final class CreateDropInWorkOrderDialog {

    private CreateDropInWorkOrderDialog() {}

    static void show(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.dropin.title"));
        dialog.setHeaderText(I18n.get("dialog.workorder.dropin.header"));
        ActionDialogs.styleDialog(dialog);

        BookingFormPane form = new BookingFormPane(garage, null, LocalDate.now(), null, null, true);
        form.setOnContentGrown(() -> ActionDialogs.growToFitContent(dialog));

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, form.requiredFieldsFilledBinding(0));

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (!form.validate(garage, 0)) {
                    return;
                }

                Vehicle vehicle = form.getSelectedVehicle();
                List<ServiceItem> services = form.getSelectedServices();
                List<Mechanic> mechanics = form.getSelectedMechanics();

                Booking booking = garage.createDropInBooking(vehicle.getId(), services, mechanics);
                if (booking == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.workorder.create_failed"));
                    return;
                }

                // Samma fördelning som bokningsdialogen: en arbetsorder per mekaniker, var och en
                // med sina egna tjänster.
                Map<Integer, List<ServiceItem>> plan = WorkOrderPlan.planWorkOrders(garage, booking);
                int created = 0;
                for (Map.Entry<Integer, List<ServiceItem>> entry : plan.entrySet()) {
                    List<Integer> ids = new ArrayList<Integer>();
                    for (ServiceItem s : entry.getValue()) {
                        ids.add(Integer.valueOf(s.getId()));
                    }
                    if (garage.createWorkOrder(booking.getId(), entry.getKey().intValue(), ids) != null) {
                        created++;
                    }
                }

                if (created == 0) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.workorder.create_failed"));
                    return;
                }
                if (created < plan.size()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                            I18n.get("dialog.workorder.partial_failed")
                                    .replace("{0}", String.valueOf(created))
                                    .replace("{1}", String.valueOf(plan.size())));
                }

                com.wac.autocore.service.MechanicSchedule.getInstance().syncFromDatabase();
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
