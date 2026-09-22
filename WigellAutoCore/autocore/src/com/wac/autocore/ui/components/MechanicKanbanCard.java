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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Kompakt Kanban-kort för mekaniker på Dashboard/Översikten.
 * - Tillåter att ALLA mekaniker visas samtidigt sida vid sida i en Kanban-rad.
 * - Varje kort har vänster/höger-pilar för att vid behov byta mekaniker.
 * - 3 kompakta vyer per kort: Dag (7-16 med tidsbokning), Vecka (beläggningsskala Grön-Gul-Orange-Röd)
 *   och Månad (tillgänglighetskalender).
 */
public class MechanicKanbanCard {

    public enum KanbanViewMode {
        DAY, WEEK, MONTH
    }

    /**
     * Bygger hela Kanban-sektionen med alla mekaniker synliga samtidigt sida vid sida.
     */
    public static VBox buildBoard(GarageSystem garage, Runnable onRefresh) {
        List<Mechanic> mechanics = garage.getMechanics();

        // Rubrikrad för hela Kanban-sektionen
        Label title = new Label(I18n.get("kanban.title"));
        title.getStyleClass().add("panel-title");

        Label sub = new Label(I18n.get("kanban.subtitle"));
        sub.getStyleClass().add("panel-sub");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        // Belastningslegend
        HBox legend = buildCompactLegend();

        HBox headLeft = new HBox(8, new VBox(2, title, sub));
        headLeft.setAlignment(Pos.CENTER_LEFT);

        HBox boardHead = new HBox(12, headLeft, spr, legend);
        boardHead.setAlignment(Pos.CENTER_LEFT);
        boardHead.setPadding(new Insets(0, 0, 4, 0));

        // Rad med alla mekanikerkort sida vid sida
        HBox cardsRow = new HBox(12);
        cardsRow.setAlignment(Pos.TOP_LEFT);

        if (mechanics.isEmpty()) {
            cardsRow.getChildren().add(new Label("Inga mekaniker registrerade."));
        } else {
            for (int i = 0; i < mechanics.size(); i++) {
                MechanicKanbanCard card = new MechanicKanbanCard(garage, i, onRefresh);
                VBox cardView = card.getView();
                HBox.setHgrow(cardView, Priority.ALWAYS);
                cardsRow.getChildren().add(cardView);
            }
        }

        VBox board = new VBox(10, boardHead, cardsRow);
        board.getStyleClass().addAll("panel", "kanban-board-panel");
        return board;
    }

