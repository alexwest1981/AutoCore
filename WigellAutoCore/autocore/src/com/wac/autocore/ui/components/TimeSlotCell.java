package com.wac.autocore.ui.components;

import com.wac.autocore.ui.i18n.I18n;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Anpassad ListCell för starttider: grön för ledig tid, röd för upptagen tid.
 * Inaktiverar automatiskt upptagna tider i popup-listan så att dubbelbokning förhindras.
 */
public class TimeSlotCell extends ListCell<LocalTime> {

    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Function<LocalTime, Boolean> isBusyFunc;
    private final boolean isDropdownItem;

    public TimeSlotCell(Function<LocalTime, Boolean> isBusyFunc, boolean isDropdownItem) {
        this.isBusyFunc = isBusyFunc;
        this.isDropdownItem = isDropdownItem;
    }

    @Override
    protected void updateItem(LocalTime time, boolean empty) {
        super.updateItem(time, empty);
        if (empty || time == null) {
            setText(null);
            setGraphic(null);
            setStyle("");
            setDisable(false);
        } else {
            boolean busy = isBusyFunc != null && Boolean.TRUE.equals(isBusyFunc.apply(time));
            String timeStr = time.format(TIME_FMT);
            String statusText = busy ? I18n.get("dialog.booking.time_busy") : I18n.get("dialog.booking.time_available");

            Circle dot = new Circle(4);
            dot.setFill(Color.web(busy ? "#f87171" : "#22c55e"));

            Label textLabel = new Label(timeStr + "  (" + statusText + ")");
            textLabel.setStyle(busy
                    ? "-fx-text-fill: #f87171; -fx-font-weight: bold;"
                    : "-fx-text-fill: #22c55e; -fx-font-weight: bold;");

            HBox box = new HBox(8, dot, textLabel);
            box.setAlignment(Pos.CENTER_LEFT);

            setGraphic(box);
            setText(null);

            if (isDropdownItem) {
                setDisable(busy);
                if (busy) {
                    setStyle("-fx-opacity: 0.60; -fx-background-color: rgba(248, 113, 113, 0.12);");
                } else {
                    setStyle("-fx-opacity: 1.0; -fx-background-color: rgba(34, 197, 94, 0.08);");
                }
            } else {
                setDisable(false);
                setStyle("");
            }
        }
    }
}
