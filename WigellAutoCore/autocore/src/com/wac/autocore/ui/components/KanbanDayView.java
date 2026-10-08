package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import com.wac.autocore.seed.SeedText;

/** One part of the mechanic card. */
class KanbanDayView {

    private final MechanicKanbanCard card;

    KanbanDayView(MechanicKanbanCard card) {
        this.card = card;
    }

        Node slotRow(Mechanic mech, TimeSlot slot) {
        Label timeBadge = new Label(String.format("%02d:00", slot.getHour()));
        timeBadge.getStyleClass().add("kanban-time-badge-compact");
        timeBadge.setMinWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kanban-slot-row-compact");

        if (slot.isBooked()) {
            row.getStyleClass().add("booked");
            row.setCursor(Cursor.HAND);
            if (card.isExpanded(slot)) {
                row.setStyle("-fx-border-color: -wac-accent; -fx-border-width: 1px; -fx-border-radius: 4px;");
            }
            int orderOrBookingId = slot.getWorkOrderId() > 0 ? slot.getWorkOrderId() : slot.getBookingId();
            javafx.scene.control.Tooltip.install(row, new javafx.scene.control.Tooltip(
                    I18n.get("kanban.slot.tooltip", orderOrBookingId)));
            row.setOnMouseClicked(e -> {
                card.toggleExpandSlot(slot);
            });

            Label regBadge = new Label(slot.getVehicleReg() != null ? slot.getVehicleReg() : I18n.get("kanban.slot.booked"));
            regBadge.getStyleClass().addAll("badge", "info", "small");
            regBadge.setMinWidth(Region.USE_PREF_SIZE);

            String desc = slot.getDescription() != null ? SeedText.resolve(slot.getDescription()) : I18n.get("table.col.service");
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("kanban-slot-desc-compact");
            descLabel.setMinWidth(0);
            descLabel.setMaxWidth(Double.MAX_VALUE);
            descLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            descLabel.setEllipsisString("…");
            descLabel.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(descLabel, Priority.ALWAYS);

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #eab308; -fx-font-size: 12px;");
            dot.setMinWidth(Region.USE_PREF_SIZE);

            row.getChildren().addAll(timeBadge, regBadge, descLabel, dot);
        } else {
            row.getStyleClass().add("free");

            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);

            Button bookBtn = new Button();
            bookBtn.getStyleClass().addAll("primary", "kanban-slot-plus-btn");

            javafx.scene.shape.SVGPath plusIcon = new javafx.scene.shape.SVGPath();
            plusIcon.setContent("M 4 0 H 6 V 4 H 10 V 6 H 6 V 10 H 4 V 6 H 0 V 4 H 4 Z");
            plusIcon.setFill(javafx.scene.paint.Color.WHITE);
            plusIcon.getStyleClass().add("kanban-slot-plus-icon");
            bookBtn.setGraphic(plusIcon);
            String bookSlotText = I18n.get("kanban.action.book_hour") + " (" + slot.getHour() + ":00)";
            bookBtn.setTooltip(new javafx.scene.control.Tooltip(bookSlotText));
            bookBtn.setAccessibleRole(javafx.scene.AccessibleRole.BUTTON);
            bookBtn.setAccessibleText(bookSlotText);
            bookBtn.setAccessibleHelp(bookSlotText);
            bookBtn.setOnAction(e -> {
                ActionDialogs.showCreateBookingDialog(card.garage, card.selectedDate, mech, slot.getHour(), () -> {
                    if (card.onRefresh != null) card.onRefresh.run();
                    card.render();
                });
            });

            row.getChildren().addAll(timeBadge, spr, bookBtn);
        }

        VBox slotContainer = new VBox(4);
        slotContainer.getChildren().add(row);

        if (slot.isBooked() && card.isExpanded(slot)) {
            slotContainer.getChildren().add(card.slotDrawer.build(mech, slot));
        }

        return slotContainer;
    }

        VBox build(Mechanic mech) {
        Button prevDayBtn = new Button("<");
        prevDayBtn.getStyleClass().addAll("ghost", "small");
        prevDayBtn.setOnAction(e -> {
            card.selectedDate = card.selectedDate.minusDays(1);
            card.renderBody();
        });

        Button todayBtn = new Button(I18n.get("kanban.today"));
        todayBtn.getStyleClass().addAll("ghost", "small");
        todayBtn.setOnAction(e -> {
            card.selectedDate = LocalDate.now();
            card.renderBody();
        });

        Button nextDayBtn = new Button(">");
        nextDayBtn.getStyleClass().addAll("ghost", "small");
        nextDayBtn.setOnAction(e -> {
            card.selectedDate = card.selectedDate.plusDays(1);
            card.renderBody();
        });

        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        // The full weekday name in the day view's heading. The short form ("LÖ 3 okt") looked
        // cut off above the rows, and the heading has room for the whole name.
        String dayName = card.selectedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        dayName = dayName.substring(0, 1).toUpperCase(locale) + dayName.substring(1);
        String formattedDate = card.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", locale));

        Label dateTitle = new Label(dayName + " " + formattedDate);
        dateTitle.getStyleClass().add("kanban-date-title-compact");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox navBar = new HBox(4, prevDayBtn, todayBtn, nextDayBtn, spacer, dateTitle);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 8, 0));

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<TimeSlot> slots = schedule.getSlotsForDay(mech.getId(), card.selectedDate);

        VBox slotList = new VBox(4);
        int bookedCount = 0;
        for (TimeSlot slot : slots) {
            if (slot.isBooked()) {
                bookedCount++;
            }
            slotList.getChildren().add(slotRow(mech, slot));
        }

        if (bookedCount == 0) {
            LocalDate nextDate = schedule.getNextBookingDate(mech.getId(), card.selectedDate);
            if (nextDate != null) {
                String nextDayStr = nextDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale);
                nextDayStr = nextDayStr.substring(0, 1).toUpperCase(locale) + nextDayStr.substring(1);
                String nextDateFormatted = nextDate.format(DateTimeFormatter.ofPattern("d MMM", locale));
                String fullNextStr = nextDayStr + " " + nextDateFormatted;

                Button jumpBtn = new Button("📅 " + I18n.get("kanban.card.next_booking", fullNextStr));
                jumpBtn.getStyleClass().addAll("ghost", "small");
                jumpBtn.setMinWidth(0);
                jumpBtn.setMaxWidth(Double.MAX_VALUE);
                jumpBtn.setTextOverrun(OverrunStyle.ELLIPSIS);
                jumpBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5px 8px; -fx-text-fill: -wac-accent; -fx-cursor: hand;");
                jumpBtn.setOnAction(e -> {
                    card.selectedDate = nextDate;
                    card.renderBody();
                });
                HBox jumpBox = new HBox(jumpBtn);
                jumpBox.setAlignment(Pos.CENTER);
                HBox.setHgrow(jumpBtn, Priority.ALWAYS);
                jumpBox.setPadding(new Insets(4, 0, 0, 0));
                slotList.getChildren().add(jumpBox);
            }
        }

        return new VBox(6, navBar, slotList);
    }

}