    private static HBox buildCompactLegend() {
        HBox legend = new HBox(10);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
                createDot("load-free", I18n.get("kanban.load.free")),
                createDot("load-moderate", I18n.get("kanban.load.moderate")),
                createDot("load-busy", I18n.get("kanban.load.busy")),
                createDot("load-full", I18n.get("kanban.load.full"))
        );
        return legend;
    }

    private static HBox createDot(String cssClass, String label) {
        Region dot = new Region();
        dot.getStyleClass().addAll("kanban-legend-dot", cssClass);
        dot.setPrefSize(8, 8);
        dot.setMaxSize(8, 8);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("kanban-legend-text");

        HBox box = new HBox(4, dot, lbl);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // -------------------------------------------------------------------------
    // Instansfält för ett enskilt kompakt mekanikerkort
    // -------------------------------------------------------------------------
    private final GarageSystem garage;
    private final Runnable onRefresh;
    private final VBox cardContainer;

    private int activeMechanicIndex;
    private KanbanViewMode currentMode = KanbanViewMode.DAY;
    private LocalDate selectedDate = LocalDate.now();

    private final VBox headerBox;
    private final VBox bodyContent;

    public MechanicKanbanCard(GarageSystem garage, Runnable onRefresh) {
        this(garage, 0, onRefresh);
    }

    public MechanicKanbanCard(GarageSystem garage, int initialMechanicIndex, Runnable onRefresh) {
        this.garage = garage;
        this.activeMechanicIndex = initialMechanicIndex;
        this.onRefresh = onRefresh;

        this.cardContainer = new VBox(8);
        this.cardContainer.getStyleClass().addAll("kanban-card", "kanban-card-compact");
        this.cardContainer.setMinWidth(260);

        this.headerBox = new VBox(6);
        this.bodyContent = new VBox(6);

        this.cardContainer.getChildren().addAll(headerBox, bodyContent);

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
        headerBox.getChildren().clear();

        List<Mechanic> mechanics = garage.getMechanics();
        if (mechanics.isEmpty()) {
            headerBox.getChildren().add(new Label("Ingen mekaniker"));
            return;
        }

        Mechanic mech = getActiveMechanic();

        // Rad 1: Pilar för mekanikerbyte + Mekanikernamn + Vyväxlare
        Button prevMechBtn = new Button("❮");
        prevMechBtn.getStyleClass().addAll("ghost", "kanban-nav-arrow-compact");
        prevMechBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("kanban.nav.prev")));
        prevMechBtn.setOnAction(e -> {
            activeMechanicIndex = (activeMechanicIndex - 1 + mechanics.size()) % mechanics.size();
            render();
        });

        Button nextMechBtn = new Button("❯");
        nextMechBtn.getStyleClass().addAll("ghost", "kanban-nav-arrow-compact");
        nextMechBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("kanban.nav.next")));
        nextMechBtn.setOnAction(e -> {
            activeMechanicIndex = (activeMechanicIndex + 1) % mechanics.size();
            render();
        });

        // Avatar
        String initials = getInitials(mech.getName());
        StackPane avatar = new StackPane(new Label(initials));
        avatar.getStyleClass().add("kanban-avatar-compact");
        avatar.setPrefSize(26, 26);
        avatar.setMinSize(26, 26);

        Label nameLabel = new Label(mech.getName());
        nameLabel.getStyleClass().add("kanban-mech-name-compact");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Segmenterad vyväxlare: Dag | Vecka | Månad
        Button dayBtn = createViewButton(I18n.get("kanban.view.day"), KanbanViewMode.DAY);
        Button weekBtn = createViewButton(I18n.get("kanban.view.week"), KanbanViewMode.WEEK);
        Button monthBtn = createViewButton(I18n.get("kanban.view.month"), KanbanViewMode.MONTH);

        HBox toggleGroup = new HBox(2, dayBtn, weekBtn, monthBtn);
        toggleGroup.getStyleClass().add("kanban-toggle-group-compact");

        HBox topRow = new HBox(4, prevMechBtn, avatar, nameLabel, nextMechBtn, spacer, toggleGroup);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Rad 2: Specialisering och tillgänglighetsbadge
        Label specLabel = new Label(mech.getSpecialization());
        specLabel.getStyleClass().add("kanban-mech-sub-compact");

        Region spr2 = new Region();
        HBox.setHgrow(spr2, Priority.ALWAYS);

        Label statusBadge = new Label(mech.isAvailable() ? I18n.get("table.col.available") : "Upptagen");
        statusBadge.getStyleClass().addAll("badge", mech.isAvailable() ? "green" : "yellow", "small");

        HBox subRow = new HBox(6, specLabel, spr2, statusBadge);
        subRow.setAlignment(Pos.CENTER_LEFT);

        headerBox.getChildren().addAll(topRow, subRow);
    }

    private Button createViewButton(String label, KanbanViewMode mode) {
        Button btn = new Button(label);
        btn.getStyleClass().add("kanban-toggle-btn-compact");
        if (this.currentMode == mode) {
            btn.getStyleClass().add("active");
        }
        btn.setOnAction(e -> {
            this.currentMode = mode;
            renderBody();
            renderHeader(); // Uppdatera aktiv klass
        });
        return btn;
    }

    private void renderBody() {
        bodyContent.getChildren().clear();
        Mechanic mech = getActiveMechanic();
        if (mech == null) return;

        switch (currentMode) {
            case DAY:
                bodyContent.getChildren().add(buildCompactDayView(mech));
                break;
            case WEEK:
                bodyContent.getChildren().add(buildCompactWeekView(mech));
                break;
            case MONTH:
                bodyContent.getChildren().add(buildCompactMonthView(mech));
                break;
        }
    }

    // =========================================================================
    // 1. KOMPAKT DAGSVY (07:00 - 16:00)
    // =========================================================================
    private VBox buildCompactDayView(Mechanic mech) {
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
        String dayName = selectedDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        String formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("d MMM", locale));

        Label dateTitle = new Label(dayName + " " + formattedDate);
        dateTitle.getStyleClass().add("kanban-date-title-compact");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox navBar = new HBox(4, prevDayBtn, todayBtn, nextDayBtn, spacer, dateTitle);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 4, 0));

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<TimeSlot> slots = schedule.getSlotsForDay(mech.getId(), selectedDate);

        VBox slotList = new VBox(4);
        for (TimeSlot slot : slots) {
            slotList.getChildren().add(buildCompactTimeSlotRow(mech, slot));
        }

        ScrollPane scroll = new ScrollPane(slotList);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(260);
        scroll.setPrefHeight(260);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        return new VBox(4, navBar, scroll);
    }

    private HBox buildCompactTimeSlotRow(Mechanic mech, TimeSlot slot) {
        Label timeBadge = new Label(String.format("%02d:00", slot.getHour()));
        timeBadge.getStyleClass().add("kanban-time-badge-compact");

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kanban-slot-row-compact");

        if (slot.isBooked()) {
            row.getStyleClass().add("booked");

            Label regBadge = new Label(slot.getVehicleReg() != null ? slot.getVehicleReg() : "Bokad");
            regBadge.getStyleClass().addAll("badge", "blue", "small");

            String desc = slot.getDescription() != null ? slot.getDescription() : "Service";
            if (desc.length() > 18) desc = desc.substring(0, 16) + "…";
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("kanban-slot-desc-compact");

            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #eab308; -fx-font-size: 10px;");

            row.getChildren().addAll(timeBadge, regBadge, descLabel, spr, dot);
        } else {
            row.getStyleClass().add("free");

            Label freeLabel = new Label(I18n.get("kanban.day.available"));
            freeLabel.getStyleClass().add("kanban-slot-free-compact");

            Region spr = new Region();
            HBox.setHgrow(spr, Priority.ALWAYS);

            Button bookBtn = new Button("+");
            bookBtn.getStyleClass().addAll("primary", "small", "kanban-slot-plus-btn");
            bookBtn.setTooltip(new javafx.scene.control.Tooltip(I18n.get("kanban.action.book_hour")));
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
    // 2. KOMPAKT VECKOVY (Beläggningsskala Grön -> Gul -> Orange -> Röd)
    // =========================================================================
    private VBox buildCompactWeekView(Mechanic mech) {
        LocalDate monday = selectedDate.with(DayOfWeek.MONDAY);

        Button prevWeekBtn = new Button("❮");
        prevWeekBtn.getStyleClass().addAll("ghost", "small");
        prevWeekBtn.setOnAction(e -> {
            selectedDate = selectedDate.minusWeeks(1);
            renderBody();
        });

        Button todayBtn = new Button(I18n.get("kanban.today"));
        todayBtn.getStyleClass().addAll("ghost", "small");
        todayBtn.setOnAction(e -> {
            selectedDate = LocalDate.now();
            renderBody();
        });

        Button nextWeekBtn = new Button("❯");
        nextWeekBtn.getStyleClass().addAll("ghost", "small");
        nextWeekBtn.setOnAction(e -> {
            selectedDate = selectedDate.plusWeeks(1);
            renderBody();
        });

        Label weekTitle = new Label(I18n.get("kanban.week.title", monday.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
        weekTitle.getStyleClass().add("kanban-date-title-compact");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox navBar = new HBox(4, prevWeekBtn, todayBtn, nextWeekBtn, spr, weekTitle);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 4, 0));

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<DayLoad> weekLoads = schedule.getWeekLoads(mech.getId(), monday);

        VBox daysList = new VBox(4);
        for (DayLoad dl : weekLoads) {
            daysList.getChildren().add(buildCompactWeekDayRow(mech, dl));
        }

        return new VBox(4, navBar, daysList);
    }

    private HBox buildCompactWeekDayRow(Mechanic mech, DayLoad dl) {
        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String dayName = dl.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        String dateStr = dayName + " " + dl.getDate().getDayOfMonth();

        Label dayLabel = new Label(dateStr);
        dayLabel.getStyleClass().add("kanban-week-day-name");
        dayLabel.setPrefWidth(55);

        // Beläggningspill
        String loadClass = "load-" + dl.getLevel().getCode();
        Label pill = new Label(I18n.get("kanban.load." + dl.getLevel().getCode()));
        pill.getStyleClass().addAll("kanban-load-pill-compact", loadClass);
        pill.setPrefWidth(85);

        // Siffror (t.ex. 3/9 h)
        Label countLabel = new Label(dl.getBookedHours() + "/" + dl.getTotalHours() + "h");
        countLabel.getStyleClass().add("kanban-week-count-compact");
        // 9 timboxar (kl. 07:00 till 16:00) där bokade timmar markeras med belastningsfärg
        HBox hourBoxes = new HBox(2);
        hourBoxes.getStyleClass().add("kanban-hour-boxes");
        hourBoxes.setAlignment(Pos.CENTER_LEFT);

        for (TimeSlot slot : dl.getSlots()) {
            StackPane box = new StackPane();
            box.getStyleClass().add("kanban-hour-box");

            String timeTooltip = String.format("%02d:00 - %02d:00", slot.getHour(), slot.getHour() + 1);
            if (slot.isBooked()) {
                box.getStyleClass().addAll("booked", loadClass);
                String desc = slot.getDescription() != null ? slot.getDescription() : "";
                String reg = slot.getVehicleReg() != null ? " (" + slot.getVehicleReg() + ")" : "";
                javafx.scene.control.Tooltip.install(box, new javafx.scene.control.Tooltip(timeTooltip + ": " + I18n.get("kanban.slot.booked") + reg + " " + desc));
            } else {
                box.getStyleClass().add("free");
                javafx.scene.control.Tooltip.install(box, new javafx.scene.control.Tooltip(timeTooltip + ": " + I18n.get("kanban.day.available")));
            }
            hourBoxes.getChildren().add(box);
        }

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox row = new HBox(6, dayLabel, pill, hourBoxes, spr, countLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kanban-week-row-compact");
        row.setCursor(Cursor.HAND);

        // Klick öppnar dagsvyn för vald dag
        row.setOnMouseClicked(e -> {
            this.selectedDate = dl.getDate();
            this.currentMode = KanbanViewMode.DAY;
            render();
        });

        return row;
    }

    // =========================================================================
    // 3. KOMPAKT MÅNADSVY (Kalenderraster)
    // =========================================================================
    private VBox buildCompactMonthView(Mechanic mech) {
        YearMonth ym = YearMonth.from(selectedDate);

        Button prevMonthBtn = new Button("❮");
        prevMonthBtn.getStyleClass().addAll("ghost", "small");
        prevMonthBtn.setOnAction(e -> {
            selectedDate = selectedDate.minusMonths(1);
            renderBody();
        });

        Button todayBtn = new Button(I18n.get("kanban.today"));
        todayBtn.getStyleClass().addAll("ghost", "small");
        todayBtn.setOnAction(e -> {
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
        String monthName = ym.getMonth().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        Label monthTitle = new Label(monthName + " " + ym.getYear());
        monthTitle.getStyleClass().add("kanban-date-title-compact");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        HBox navBar = new HBox(4, prevMonthBtn, todayBtn, nextMonthBtn, spr, monthTitle);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 4, 0));

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
            head.setPrefWidth(30);
            grid.add(head, c, 0);
        }

        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<MonthDayStatus> days = schedule.getMonthDays(mech.getId(), ym, mech.isAvailable());

        int col = 0;
        int row = 1;
        for (MonthDayStatus s : days) {
            StackPane cell = buildCompactMonthCell(s);
            grid.add(cell, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }

        return new VBox(4, navBar, grid);
    }

    private StackPane buildCompactMonthCell(MonthDayStatus s) {
        StackPane cell = new StackPane();
        cell.setPrefSize(30, 24);
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
                this.selectedDate = s.getDate();
                this.currentMode = KanbanViewMode.DAY;
                render();
            });
        }

        return cell;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
