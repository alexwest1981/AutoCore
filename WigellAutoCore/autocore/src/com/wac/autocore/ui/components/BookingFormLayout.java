package com.wac.autocore.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
 * Radlayouten i bokningsformuläret:
 * Fordon -> Tjänster -> Mekaniker -> Datum & Tid -> Beskrivning -> Status.
 * I drop-in-läget hoppas Datum & Tid över: där finns ingen tid att välja.
 */
class BookingFormLayout {

    private final GridPane grid;
    private final boolean dropIn;

    BookingFormLayout(GridPane grid, boolean dropIn) {
        this.grid = grid;
        this.dropIn = dropIn;
    }

    void layout(ComboBox<Vehicle> vehicleBox,
                MultiSelectComboBox<ServiceItem> serviceMulti, Label totalSummaryLabel,
                MultiSelectComboBox<Mechanic> mechanicMulti, Label mechanicFilterHint,
                BookingScheduleField scheduleField,
                DatePicker datePicker, ComboBox<LocalTime> startTimeBox, Label durationLabel,
                TextField descField, ComboBox<String> statusBox,
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

        // Tjänster
        Label serviceLbl = new Label(I18n.get("dialog.booking.service_select") + ":");
        GridPane.setValignment(serviceLbl, VPos.TOP);
        serviceLbl.setPadding(new Insets(6, 0, 0, 0));
        grid.add(serviceLbl, 0, rowIdx);
        // Fältet ska fylla kolumnen, annars stannar pilen långt in från kanten.
        serviceMulti.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(serviceMulti, Priority.ALWAYS);
        VBox serviceCol = new VBox(6, serviceMulti);
        GridPane.setHgrow(serviceCol, Priority.ALWAYS);
        if (isServicesLocked) {
            Label lockNotice = new Label("🔒 " + I18n.get("dialog.booking.services_locked_work_started"));
            lockNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
            serviceCol.getChildren().add(lockNotice);
        }
        serviceCol.getChildren().add(totalSummaryLabel);
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

        // Datum & Tid (kalendern synlig med starttiden bredvid). En drop-in bokas när kunden
        // kommer in, så det finns inget att välja och raden lämnas borta.
        if (!dropIn) {
            Label dateTimeLbl = new Label(I18n.get("dialog.booking.date_and_time") + ":");
            GridPane.setValignment(dateTimeLbl, VPos.TOP);
            dateTimeLbl.setPadding(new Insets(6, 0, 0, 0));
            grid.add(dateTimeLbl, 0, rowIdx);

            VBox calCol = new VBox(10, scheduleField.getDateHeader(), scheduleField.getCalendarHint(), scheduleField.getCalendarNode());
            calCol.setAlignment(Pos.TOP_LEFT);
            calCol.getStyleClass().add("booking-card");

            Label timeTitle = new Label(I18n.get("dialog.booking.time_select") + ":");
            timeTitle.getStyleClass().add("booking-card-title");
            Label timeHint = new Label(I18n.get("dialog.booking.only_free_times"));
            timeHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
            timeHint.setWrapText(true);
            VBox timeCol = new VBox(8, timeTitle, startTimeBox, timeHint, durationLabel);
            timeCol.setAlignment(Pos.CENTER_LEFT);
            timeCol.setMinWidth(220);
            timeCol.setPrefWidth(240);
            HBox.setHgrow(timeCol, Priority.ALWAYS);
            startTimeBox.setMaxWidth(Double.MAX_VALUE);
            timeCol.getStyleClass().add("booking-card");

            HBox dateTimeRow = new HBox(16, calCol, timeCol);
            dateTimeRow.setAlignment(Pos.TOP_LEFT);

            // Datumväljaren själv läggs inte i layouten: kalendern ovan är byggd från ett eget
            // skinn, och lägger man ändå kontrollen i scenen skapar JavaFX ett andra skinn —
            // då kastar DatePickerSkin "duplicate children added" och formuläret dör vid klick.
            VBox dateTimeContainer = new VBox(4, dateTimeRow);
            GridPane.setHgrow(dateTimeContainer, Priority.ALWAYS);
            grid.add(dateTimeContainer, 1, rowIdx++);
        }

        // Beskrivning
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, rowIdx);
        GridPane.setHgrow(descField, Priority.ALWAYS);
        grid.add(descField, 1, rowIdx++);

        // Status (om redigering)
        if (statusBox != null) {
            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, rowIdx);
            GridPane.setHgrow(statusBox, Priority.ALWAYS);
            grid.add(statusBox, 1, rowIdx++);
        }
    }
}
