package com.wac.autocore.ui.components;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;

/**
 * The vehicle field in the booking form. Shows the vehicle with its owner on the same row,
 * and pre-selects the booking's vehicle when editing.
 */
class BookingVehicleField {

    private final ComboBox<Vehicle> box = new ComboBox<Vehicle>();

    BookingVehicleField(GarageSystem garage, Booking existingBooking) {
        box.getItems().addAll(garage.getVehicles());
        box.setMaxWidth(Double.MAX_VALUE);
        BookingFormPane.setupComboBoxDisplay(box, new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                if (v == null) return "";
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                return v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ") · " + owner;
            }
            @Override
            public Vehicle fromString(String string) { return null; }
        });
        // A new booking starts with nothing picked, so the field shows its prompt instead of the first
        // vehicle in the register.
        box.setPromptText(I18n.get("dialog.booking.vehicle_select"));
        if (existingBooking != null) {
            for (Vehicle v : box.getItems()) {
                if (v.getId() == existingBooking.getVehicleId()) {
                    box.getSelectionModel().select(v);
                    break;
                }
            }
        }
    }

    ComboBox<Vehicle> getBox() { return box; }
}
