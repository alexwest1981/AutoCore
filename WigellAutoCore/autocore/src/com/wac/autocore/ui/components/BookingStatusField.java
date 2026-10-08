package com.wac.autocore.ui.components;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import com.wac.autocore.model.Booking;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

/**
 * The description and the status in the booking form. The status only appears while
 * editing: once the job has started it cannot go back to Booked or Confirmed, and the
 * booking cannot be cancelled, because that unlocks the services again.
 */
class BookingStatusField {

    private final TextField desc;
    private final ComboBox<String> status;

    BookingStatusField(Booking existingBooking) {
        String initialDesc = existingBooking != null && existingBooking.getDescription() != null
                ? SeedText.resolve(existingBooking.getDescription()) : "";
        this.desc = new TextField(initialDesc);
        this.desc.setPromptText(I18n.get("dialog.booking.desc_prompt"));

        if (existingBooking == null) {
            this.status = null;
            return;
        }
        this.status = new ComboBox<String>();
        if (existingBooking.isWorkStarted()) {
            status.getItems().addAll("IN_PROGRESS", "COMPLETED");
            if (existingBooking.getStatus() != null
                    && !status.getItems().contains(existingBooking.getStatus())) {
                status.getItems().add(existingBooking.getStatus());
            }
        } else {
            status.getItems().addAll("BOOKED", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED");
        }
        status.setMaxWidth(Double.MAX_VALUE);
        BookingFormPane.setupComboBoxDisplay(status, new StringConverter<String>() {
            @Override
            public String toString(String st) {
                return st != null ? UiFormatters.statusWord(st) : "";
            }
            @Override
            public String fromString(String string) { return null; }
        });
        status.getSelectionModel().select(existingBooking.getStatus() != null ? existingBooking.getStatus() : "BOOKED");
    }

    TextField getDesc() { return desc; }

    /** The status list, or null for a new booking. */
    ComboBox<String> getStatus() { return status; }
}
