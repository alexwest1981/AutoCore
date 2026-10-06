package com.wac.autocore.ui.components;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.EntityLookup;

/**
 * Fordonsfältet i bokningsformuläret. Visar fordonet med ägaren i samma rad,
 * och förväljer bokningens fordon vid redigering.
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
        if (existingBooking != null) {
            for (Vehicle v : box.getItems()) {
                if (v.getId() == existingBooking.getVehicleId()) {
                    box.getSelectionModel().select(v);
                    break;
                }
            }
        } else {
            box.getSelectionModel().selectFirst();
        }
    }

    ComboBox<Vehicle> getBox() { return box; }
}
