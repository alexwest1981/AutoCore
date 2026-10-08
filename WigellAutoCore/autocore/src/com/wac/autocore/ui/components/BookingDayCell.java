package com.wac.autocore.ui.components;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import javafx.scene.control.DateCell;
import javafx.scene.control.Tooltip;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.BookingAvailability;

/** A day in the calendar. The rules are read on every draw. */
class BookingDayCell extends DateCell {

    private final GarageSystem garage;
    private final int excludeId;
    private final BookingFormPane form;

    BookingDayCell(GarageSystem garage, int excludeId, BookingFormPane form) {
        this.garage = garage;
        this.excludeId = excludeId;
        this.form = form;
    }

    @Override
    public void updateItem(LocalDate date, boolean empty) {
        super.updateItem(date, empty);
        if (empty || date == null) {
            return;
        }
        if (date.isBefore(LocalDate.now())) {
            unbookable("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-opacity: 0.45;",
                    "dialog.booking.date_past");
            return;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            unbookable("-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-opacity: 0.45;",
                    "dialog.booking.date_weekend");
            return;
        }
        int duration = form.getTotalEstimatedMinutes() > 0 ? form.getTotalEstimatedMinutes() : 60;
        if (duration > BookingAvailability.MAX_WORK_MINUTES_PER_DAY) {
            unbookable("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-opacity: 0.50;",
                    "dialog.booking.duration_too_long");
            return;
        }
        List<Mechanic> team = form.getSelectedMechanics();   // de mekaniker som är valda i fältet
        if (!BookingAvailability.hasAvailableSlotOnDate(garage, form.getSelectedMechanic(), team, date, duration, excludeId)) {
            unbookable("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-opacity: 0.50;",
                    "dialog.booking.date_fully_booked");
            return;
        }
        setDisable(false);
        getStyleClass().remove("unbookable-day");
        setStyle("");
        setTooltip(null);
    }

    /** Redraws the cell with the choices that apply now. */
    void refresh() {
        updateItem(getItem(), isEmpty());
    }

    private void unbookable(String style, String messageKey) {
        setDisable(true);
        if (!getStyleClass().contains("unbookable-day")) {
            getStyleClass().add("unbookable-day");
        }
        setStyle(style);
        setTooltip(new Tooltip(I18n.get(messageKey)));
    }
}
