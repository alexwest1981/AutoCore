package com.wac.autocore.ui.components;

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
    private final ScheduleSource source;

    BookingDayCell(GarageSystem garage, int excludeId, ScheduleSource source) {
        this.garage = garage;
        this.excludeId = excludeId;
        this.source = source;
    }

    @Override
    public void updateItem(LocalDate date, boolean empty) {
        super.updateItem(date, empty);
        if (empty || date == null) {
            return;
        }
        int duration = source.getTotalEstimatedMinutes() > 0 ? source.getTotalEstimatedMinutes() : 60;
        List<Mechanic> team = source.getSelectedMechanics();   // the mechanics picked in the field
        String reason = BookingAvailability.unbookableReason(garage, date, source.getSelectedMechanic(), team,
                duration, excludeId);
        if (reason != null) {
            boolean tooLong = "dialog.booking.duration_too_long".equals(reason)
                    || "dialog.booking.date_fully_booked".equals(reason);
            unbookable(tooLong
                    ? "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-opacity: 0.50;"
                    : "-fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-opacity: 0.45;", reason);
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
