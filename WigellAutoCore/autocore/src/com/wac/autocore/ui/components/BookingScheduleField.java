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
 * Datum, starttid och sluttid i bokningsformuläret.
 * Kalendern gråmarkerar dagar som inte rymmer hela arbetet, och tidslistan visar bara lediga tider.
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
        // 4. Bokningsdatum (visas som kalender där otillgängliga datum gråmarkeras)
        LocalDate initialDateVal = existingBooking != null && existingBooking.getDate() != null
        ? existingBooking.getDate()
        : (initialDate != null ? initialDate : LocalDate.now().plusDays(1));
        this.datePicker = new DatePicker(initialDateVal);
        this.datePicker.setVisible(false);
        this.datePicker.setManaged(false);

        // Kalenderns dagar. Fabriken sätts en gång och cellerna läser aktuella tjänster och tider
        // varje gång de ritas, så de kan uppdateras med refresh() när valet ändras.
        this.datePicker.setDayCellFactory(picker -> {
        BookingDayCell cell = new BookingDayCell(garage, excludeId, form);
        dayCells.add(cell);
        return cell;
        });

        com.sun.javafx.scene.control.skin.DatePickerSkin dateSkin =
        new com.sun.javafx.scene.control.skin.DatePickerSkin(this.datePicker);
        this.calendarNode = dateSkin.getPopupContent();
        // Temafärgerna sätts i stilmallen (.booking-calendar): en inline-style kan inte slå upp
        // -wac-card/-wac-line, så de föll tyst bort och kalendern blev genomskinlig.
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

        // Ritar om kalenderns dagar när tjänster, mekaniker eller datum ändrats. Cellerna behåller
        // annars den bedömning de gjorde när de skapades, och en dag som inte längre rymmer hela
        // jobbet stod kvar som bokningsbar.
        Runnable setupDatePickerCells = () -> {
        for (BookingDayCell cell : dayCells) {
        cell.refresh();
        }
        };

        Runnable ensureValidDate = () -> {
        int duration = form.getTotalEstimatedMinutes() > 0 ? form.getTotalEstimatedMinutes() : 60;
        List<Mechanic> team = form.getSelectedMechanics();   // de mekaniker som är valda i fältet
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

        // 5. Starttid med stängningsspärr (17:00) och tillgänglighetsindikering

        Function<LocalTime, Boolean> isBusyFunc = time -> {
        if (time == null) return false;
        int duration = form.getTotalEstimatedMinutes() > 0 ? form.getTotalEstimatedMinutes() : 60;
        LocalTime end = time.plusMinutes(duration);
        if (end.isAfter(BookingAvailability.CLOSING_TIME)) {
        return true;
        }
        LocalDate d = datePicker.getValue();
        List<Mechanic> team = form.getSelectedMechanics();   // de mekaniker som är valda i fältet
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

        // 6. Dynamisk sluttidsberäkning
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
        // Bara de tider som går att boka: de som ryms före stängning och är lediga för vald
        // mekaniker. Upptagna tider visas inte alls, och finns det ingen kvar stängs fältet av.
        List<LocalTime> freeTimes = new ArrayList<LocalTime>();
        for (int h = 7; h <= 16; h++) {
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

        // Koppla lyssnare
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

    /** Väljer den första lediga tiden, annars klockan 08. */
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

    /** Läser om dagarna och väljer ett giltigt datum. */
    void refreshDate() {
        setupDatePickerCells.run();
        ensureValidDate.run();
    }

    /** Läser bara om tiderna. */
    void refreshTimes() {
        refreshTimeBox.run();
    }

    /** Läser om dagarna, datumet och tiderna. */
    void refresh() {
        setupDatePickerCells.run();
        ensureValidDate.run();
        refreshTimeBox.run();
    }
}
