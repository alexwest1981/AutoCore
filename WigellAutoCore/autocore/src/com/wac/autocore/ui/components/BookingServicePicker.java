package com.wac.autocore.ui.components;

import java.util.ArrayList;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

/**
 * The services are picked as chips in the field, with price and time per service in the list.
 * A booking that already has services shows them as picked, in edit mode too.
 * Once the job has started the services cannot be changed.
 */
class BookingServicePicker {

    private final MultiSelectComboBox<ServiceItem> multi;

    BookingServicePicker(GarageSystem garage, Booking existingBooking,
            ObservableList<ServiceItem> selectedServices, boolean locked) {
        loadExistingServices(garage, existingBooking, selectedServices);

        this.multi = new MultiSelectComboBox<ServiceItem>(
                I18n.get("dialog.booking.service_select"),
                s -> SeedText.resolve(s.getName()),
                s -> UiFormatters.formatMoney(s.getPrice()) + " · " + s.getEstimatedMinutes() + " min");
        multi.setItems(garage.getServiceItems());
        multi.setKeyProvider(s -> s.getId());
        multi.setSelectedItems(new ArrayList<ServiceItem>(selectedServices));
        multi.getSelectedItems().addListener((ListChangeListener<ServiceItem>) c -> {
            selectedServices.setAll(multi.getSelectedItems());
        });
        if (locked) {
            multi.setDisable(true);
        }
    }

    MultiSelectComboBox<ServiceItem> getMulti() { return multi; }

    /** Picks the booking's services out of the register, whether they sit as records or as ids. */
    private void loadExistingServices(GarageSystem garage, Booking existingBooking,
            ObservableList<ServiceItem> selectedServices) {
        if (existingBooking == null) {
            return;
        }
        if (existingBooking.getServiceItems() != null && !existingBooking.getServiceItems().isEmpty()) {
            selectedServices.addAll(existingBooking.getServiceItems());
            return;
        }
        if (existingBooking.getServiceItemIds() != null && !existingBooking.getServiceItemIds().isEmpty()) {
            for (int sid : existingBooking.getServiceItemIds()) {
                addServiceById(garage, sid, selectedServices);
            }
            return;
        }
        if (existingBooking.getServiceItemId() > 0) {
            addServiceById(garage, existingBooking.getServiceItemId(), selectedServices);
        }
    }

    private void addServiceById(GarageSystem garage, int serviceId,
            ObservableList<ServiceItem> selectedServices) {
        for (ServiceItem s : garage.getServiceItems()) {
            if (s.getId() == serviceId) {
                selectedServices.add(s);
                return;
            }
        }
    }
}
