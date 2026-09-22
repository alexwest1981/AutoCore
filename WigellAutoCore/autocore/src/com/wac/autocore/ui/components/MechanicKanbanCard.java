package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.service.MechanicSchedule.DayLoad;
import com.wac.autocore.service.MechanicSchedule.LoadLevel;
import com.wac.autocore.service.MechanicSchedule.MonthDayStatus;
import com.wac.autocore.service.MechanicSchedule.TimeSlot;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Interaktivt Kanban-kort för verkstadens mekaniker på Dashboard/Översikten.
 * - Navigering mellan mekaniker via vänster/höger-pilar (< och >).
 * - Tre vyer på samma kort: Dag (07:00-16:00 med bokningsknapp),
 *   Vecka (beläggningsgrad Grön->Gul->Orange->Röd) och Månad (tillgänglighetskalender).
 */
public class MechanicKanbanCard {

    public enum KanbanViewMode {
        DAY, WEEK, MONTH
    }

    private final GarageSystem garage;
    private final Runnable onRefresh;
    private final VBox cardContainer;

    private int activeMechanicIndex = 0;
    private KanbanViewMode currentMode = KanbanViewMode.DAY;
    private LocalDate selectedDate = LocalDate.now();

    // UI-referenser för dynamisk omritning
    private final VBox bodyContent;
    private final HBox headerLeft;
    private final HBox headerRight;

    public MechanicKanbanCard(GarageSystem garage, Runnable onRefresh) {
        this.garage = garage;
        this.onRefresh = onRefresh;

        this.cardContainer = new VBox(12);
        this.cardContainer.getStyleClass().addAll("panel", "kanban-card");

        this.headerLeft = new HBox(10);
        this.headerLeft.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(this.headerLeft, Priority.ALWAYS);

        this.headerRight = new HBox(8);
        this.headerRight.setAlignment(Pos.CENTER_RIGHT);

        HBox topBar = new HBox(12, headerLeft, headerRight);
        topBar.setAlignment(Pos.CENTER_LEFT);

        this.bodyContent = new VBox(10);

        this.cardContainer.getChildren().addAll(topBar, bodyContent);

        I18n.addListener(lang -> render());
        render();
    }

    public VBox getView() {
        return cardContainer;
    }

    public void render() {
        renderHeader();
        renderBody();
    }

