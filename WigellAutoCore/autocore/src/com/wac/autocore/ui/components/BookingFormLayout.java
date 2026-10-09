package com.wac.autocore.ui.components;

import com.wac.autocore.model.ServicePackage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.ui.i18n.I18n;

import java.time.LocalTime;

/**
 * The row layout of the booking form:
 * Vehicle -> Service package -> Services -> Mechanic -> Date & Time -> Description -> Status.
 * In drop-in mode Date & Time is swapped for the time the service works out.
 */
class BookingFormLayout {

    private final GridPane grid;
    private final boolean dropIn;

    BookingFormLayout(GridPane grid, boolean dropIn) {
        this.grid = grid;
        this.dropIn = dropIn;
    }

    void layout(ComboBox<Vehicle> vehicleBox,
                ComboBox<ServicePackage> packageBox,
                MultiSelectComboBox<ServiceItem> serviceMulti,
                MultiSelectComboBox<Mechanic> mechanicMulti, Label mechanicFilterHint,
                BookingScheduleField scheduleField,
                DatePicker datePicker, ComboBox<LocalTime> startTimeBox, Label durationLabel,
                TextField descField, ComboBox<String> statusBox, Label dropInTimeLabel,
                BookingServicesField servicesField,
                boolean isServicesLocked) {
        datePicker.setMaxWidth(Double.MAX_VALUE);
        startTimeBox.setMaxWidth(Double.MAX_VALUE);
        descField.setMaxWidth(Double.MAX_VALUE);
        if (statusBox != null) {
            statusBox.setMaxWidth(Double.MAX_VALUE);
        }

        int rowIdx = 0;

        // Fordon
        grid.add(new Label(I18n.get("dialog.booking.vehicle_select") + ":"), 0, rowIdx);
        GridPane.setHgrow(vehicleBox, Priority.ALWAYS);
        grid.add(vehicleBox, 1, rowIdx++);

        grid.add(new Label(I18n.get("dialog.booking.package_select") + ":"), 0, rowIdx);
        GridPane.setHgrow(packageBox, Priority.ALWAYS);
        grid.add(packageBox, 1, rowIdx++);

        // Tjänster
        Label serviceLbl = new Label(I18n.get("dialog.booking.service_select") + ":");
        GridPane.setValignment(serviceLbl, VPos.TOP);
        serviceLbl.setPadding(new Insets(6, 0, 0, 0));
        grid.add(serviceLbl, 0, rowIdx);
        // The field should fill the column, otherwise the arrow stops well inside the edge.
        serviceMulti.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(serviceMulti, Priority.ALWAYS);
        VBox serviceCol = new VBox(6, serviceMulti);
        GridPane.setHgrow(serviceCol, Priority.ALWAYS);
        if (isServicesLocked) {
            Label lockNotice = new Label("🔒 " + I18n.get("dialog.booking.services_locked_work_started"));
            lockNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
            serviceCol.getChildren().add(lockNotice);
        }
        grid.add(serviceCol, 1, rowIdx++);

        // Mekaniker
        Label mechLbl = new Label(I18n.get("dialog.booking.mechanic_select") + ":");
        GridPane.setValignment(mechLbl, VPos.TOP);
        mechLbl.setPadding(new Insets(6, 0, 0, 0));
        grid.add(mechLbl, 0, rowIdx);
        mechanicMulti.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mechanicMulti, Priority.ALWAYS);
        VBox mechCol = new VBox(4, mechanicMulti, mechanicFilterHint);
        GridPane.setHgrow(mechCol, Priority.ALWAYS);
        grid.add(mechCol, 1, rowIdx++);

