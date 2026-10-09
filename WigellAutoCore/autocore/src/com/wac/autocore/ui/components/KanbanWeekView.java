package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.model.DayLoad;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import com.wac.autocore.seed.SeedText;

/** One part of the mechanic card. */
class KanbanWeekView {

    private final MechanicKanbanCard card;

    KanbanWeekView(MechanicKanbanCard card) {
        this.card = card;
    }

        Node dayRow(Mechanic mech, DayLoad dl) {
        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String dayName = dl.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        String dateStr = dayName + " " + dl.getDate().getDayOfMonth();

        Label dayLabel = new Label(dateStr);
        dayLabel.getStyleClass().add("kanban-week-day-name");
        dayLabel.setMinWidth(60);
        dayLabel.setPrefWidth(60);
        dayLabel.setMaxWidth(55);

        String loadClass = "load-" + dl.getLevel().getCode();

        // Numbers (e.g. 3/9 h)
        Label countLabel = new Label(dl.getBookedHours() + "/" + dl.getTotalHours() + "h");
        countLabel.getStyleClass().add("kanban-week-count-compact");
        countLabel.setMinWidth(50);
        countLabel.setPrefWidth(50);
        countLabel.setMaxWidth(45);
        // 9 hour boxes (07:00 to 16:00) where booked hours are marked with the load colour
        HBox hourBoxes = new HBox(2);
        hourBoxes.getStyleClass().add("kanban-hour-boxes");
        hourBoxes.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hourBoxes, Priority.ALWAYS);

        for (TimeSlot slot : dl.getSlots()) {
            StackPane box = new StackPane();
            box.getStyleClass().add("kanban-hour-box");
            // The boxes share the width of the row instead of sitting in a narrow column of their own.
            HBox.setHgrow(box, Priority.ALWAYS);
            box.setMaxWidth(Double.MAX_VALUE);

            String timeTooltip = String.format("%02d:00 - %02d:00", slot.getHour(), slot.getHour() + 1);
            if (slot.isBooked()) {
                box.getStyleClass().addAll("booked", loadClass);
                box.setCursor(Cursor.HAND);
                if (card.isExpanded(slot)) {
                    box.setStyle("-fx-border-color: #ffffff; -fx-border-width: 1.5px; -fx-border-radius: 2px;");
                }
                String desc = slot.getDescription() != null ? SeedText.resolve(slot.getDescription()) : "";
                String reg = slot.getVehicleReg() != null ? " (" + slot.getVehicleReg() + ")" : "";
                String clickHint = " · " + I18n.get("kanban.slot.click_to_expand");
                javafx.scene.control.Tooltip.install(box, new javafx.scene.control.Tooltip(
                        timeTooltip + ": " + I18n.get("kanban.slot.booked") + reg + " " + desc + clickHint));
                box.setOnMouseClicked(e -> {
                    e.consume();
                    card.toggleExpandSlot(slot);
                });
            } else {
                box.getStyleClass().add("free");
                javafx.scene.control.Tooltip.install(box, new javafx.scene.control.Tooltip(timeTooltip + ": " + I18n.get("kanban.day.available")));
            }
            hourBoxes.getChildren().add(box);
        }

        HBox row = new HBox(6, dayLabel, hourBoxes, countLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kanban-week-row-compact");
        row.setCursor(Cursor.HAND);

        // A click opens the day view for the chosen day
        row.setOnMouseClicked(e -> {
            card.selectedDate = dl.getDate();
            card.currentMode = MechanicKanbanCard.KanbanViewMode.DAY;
            card.render();
        });

        VBox dayItem = new VBox(4);
        dayItem.getChildren().add(row);

        // If a time slot this day is expanded, show the information unfolded right below the day.
        if (card.expandedSlot != null && card.expandedSlot.getDate().equals(dl.getDate())) {
            dayItem.getChildren().add(card.slotDrawer.build(mech, card.expandedSlot));
        }

        return dayItem;
    }

        VBox build(Mechanic mech) {
        LocalDate monday = card.selectedDate.with(DayOfWeek.MONDAY);

        Button prevWeekBtn = new Button("<");
        prevWeekBtn.getStyleClass().addAll("ghost", "small");
        prevWeekBtn.setOnAction(e -> {
            card.selectedDate = card.selectedDate.minusWeeks(1);
            card.renderBody();
        });

        Button todayBtn = new Button(I18n.get("kanban.nav.today"));
        todayBtn.getStyleClass().addAll("secondary", "small");
        todayBtn.setOnAction(e -> {
            card.selectedDate = LocalDate.now();
            card.renderBody();
        });

        Button nextWeekBtn = new Button(">");
        nextWeekBtn.getStyleClass().addAll("ghost", "small");
        nextWeekBtn.setOnAction(e -> {
            card.selectedDate = card.selectedDate.plusWeeks(1);
            card.renderBody();
        });

        Label weekTitle = new Label(I18n.get("kanban.week.title", monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
        weekTitle.getStyleClass().add("kanban-date-title-compact");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox navBar = new HBox(4, prevWeekBtn, todayBtn, nextWeekBtn, spr, weekTitle);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 8, 0));

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<DayLoad> weekLoads = schedule.getWeekLoads(mech.getId(), monday);

        VBox daysList = new VBox(4);
        for (DayLoad dl : weekLoads) {
            daysList.getChildren().add(dayRow(mech, dl));
        }

        return new VBox(4, navBar, daysList);
    }

}
