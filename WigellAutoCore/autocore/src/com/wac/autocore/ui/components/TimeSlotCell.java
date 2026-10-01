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
                    ? "-fx-text-fill: #9ca3af; -fx-font-weight: normal;"
                    : "-fx-text-fill: #111827; -fx-font-weight: normal;");

            HBox box = new HBox(8, dot, textLabel);
            box.setAlignment(Pos.CENTER_LEFT);

            setGraphic(box);
            setText(null);

            if (isDropdownItem) {
                setDisable(busy);
                if (busy) {
                    setStyle("-fx-opacity: 0.50; -fx-background-color: transparent;");
                } else {
                    setStyle("-fx-opacity: 1.0; -fx-background-color: transparent;");
                }
            } else {
                setDisable(false);
                setStyle("-fx-background-color: transparent;");
            }
        }
    }
}