        // Date & Time (the calendar and what the booking holds side by side, the start time beneath
        // them). A drop-in is booked as the customer walks in; there is nothing to pick there, so
        // the row shows the worked-out time instead.
        if (!dropIn) {
            Label dateTimeLbl = new Label(I18n.get("dialog.booking.date_and_time") + ":");
            GridPane.setValignment(dateTimeLbl, VPos.TOP);
            dateTimeLbl.setPadding(new Insets(6, 0, 0, 0));
            grid.add(dateTimeLbl, 0, rowIdx);

            VBox calCol = new VBox(10, scheduleField.getDateHeader(), scheduleField.getCalendarHint(), scheduleField.getCalendarNode());
            calCol.setAlignment(Pos.TOP_LEFT);
            calCol.getStyleClass().add("booking-card");

            // What the booking holds, grouped by package, with time and price per service.
            Label contentsTitle = new Label(I18n.get("dialog.booking.packages_and_services"));
            contentsTitle.getStyleClass().add("booking-card-title");
            VBox contentsCol = new VBox(8, contentsTitle, servicesField.getScroll(), servicesField.getSummary());
            contentsCol.setAlignment(Pos.TOP_LEFT);
            // The list swallows the height the card has left over, so the summary ends up at the
            // bottom of the card rather than leaving a gap under it.
            VBox.setVgrow(servicesField.getScroll(), Priority.ALWAYS);
            contentsCol.getStyleClass().add("booking-card");

            // Half each, so the two cards end up the same width whatever the calendar asks for.
            GridPane cards = new GridPane();
            cards.setHgap(16);
            ColumnConstraints calendarHalf = new ColumnConstraints();
            calendarHalf.setPercentWidth(50);
            ColumnConstraints contentsHalf = new ColumnConstraints();
            contentsHalf.setPercentWidth(50);
            cards.getColumnConstraints().addAll(calendarHalf, contentsHalf);
            // The row is as tall as the calendar, and the contents card is stretched to match it,
            // so the two cards end level.
            GridPane.setFillHeight(calCol, false);
            GridPane.setValignment(calCol, VPos.TOP);
            cards.add(calCol, 0, 0);
            cards.add(contentsCol, 1, 0);

            // The start time runs underneath both cards and takes the full width.
            Label timeTitle = new Label(I18n.get("dialog.booking.time_select") + ":");
            timeTitle.getStyleClass().add("booking-card-title");
            Label timeHint = new Label(I18n.get("dialog.booking.only_free_times"));
            timeHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
            timeHint.setWrapText(true);
            startTimeBox.setMaxWidth(Double.MAX_VALUE);
            VBox timeCol = new VBox(8, timeTitle, startTimeBox, timeHint, durationLabel);
            timeCol.setAlignment(Pos.CENTER_LEFT);
            timeCol.getStyleClass().add("booking-card");

            VBox dateTimeCol = new VBox(12, cards, timeCol);
            dateTimeCol.setAlignment(Pos.TOP_LEFT);

            // The date picker itself is not put into the layout: the calendar above is built from
            // a skin of its own, and putting the control into the scene anyway makes JavaFX create
            // a second skin — then DatePickerSkin throws "duplicate children added" and the form dies on click.
            GridPane.setHgrow(dateTimeCol, Priority.ALWAYS);
            grid.add(dateTimeCol, 1, rowIdx++);
        } else {
            // The customer should see when the car starts and when it is done before the booking is approved.
            Label timeLbl = new Label(I18n.get("dialog.booking.dropin_time") + ":");
            GridPane.setValignment(timeLbl, VPos.TOP);
            timeLbl.setPadding(new Insets(6, 0, 0, 0));
            grid.add(timeLbl, 0, rowIdx);

            VBox timeCol = new VBox(6, dropInTimeLabel);
            timeCol.setAlignment(Pos.CENTER_LEFT);
            timeCol.setMinWidth(220);
            timeCol.setPrefWidth(240);
            timeCol.getStyleClass().add("booking-card");
            GridPane.setHgrow(timeCol, Priority.ALWAYS);
            grid.add(timeCol, 1, rowIdx++);
        }

        // Beskrivning
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        GridPane.setHgrow(descField, Priority.ALWAYS);
        grid.add(descField, 1, rowIdx++);

        // Status (when editing)
        if (statusBox != null) {
            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            GridPane.setHgrow(statusBox, Priority.ALWAYS);
            grid.add(statusBox, 1, rowIdx++);
        }
    }
}
