package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.model.MonthDayStatus;
import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** En del av mekanikerkortet. */
class KanbanMonthView {

    private final MechanicKanbanCard card;

    KanbanMonthView(MechanicKanbanCard card) {
        this.card = card;
    }

        StackPane cell(MonthDayStatus s) {
        StackPane cell = new StackPane();
        cell.setMinSize(30, 24);
        cell.setPrefSize(30, 24);
        cell.setMaxSize(30, 24);
        cell.getStyleClass().add("kanban-month-cell-compact");

        Label num = new Label(String.valueOf(s.getDate().getDayOfMonth()));
        num.getStyleClass().add("kanban-month-num-compact");
        cell.getChildren().add(num);

        if (!s.isInCurrentMonth()) {
            cell.getStyleClass().add("out-of-month");
        } else if (s.isWeekend()) {
            cell.getStyleClass().add("weekend");
        } else if (!s.isMechanicAvailable()) {
            cell.getStyleClass().add("unavailable");
        } else {
            String loadClass = "load-" + s.getLevel().getCode();
            cell.getStyleClass().addAll("workday", loadClass);

            String tip = s.getDate().toString() + " · " + s.getBookedHours() + "/9h (" + I18n.get("kanban.load." + s.getLevel().getCode()) + ")";
            javafx.scene.control.Tooltip.install(cell, new javafx.scene.control.Tooltip(tip));
        }

        if (s.isInCurrentMonth() && !s.isWeekend()) {
            cell.setCursor(Cursor.HAND);
            cell.setOnMouseClicked(e -> {
                card.selectedDate = s.getDate();
                card.currentMode = MechanicKanbanCard.KanbanViewMode.DAY;
                card.render();
            });
        }

        return cell;
    }


        VBox build(Mechanic mech) {
        YearMonth ym = YearMonth.from(card.selectedDate);

        Button prevMonthBtn = new Button("<");
        prevMonthBtn.getStyleClass().addAll("ghost", "small");
        prevMonthBtn.setOnAction(e -> {
            card.selectedDate = card.selectedDate.minusMonths(1);
            card.renderBody();
        });

        Button todayBtn = new Button(I18n.get("kanban.today"));
        todayBtn.getStyleClass().addAll("ghost", "small");
        todayBtn.setOnAction(e -> {
            card.selectedDate = LocalDate.now();
            card.renderBody();
        });

        Button nextMonthBtn = new Button(">");
        nextMonthBtn.getStyleClass().addAll("ghost", "small");
        nextMonthBtn.setOnAction(e -> {
            card.selectedDate = card.selectedDate.plusMonths(1);
            card.renderBody();
        });

        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String monthName = ym.getMonth().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        Label monthTitle = new Label(monthName + " " + ym.getYear());
        monthTitle.getStyleClass().add("kanban-date-title-compact");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox navBar = new HBox(4, prevMonthBtn, todayBtn, nextMonthBtn, spr, monthTitle);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 8, 0));

        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        grid.setAlignment(Pos.CENTER);

        String[] headers = I18n.isSwedish() ?
                new String[]{"M", "T", "O", "T", "F", "L", "S"} :
                new String[]{"M", "T", "W", "T", "F", "S", "S"};

        for (int c = 0; c < 7; c++) {
            Label head = new Label(headers[c]);
            head.getStyleClass().add("kanban-month-head-compact");
            head.setAlignment(Pos.CENTER);
            head.setMinWidth(30);
            head.setPrefWidth(30);
            head.setMaxWidth(30);
            grid.add(head, c, 0);
        }

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<MonthDayStatus> days = schedule.getMonthDays(mech.getId(), ym, mech.isAvailable());

        int col = 0;
        int row = 1;
        for (MonthDayStatus s : days) {
            StackPane cell = cell(s);
            grid.add(cell, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        return new VBox(4, navBar, grid);
    }

}
