package com.wac.autocore.ui.components;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.BookingAvailability;
import com.wac.autocore.ui.util.UiFormatters;

/**
 * The date, the start time and the end time in the booking form.
 * The calendar greys out days that cannot hold the whole job, and the time list shows free times only.
 */
class BookingScheduleField {

    private final DatePicker datePicker;
    private final ComboBox<LocalTime> startTimeBox = new ComboBox<LocalTime>();
    private final Label durationLabel = new Label();
    private final List<BookingDayCell> dayCells = new ArrayList<BookingDayCell>();
    private final Label dateHeaderLabel;
    private final Label calendarHintLabel;
    private final Node calendarNode;
    private final Runnable setupDatePickerCells;
    private final Runnable ensureValidDate;
    private final Runnable refreshTimeBox;

    BookingScheduleField(GarageSystem garage, BookingFormPane form, ComboBox<Mechanic> mechanicBox,
                         int excludeId, Booking existingBooking, LocalDate initialDate) {
        LocalDate initialDateVal = existingBooking != null && existingBooking.getDate() != null
        ? existingBooking.getDate()
        : (initialDate != null ? initialDate : LocalDate.now().plusDays(1));
        this.datePicker = new DatePicker(initialDateVal);
        this.datePicker.setVisible(false);
        this.datePicker.setManaged(false);

        // The calendar's days. The factory is set once, and the cells read the current services and
        // times each time they are drawn, so refresh() can update them when the selection changes.
        this.datePicker.setDayCellFactory(picker -> {
        BookingDayCell cell = new BookingDayCell(garage, excludeId, form);
        dayCells.add(cell);
        return cell;
        });

        com.sun.javafx.scene.control.skin.DatePickerSkin dateSkin =
        new com.sun.javafx.scene.control.skin.DatePickerSkin(this.datePicker);
        this.calendarNode = dateSkin.getPopupContent();
        // The theme colours are set in the stylesheet (.booking-calendar): an inline style cannot
        // resolve -wac-card/-wac-line, so they dropped out silently and the calendar went see-through.
        calendarNode.getStyleClass().add("booking-calendar");

        Label dateHeaderLabel = new Label();
        dateHeaderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -wac-text;");
        Label calendarHintLabel = new Label(I18n.get("dialog.booking.calendar_hint"));
        calendarHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -wac-muted;");
        calendarHintLabel.setWrapText(true);
        calendarHintLabel.setMaxWidth(230);

        Runnable updateDateHeader = () -> {
        LocalDate d = datePicker.getValue();
        if (d != null) {
        dateHeaderLabel.setText("📅 " + I18n.get("dialog.booking.selected_date", UiFormatters.formatDate(d)));
        } else {
        dateHeaderLabel.setText("");
        }
        };
        this.datePicker.valueProperty().addListener((obs, o, n) -> updateDateHeader.run());
        updateDateHeader.run();

        // Redraws the calendar's days when services, mechanics or the date have changed. Otherwise
        // the cells keep the judgement they made when they were created, and a day that no longer
        // holds the whole job stayed bookable.
        Runnable setupDatePickerCells = () -> {
        for (BookingDayCell cell : dayCells) {
        cell.refresh();
        }
        };

        Runnable ensureValidDate = () -> {
        int duration = form.getTotalEstimatedMinutes() > 0 ? form.getTotalEstimatedMinutes() : 60;
        List<Mechanic> team = form.getSelectedMechanics();   // the mechanics picked in the field
        Mechanic m = form.getSelectedMechanic();
        LocalDate current = datePicker.getValue();
        if (current == null || !BookingAvailability.hasAvailableSlotOnDate(garage, m, team, current, duration, excludeId)) {
        LocalDate check = LocalDate.now().plusDays(1);
        for (int i = 0; i < 60; i++) {
        if (BookingAvailability.hasAvailableSlotOnDate(garage, m, team, check, duration, excludeId)) {
        datePicker.setValue(check);
        break;
        }
        check = check.plusDays(1);
        }
        }
        };

        // Start time, blocked after closing (17:00), with availability marked.

        Function<LocalTime, Boolean> isBusyFunc = time -> {
        if (time == null) return false;
        int duration = form.getTotalEstimatedMinutes() > 0 ? form.getTotalEstimatedMinutes() : 60;
        LocalTime end = time.plusMinutes(duration);
        if (end.isAfter(BookingAvailability.CLOSING_TIME)) {
        return true;
        }
        LocalDate d = datePicker.getValue();
        List<Mechanic> team = form.getSelectedMechanics();   // the mechanics picked in the field
        if (team != null && !team.isEmpty()) {
        return BookingAvailability.isTeamBooked(garage, team, d, time, end, excludeId);
        }
        Mechanic m = form.getSelectedMechanic();
        return BookingAvailability.isRangeBooked(garage, m, d, time, end, excludeId);
        };

        this.startTimeBox.setCellFactory(lv -> new TimeSlotCell(isBusyFunc, true));
        this.startTimeBox.setButtonCell(new TimeSlotCell(isBusyFunc, false));
        this.startTimeBox.setConverter(new StringConverter<LocalTime>() {
        @Override
        public String toString(LocalTime t) {
        return t == null ? "" : t.format(TimeSlotCell.TIME_FMT);
        }
        @Override
        public LocalTime fromString(String string) { return null; }
        });

        // End time recalculated from the start time and the total duration.
        this.durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");

        Runnable updateDuration = () -> {
        LocalTime start = startTimeBox.getValue();
        int totalMin = form.getTotalEstimatedMinutes();
        if (start != null && totalMin > 0) {
        LocalTime end = start.plusMinutes(totalMin);
        if (end.isAfter(BookingAvailability.CLOSING_TIME)) {
        durationLabel.setText("⚠️ " + I18n.get("dialog.booking.time_window",
        start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT))
        + " (" + totalMin + " min) – " + I18n.get("dialog.booking.closing_time_exceeded"));
        durationLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        } else {
        durationLabel.setText(I18n.get("dialog.booking.time_window",
        start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT))
        + " (" + totalMin + " min)");
        durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");
        }
        } else if (start != null) {
        LocalTime end = start.plusHours(1);
        durationLabel.setText(I18n.get("dialog.booking.time_window",
        start.format(TimeSlotCell.TIME_FMT), end.format(TimeSlotCell.TIME_FMT)) + " (60 min)");
        durationLabel.setStyle("-fx-text-fill: -wac-accent; -fx-font-weight: bold;");
        } else {
        durationLabel.setText("");
        }
        };

        Runnable refreshTimeBox = () -> {
        int duration = form.getTotalEstimatedMinutes() > 0 ? form.getTotalEstimatedMinutes() : 60;
        // Only the times that can be booked: those that fit before closing and are free for the
        // picked mechanic. Busy times are not shown at all, and if none are left the field is locked.
        List<LocalTime> freeTimes = new ArrayList<LocalTime>();
        // On today's date the hours already gone cannot be booked, so the list starts at the
        // current one. Any other day still starts at opening.
        LocalDate pickedDate = datePicker.getValue();
        int firstHour = pickedDate != null && pickedDate.isEqual(LocalDate.now())
                ? Math.max(7, LocalTime.now().getHour()) : 7;
        for (int h = firstHour; h <= 16; h++) {
        LocalTime t = LocalTime.of(h, 0);
        if (t.plusMinutes(duration).isAfter(BookingAvailability.CLOSING_TIME)) {
        continue;
        }
        if (Boolean.TRUE.equals(isBusyFunc.apply(t))) {
        continue;
        }
        freeTimes.add(t);
        }
        LocalTime currentSel = startTimeBox.getValue();
        startTimeBox.setItems(FXCollections.observableArrayList(freeTimes));
        boolean none = freeTimes.isEmpty();
        startTimeBox.setDisable(none);
        startTimeBox.setPromptText(none ? I18n.get("dialog.booking.no_free_times") : null);
        if (currentSel != null && freeTimes.contains(currentSel)) {
        startTimeBox.getSelectionModel().select(currentSel);
        } else {
        selectFirstAvailableTime(freeTimes, isBusyFunc);
        }
        if (startTimeBox.getButtonCell() != null) {
        startTimeBox.getButtonCell().updateIndex(-1);
        }
        updateDuration.run();
        };

        // Wire up the listeners
        this.datePicker.valueProperty().addListener((obs, o, n) -> refreshTimeBox.run());
        mechanicBox.valueProperty().addListener((obs, o, n) -> {
        setupDatePickerCells.run();
        refreshTimeBox.run();
        });
        this.startTimeBox.valueProperty().addListener((obs, o, n) -> updateDuration.run());
        this.setupDatePickerCells = setupDatePickerCells;
        this.ensureValidDate = ensureValidDate;
        this.refreshTimeBox = refreshTimeBox;
        this.dateHeaderLabel = dateHeaderLabel;
        this.calendarHintLabel = calendarHintLabel;
    }

    DatePicker getDatePicker() { return datePicker; }

    ComboBox<LocalTime> getStartTimeBox() { return startTimeBox; }

    Label getDurationLabel() { return durationLabel; }

    Node getCalendarNode() { return calendarNode; }

    Label getDateHeader() { return dateHeaderLabel; }

    Label getCalendarHint() { return calendarHintLabel; }

    /** Picks the first free time, otherwise 08:00. */
    private void selectFirstAvailableTime(List<LocalTime> timeOptions, Function<LocalTime, Boolean> isBusyFunc) {
        LocalTime firstFree = null;
        for (LocalTime t : timeOptions) {
            if (!Boolean.TRUE.equals(isBusyFunc.apply(t))) {
                firstFree = t;
                break;
            }
        }
        startTimeBox.getSelectionModel().select(firstFree != null ? firstFree : LocalTime.of(8, 0));
    }

    /** Re-reads the days and picks a valid date. */
    void refreshDate() {
        setupDatePickerCells.run();
        ensureValidDate.run();
    }

    /** Re-reads the times only. */
    void refreshTimes() {
        refreshTimeBox.run();
    }

    /** Re-reads the days, the date and the times. */
    void refresh() {
        setupDatePickerCells.run();
        ensureValidDate.run();
        refreshTimeBox.run();
    }
}