    private Mechanic getActiveMechanic() {
        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) return null;
        if (activeMechanicIndex < 0) activeMechanicIndex = 0;
        if (activeMechanicIndex >= mechanics.size()) activeMechanicIndex = mechanics.size() - 1;
        return mechanics.get(activeMechanicIndex);
    }

    private void renderHeader() {
        headerLeft.getChildren().clear();
        headerRight.getChildren().clear();

        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) {
            headerLeft.getChildren().add(new Label("Inga mekaniker registrerade"));
            return;
        }

        Mechanic mech = getActiveMechanic();

        // Vänster och höger pilar för mekanikerbyte
        Button prevMechBtn = new Button("❮");
        prevMechBtn.getStyleClass().addAll("ghost", "kanban-nav-arrow");
        prevMechBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("kanban.nav.prev")));
        prevMechBtn.setDisable(mechanics.size() <= 1);
        prevMechBtn.setOnAction(e -> {
            activeMechanicIndex = (activeMechanicIndex - 1 + mechanics.size()) % mechanics.size();
            render();
        });

        Button nextMechBtn = new Button("❯");
        nextMechBtn.getStyleClass().addAll("ghost", "kanban-nav-arrow");
        nextMechBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("kanban.nav.next")));
        nextMechBtn.setDisable(mechanics.size() <= 1);
        nextMechBtn.setOnAction(e -> {
            activeMechanicIndex = (activeMechanicIndex + 1) % mechanics.size();
            render();
        });

        // Mekaniker-avatar med initialer
        String initials = getInitials(mech.getName());
        StackPane avatar = new StackPane(new Label(initials));
        avatar.getStyleClass().add("kanban-avatar");
        avatar.setPrefSize(38, 38);

        // Mekaniker-info
        Label nameLabel = new Label(mech.getName());
        nameLabel.getStyleClass().add("kanban-mech-name");

        Label specLabel = new Label(mech.getSpecialization() + " · " + mech.getPhone());
        specLabel.getStyleClass().add("kanban-mech-sub");

        VBox mechText = new VBox(2, nameLabel, specLabel);

        // Tillgänglighetsbadge
        Label statusBadge = new Label(mech.isAvailable() ? I18n.get("table.col.available") : "Upptagen");
        statusBadge.getStyleClass().addAll("badge", mech.isAvailable() ? "green" : "yellow");

        // Räknare t.ex. "1 / 3"
        Label counterBadge = new Label((activeMechanicIndex + 1) + " / " + mechanics.size());
        counterBadge.getStyleClass().addAll("badge", "blue");

        headerLeft.getChildren().addAll(prevMechBtn, avatar, mechText, statusBadge, counterBadge, nextMechBtn);

        // Höger: Segmenterad vy-växlare [ Dag | Vecka | Månad ]
        Button dayBtn = createViewButton(I18n.get("kanban.view.day"), KanbanViewMode.DAY);
        Button weekBtn = createViewButton(I18n.get("kanban.view.week"), KanbanViewMode.WEEK);
        Button monthBtn = createViewButton(I18n.get("kanban.view.month"), KanbanViewMode.MONTH);

        HBox viewToggleGroup = new HBox(2, dayBtn, weekBtn, monthBtn);
        viewToggleGroup.getStyleClass().add("kanban-toggle-group");

        headerRight.getChildren().add(viewToggleGroup);
    }

    private Button createViewButton(String label, KanbanViewMode mode) {
        Button btn = new Button(label);
        btn.getStyleClass().add("kanban-toggle-btn");
        if (this.currentMode == mode) {
            btn.getStyleClass().add("active");
        }
        btn.setOnAction(e -> {
            this.currentMode = mode;
            render();
        });
        return btn;
    }

    private void renderBody() {
        bodyContent.getChildren().clear();
        Mechanic mech = getActiveMechanic();
        if (mech == null) {
            bodyContent.getChildren().add(new Label("Ingen mekaniker vald."));
            return;
        }

        switch (currentMode) {
            case DAY:
                bodyContent.getChildren().add(buildDayView(mech));
                break;
            case WEEK:
                bodyContent.getChildren().add(buildWeekView(mech));
                break;
            case MONTH:
                bodyContent.getChildren().add(buildMonthView(mech));
                break;
        }
    }

    // =========================================================================
    // 1. DAGSVY (07:00 - 16:00)
    // =========================================================================
    private VBox buildDayView(Mechanic mech) {
        // Datumkontroller
        Button prevDayBtn = new Button("❮");
        prevDayBtn.getStyleClass().addAll("ghost", "small");
        prevDayBtn.setOnAction(e -> {
            selectedDate = selectedDate.minusDays(1);
            renderBody();
        });

        Button todayBtn = new Button(I18n.get("kanban.today"));
        todayBtn.getStyleClass().addAll("ghost", "small");
        todayBtn.setOnAction(e -> {
            selectedDate = LocalDate.now();
            renderBody();
        });

        Button nextDayBtn = new Button("❯");
        nextDayBtn.getStyleClass().addAll("ghost", "small");
        nextDayBtn.setOnAction(e -> {
            selectedDate = selectedDate.plusDays(1);
            renderBody();
        });

        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String dayName = selectedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        dayName = dayName.substring(0, 1).toUpperCase(locale) + dayName.substring(1);
        String formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));

        Label dateTitle = new Label(dayName + " · " + formattedDate);
        dateTitle.getStyleClass().add("kanban-date-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label workdayInfo = new Label(I18n.get("kanban.day.workday"));
        workdayInfo.getStyleClass().add("kanban-workday-sub");

        HBox dateBar = new HBox(8, prevDayBtn, todayBtn, nextDayBtn, dateTitle, spacer, workdayInfo);
        dateBar.setAlignment(Pos.CENTER_LEFT);
        dateBar.setPadding(new Insets(2, 0, 8, 0));

        // Tidsslottar (07:00 - 16:00)
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<TimeSlot> slots = schedule.getSlotsForDay(mech.getId(), selectedDate);

        VBox slotList = new VBox(6);
        for (TimeSlot slot : slots) {
            slotList.getChildren().add(buildTimeSlotRow(mech, slot));
        }

        return new VBox(6, dateBar, slotList);
    }

    private HBox buildTimeSlotRow(Mechanic mech, TimeSlot slot) {
        Label timeBadge = new Label(slot.getTimeRange());
        timeBadge.getStyleClass().add("kanban-time-badge");
        timeBadge.setPrefWidth(105);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kanban-slot-row");

        if (slot.isBooked()) {
            row.getStyleClass().add("booked");

            Label regBadge = new Label(slot.getVehicleReg() != null ? slot.getVehicleReg() : "Fordon");
            regBadge.getStyleClass().addAll("badge", "blue");

            Label custLabel = new Label(slot.getCustomerName() != null ? slot.getCustomerName() : "Kund");
            custLabel.getStyleClass().add("kanban-slot-customer");

            Label descLabel = new Label("· " + (slot.getDescription() != null ? slot.getDescription() : "Bokad service"));
            descLabel.getStyleClass().add("kanban-slot-desc");

            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);

            Label bookedBadge = new Label(I18n.get("kanban.slot.booked"));
            bookedBadge.getStyleClass().addAll("badge", "yellow");

            row.getChildren().addAll(timeBadge, regBadge, custLabel, descLabel, spr, bookedBadge);
        } else {
            row.getStyleClass().add("free");

            Label freeLabel = new Label(I18n.get("kanban.day.available"));
            freeLabel.getStyleClass().add("kanban-slot-free-text");

            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);

            Button bookBtn = new Button(I18n.get("kanban.day.book_button"));
            bookBtn.getStyleClass().addAll("primary", "small");
            bookBtn.setOnAction(e -> {
                ActionDialogs.showCreateBookingDialog(garage, selectedDate, mech, slot.getHour(), () -> {
                    if (onRefresh != null) onRefresh.run();
                    render();
                });
            });

            row.getChildren().addAll(timeBadge, freeLabel, spr, bookBtn);
        }

        return row;
    }

    // =========================================================================
    // 2. VECKOVY (Beläggning Grön -> Gul -> Orange -> Röd)
    // =========================================================================
    private VBox buildWeekView(Mechanic mech) {
        LocalDate monday = selectedDate.with(DayOfWeek.MONDAY);

        Button prevWeekBtn = new Button("❮");
        prevWeekBtn.getStyleClass().addAll("ghost", "small");
        prevWeekBtn.setOnAction(e -> {
            selectedDate = selectedDate.minusWeeks(1);
            renderBody();
        });

        Button thisWeekBtn = new Button(I18n.get("kanban.today"));
        thisWeekBtn.getStyleClass().addAll("ghost", "small");
        thisWeekBtn.setOnAction(e -> {
            selectedDate = LocalDate.now();
            renderBody();
        });

        Button nextWeekBtn = new Button("❯");
        nextWeekBtn.getStyleClass().addAll("ghost", "small");
        nextWeekBtn.setOnAction(e -> {
            selectedDate = selectedDate.plusWeeks(1);
            renderBody();
        });

        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        LocalDate sunday = monday.plusDays(6);
        String weekRange = monday.format(DateTimeFormatter.ofPattern("d MMM", locale)) + " – " +
                sunday.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));

        Label weekTitle = new Label(I18n.get("kanban.week.title", monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)) + " (" + weekRange + ")");
        weekTitle.getStyleClass().add("kanban-date-title");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        // Förklaring/Legend
        HBox legend = buildLoadLegend();

        HBox topBar = new HBox(8, prevWeekBtn, thisWeekBtn, nextWeekBtn, weekTitle, spr, legend);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2, 0, 10, 0));

        // 7 dagars kolumner
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<DayLoad> weekLoads = schedule.getWeekLoads(mech.getId(), monday);

        HBox daysRow = new HBox(10);
        daysRow.setAlignment(Pos.CENTER);
        for (DayLoad dl : weekLoads) {
            VBox dayCol = buildWeekDayColumn(mech, dl);
            HBox.setHgrow(dayCol, Priority.ALWAYS);
            daysRow.getChildren().add(dayCol);
        }

        return new VBox(8, topBar, daysRow);
    }

    private VBox buildWeekDayColumn(Mechanic mech, DayLoad dl) {
        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String dayName = dl.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        String dayNum = String.valueOf(dl.getDate().getDayOfMonth());

        Label dayHeader = new Label(dayName + " " + dayNum);
        dayHeader.getStyleClass().add("kanban-week-day-title");

        // Färgkod baserad på beläggning
        String loadClass = "load-" + dl.getLevel().getCode();
        Label loadBadge = new Label(I18n.get("kanban.load." + dl.getLevel().getCode()));
        loadBadge.getStyleClass().addAll("kanban-load-pill", loadClass);

        // Progress bar för timmar (0/9 till 9/9)
        int booked = dl.getBookedHours();
        int total = dl.getTotalHours();
        int pct = (int) Math.round(dl.getLoadPercentage() * 100);

        Label hoursLabel = new Label(I18n.get("kanban.week.hours_booked", booked, total, pct));
        hoursLabel.getStyleClass().add("kanban-week-hours");

        StackPane track = new StackPane();
        track.getStyleClass().add("kanban-progress-track");
        track.setPrefHeight(6);

        Region fill = new Region();
        fill.getStyleClass().addAll("kanban-progress-fill", loadClass);
        fill.setMaxHeight(6);
        fill.setPrefHeight(6);
        fill.setPrefWidth(Math.max(4, 120 * dl.getLoadPercentage()));

        HBox fillBox = new HBox(fill);
        fillBox.setAlignment(Pos.CENTER_LEFT);
        track.getChildren().add(fillBox);

        // Lista med bokningar
        VBox bookingSnippets = new VBox(4);
        bookingSnippets.setPadding(new Insets(6, 0, 0, 0));
        int snippetCount = 0;
        for (TimeSlot s : dl.getSlots()) {
            if (s.isBooked()) {
                snippetCount++;
                if (snippetCount <= 3) {
                    Label item = new Label(String.format("%02d:00 ", s.getHour()) + s.getVehicleReg() + " " + s.getDescription());
                    item.getStyleClass().add("kanban-week-snippet");
                    bookingSnippets.getChildren().add(item);
                }
            }
        }
        if (snippetCount > 3) {
            Label more = new Label("+ " + (snippetCount - 3) + " till...");
            more.getStyleClass().addAll("kanban-week-snippet", "muted");
            bookingSnippets.getChildren().add(more);
        } else if (snippetCount == 0) {
            Label free = new Label(I18n.get("kanban.day.empty_notice"));
            free.getStyleClass().addAll("kanban-week-snippet", "muted");
            bookingSnippets.getChildren().add(free);
        }

        VBox col = new VBox(6, dayHeader, loadBadge, hoursLabel, track, bookingSnippets);
        col.getStyleClass().addAll("kanban-week-col", loadClass);
        col.setPadding(new Insets(10));
        col.setCursor(Cursor.HAND);

        // Klick på dagen byter direkt till dagsvy för det valda datumet
        col.setOnMouseClicked(e -> {
            this.selectedDate = dl.getDate();
            this.currentMode = KanbanViewMode.DAY;
            render();
        });

        return col;
    }

    private HBox buildLoadLegend() {
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_RIGHT);

        legend.getChildren().addAll(
                createLegendDot("load-free", I18n.get("kanban.load.free")),
                createLegendDot("load-moderate", I18n.get("kanban.load.moderate")),
                createLegendDot("load-busy", I18n.get("kanban.load.busy")),
                createLegendDot("load-full", I18n.get("kanban.load.full"))
        );
        return legend;
    }

    private HBox createLegendDot(String cssClass, String labelText) {
        Region dot = new Region();
        dot.getStyleClass().addAll("kanban-legend-dot", cssClass);
        dot.setPrefSize(8, 8);
        dot.setMaxSize(8, 8);

        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("kanban-legend-text");

        HBox box = new HBox(4, dot, lbl);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // =========================================================================
    // 3. MÅNADSVY (Kalender med tillgängliga vs otillgängliga dagar)
    // =========================================================================
    private VBox buildMonthView(Mechanic mech) {
        YearMonth ym = YearMonth.from(selectedDate);

        Button prevMonthBtn = new Button("❮");
        prevMonthBtn.getStyleClass().addAll("ghost", "small");
        prevMonthBtn.setOnAction(e -> {
            selectedDate = selectedDate.minusMonths(1);
            renderBody();
        });

        Button thisMonthBtn = new Button(I18n.get("kanban.today"));
        thisMonthBtn.getStyleClass().addAll("ghost", "small");
        thisMonthBtn.setOnAction(e -> {
            selectedDate = LocalDate.now();
            renderBody();
        });

        Button nextMonthBtn = new Button("❯");
        nextMonthBtn.getStyleClass().addAll("ghost", "small");
        nextMonthBtn.setOnAction(e -> {
            selectedDate = selectedDate.plusMonths(1);
            renderBody();
        });

        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, locale);
        monthName = monthName.substring(0, 1).toUpperCase(locale) + monthName.substring(1);

        Label monthTitle = new Label(monthName + " " + ym.getYear());
        monthTitle.getStyleClass().add("kanban-date-title");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        Label hint = new Label(I18n.get("kanban.month.hint"));
        hint.getStyleClass().add("kanban-workday-sub");

        HBox topBar = new HBox(8, prevMonthBtn, thisMonthBtn, nextMonthBtn, monthTitle, spr, hint);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(2, 0, 10, 0));

        // Kalendergitter
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setAlignment(Pos.CENTER);

        // Kolumnrubriker (Mån-Sön)
        String[] daysOfWeek = I18n.isSwedish() ?
                new String[]{"Mån", "Tis", "Ons", "Tor", "Fre", "Lör", "Sön"} :
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int c = 0; c < 7; c++) {
            Label colHead = new Label(daysOfWeek[c]);
            colHead.getStyleClass().add("kanban-month-head");
            colHead.setAlignment(Pos.CENTER);
            colHead.setMaxWidth(Double.MAX_VALUE);
            grid.add(colHead, c, 0);
        }

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<MonthDayStatus> monthStatuses = schedule.getMonthDays(mech.getId(), ym, mech.isAvailable());

        int colIdx = 0;
        int rowIdx = 1;
        for (MonthDayStatus s : monthStatuses) {
            VBox dayBox = buildMonthDayBox(mech, s);
            grid.add(dayBox, colIdx, rowIdx);

            colIdx++;
            if (colIdx == 7) {
                colIdx = 0;
                rowIdx++;
            }
        }

        return new VBox(8, topBar, grid);
    }

    private VBox buildMonthDayBox(Mechanic mech, MonthDayStatus s) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(78, 56);
        box.setMinSize(64, 48);
        box.getStyleClass().add("kanban-month-cell");

        Label dayNum = new Label(String.valueOf(s.getDate().getDayOfMonth()));
        dayNum.getStyleClass().add("kanban-month-num");

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("kanban-month-status");

        if (!s.isInCurrentMonth()) {
            box.getStyleClass().add("out-of-month");
            statusLabel.setText("");
        } else if (s.isWeekend()) {
            box.getStyleClass().add("weekend");
            statusLabel.setText(I18n.get("kanban.month.weekend"));
        } else if (!s.isMechanicAvailable()) {
            box.getStyleClass().add("unavailable");
            statusLabel.setText(I18n.get("kanban.month.unavailable_day"));
        } else if (s.isFullyBooked()) {
            box.getStyleClass().add("full");
            statusLabel.setText(I18n.get("kanban.month.full_day"));
        } else {
            box.getStyleClass().add("available");
            if (s.getBookedHours() > 0) {
                statusLabel.setText(s.getBookedHours() + "h bokad");
            } else {
                statusLabel.setText(I18n.get("kanban.month.available_day"));
            }
        }

        box.getChildren().addAll(dayNum, statusLabel);

        if (s.isInCurrentMonth() && !s.isWeekend()) {
            box.setCursor(Cursor.HAND);
            box.setOnMouseClicked(e -> {
                this.selectedDate = s.getDate();
                this.currentMode = KanbanViewMode.DAY;
                render();
            });
        }

        return box;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
