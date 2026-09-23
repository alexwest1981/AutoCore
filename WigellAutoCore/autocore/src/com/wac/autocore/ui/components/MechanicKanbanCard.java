package com.wac.autocore.ui.components;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.service.MechanicSchedule.DayLoad;
import com.wac.autocore.service.MechanicSchedule.MonthDayStatus;
import com.wac.autocore.service.MechanicSchedule.TimeSlot;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.scene.control.Tooltip;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        return buildBoard(garage, null, onRefresh);
    }

    public static VBox buildBoard(GarageSystem garage, com.wac.autocore.ui.navigation.PageRouter router, Runnable onRefresh) {
        List<Mechanic> mechanics = garage.getMechanics();

        // Rubrikrad för hela Kanban-sektionen
        Label title = new Label(I18n.get("kanban.title"));
        title.getStyleClass().add("panel-title");

        Label sub = new Label(I18n.get("kanban.subtitle"));
        sub.getStyleClass().add("panel-sub");

        Button addMechBtn = new Button("+ " + I18n.get("dialog.mechanic.create.title"));
        addMechBtn.getStyleClass().addAll("secondary", "small");
        addMechBtn.setOnAction(e -> javafx.application.Platform.runLater(() -> ActionDialogs.showCreateMechanicDialog(garage, onRefresh)));

        Region spr1 = new Region();
        HBox.setHgrow(spr1, Priority.ALWAYS);

        // Belastningslegend
        HBox legend = buildCompactLegend();

        HBox headLeft = new HBox(12, new VBox(2, title, sub), addMechBtn);
        headLeft.setAlignment(Pos.CENTER_LEFT);

        HBox boardHead = new HBox(12, headLeft, spr1, legend);
        boardHead.setAlignment(Pos.CENTER_LEFT);
        boardHead.setPadding(new Insets(0, 0, 4, 0));

        // Rad med alla mekanikerkort sida vid sida
        HBox cardsRow = new HBox(14);
        cardsRow.setAlignment(Pos.TOP_LEFT);
        cardsRow.setMinWidth(Region.USE_PREF_SIZE);

        // Horisontell scroll om många mekaniker tillkommer
        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        // Rad 2: Filter-chips & Scroll-kontroller
        HBox filterBox = new HBox(8);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label countLabel = new Label();
        countLabel.getStyleClass().add("kanban-count-label");

        Button scrollLeftBtn = new Button("❮");
        scrollLeftBtn.getStyleClass().addAll("kanban-scroll-btn", "small");
        scrollLeftBtn.setTooltip(new Tooltip(I18n.get("kanban.filter.scroll_prev")));
        scrollLeftBtn.setOnAction(e -> scroll.setHvalue(Math.max(0.0, scroll.getHvalue() - 0.35)));

        Button scrollRightBtn = new Button("❯");
        scrollRightBtn.getStyleClass().addAll("kanban-scroll-btn", "small");
        scrollRightBtn.setTooltip(new Tooltip(I18n.get("kanban.filter.scroll_next")));
        scrollRightBtn.setOnAction(e -> scroll.setHvalue(Math.min(1.0, scroll.getHvalue() + 0.35)));

        HBox scrollControls = new HBox(8, countLabel, scrollLeftBtn, scrollRightBtn);
        scrollControls.setAlignment(Pos.CENTER_RIGHT);

        Region spr2 = new Region();
        HBox.setHgrow(spr2, Priority.ALWAYS);

        HBox controlBar = new HBox(10, filterBox, spr2, scrollControls);
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(4, 0, 6, 0));

        final String[] activeFilter = new String[]{null};
        final List<Button> filterButtons = new ArrayList<Button>();

        Runnable populateCards = () -> {
            cardsRow.getChildren().clear();
            if (mechanics.isEmpty()) {
                cardsRow.getChildren().add(new Label(I18n.get("kanban.empty_mechanics")));
                countLabel.setText(I18n.get("kanban.filter.showing", 0, 0));
                return;
            }

            int count = 0;
            for (int i = 0; i < mechanics.size(); i++) {
                Mechanic m = mechanics.get(i);
                if (activeFilter[0] == null || activeFilter[0].equalsIgnoreCase(m.getSpecialization())) {
                    MechanicKanbanCard card = new MechanicKanbanCard(garage, i, router, onRefresh);
                    VBox cardView = card.getView();
                    cardView.setMinWidth(310);
                    cardView.setPrefWidth(350);
                    cardView.setMaxWidth(480);
                    HBox.setHgrow(cardView, Priority.ALWAYS);
                    cardsRow.getChildren().add(cardView);
                    count++;
                }
            }

            if (count == 0) {
                Label empty = new Label(I18n.get("kanban.filter.empty"));
                empty.setStyle("-fx-text-fill: -wac-muted; -fx-padding: 24 16; -fx-font-style: italic;");
                cardsRow.getChildren().add(empty);
            }

            countLabel.setText(I18n.get("kanban.filter.showing", count, mechanics.size()));
            scroll.setHvalue(0.0);
        };

        // Samla unika specialiseringar och räkna mekaniker per kategori
        Map<String, Integer> specCounts = new LinkedHashMap<String, Integer>();
        for (Mechanic m : mechanics) {
            String spec = m.getSpecialization();
            if (spec != null && !spec.trim().isEmpty()) {
                specCounts.put(spec, specCounts.containsKey(spec) ? specCounts.get(spec) + 1 : 1);
            }
        }

        // Knapp: Alla
        Button allBtn = new Button(I18n.get("kanban.filter.all") + " (" + mechanics.size() + ")");
        allBtn.getStyleClass().addAll("kanban-filter-chip", "active");
        filterButtons.add(allBtn);
        allBtn.setOnAction(e -> {
            activeFilter[0] = null;
            for (Button b : filterButtons) {
                b.getStyleClass().remove("active");
            }
            allBtn.getStyleClass().add("active");
            populateCards.run();
        });
        filterBox.getChildren().add(allBtn);

        // Knappar för varje specialisering
        for (Map.Entry<String, Integer> entry : specCounts.entrySet()) {
            final String specKey = entry.getKey();
            String displaySpec = formatSpecialization(specKey) + " (" + entry.getValue() + ")";
            Button chip = new Button(displaySpec);
            chip.getStyleClass().add("kanban-filter-chip");
            filterButtons.add(chip);
            chip.setOnAction(e -> {
                activeFilter[0] = specKey;
                for (Button b : filterButtons) {
                    b.getStyleClass().remove("active");
                }
                chip.getStyleClass().add("active");
                populateCards.run();
            });
            filterBox.getChildren().add(chip);
        }

        populateCards.run();

        VBox board = new VBox(8, boardHead, controlBar, scroll);
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
    private final com.wac.autocore.ui.navigation.PageRouter router;
    private final Runnable onRefresh;
    private final VBox cardContainer;

    private int activeMechanicIndex;
    private KanbanViewMode currentMode = KanbanViewMode.DAY;
    private LocalDate selectedDate = LocalDate.now();
    private TimeSlot expandedSlot;

    private void toggleExpandSlot(TimeSlot slot) {
        if (expandedSlot != null && expandedSlot.getDate().equals(slot.getDate()) && expandedSlot.getHour() == slot.getHour()) {
            expandedSlot = null;
        } else {
            expandedSlot = slot;
        }
        renderBody();
    }

    private boolean isExpanded(TimeSlot slot) {
        return expandedSlot != null && expandedSlot.getDate().equals(slot.getDate()) && expandedSlot.getHour() == slot.getHour();
    }

    private final VBox headerBox;
    private final VBox bodyContent;

    public MechanicKanbanCard(GarageSystem garage, Runnable onRefresh) {
        this(garage, 0, null, onRefresh);
    }

    public MechanicKanbanCard(GarageSystem garage, int initialMechanicIndex, Runnable onRefresh) {
        this(garage, initialMechanicIndex, null, onRefresh);
    }

    public MechanicKanbanCard(GarageSystem garage, int initialMechanicIndex,
                              com.wac.autocore.ui.navigation.PageRouter router, Runnable onRefresh) {
        this.garage = garage;
        this.activeMechanicIndex = initialMechanicIndex;
        this.router = router;
        this.onRefresh = onRefresh;

        this.cardContainer = new VBox(8);
        this.cardContainer.getStyleClass().addAll("kanban-card", "kanban-card-compact");
        this.cardContainer.setMinWidth(290);
        this.cardContainer.setPrefWidth(350);

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
            headerBox.getChildren().add(new Label(I18n.get("kanban.no_mechanic")));
            return;
        }

        Mechanic mech = getActiveMechanic();

        // Avatar
        String initials = getInitials(mech.getName());
        StackPane avatar = new StackPane(new Label(initials));
        avatar.getStyleClass().add("kanban-avatar-compact");
        avatar.setPrefSize(26, 26);
        avatar.setMinSize(26, 26);
        avatar.setMaxSize(26, 26);

        Label nameLabel = new Label(mech.getName());
        nameLabel.getStyleClass().add("kanban-mech-name-compact");
        nameLabel.setMinWidth(Region.USE_PREF_SIZE);

        HBox mechTitle = new HBox(6, avatar, nameLabel);
        mechTitle.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Segmenterad vyväxlare: Dag | Vecka | Månad
        Button dayBtn = createViewButton(I18n.get("kanban.view.day"), KanbanViewMode.DAY);
        Button weekBtn = createViewButton(I18n.get("kanban.view.week"), KanbanViewMode.WEEK);
        Button monthBtn = createViewButton(I18n.get("kanban.view.month"), KanbanViewMode.MONTH);

        HBox toggleGroup = new HBox(2, dayBtn, weekBtn, monthBtn);
        toggleGroup.getStyleClass().add("kanban-toggle-group-compact");

        HBox topRow = new HBox(8, mechTitle, spacer, toggleGroup);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Rad 2: Specialisering och tillgänglighetsbadge
        Label specLabel = new Label(formatSpecialization(mech.getSpecialization()));
        specLabel.getStyleClass().add("kanban-mech-sub-compact");

        Region spr2 = new Region();
        HBox.setHgrow(spr2, Priority.ALWAYS);

        Label statusBadge = new Label(mech.isAvailable() ? I18n.get("table.col.available") : I18n.get("table.col.unavailable"));
        statusBadge.getStyleClass().addAll("badge", mech.isAvailable() ? "green" : "yellow", "small");

        HBox subRow = new HBox(6, specLabel, spr2, statusBadge);
        subRow.setAlignment(Pos.CENTER_LEFT);

        headerBox.getChildren().addAll(topRow, subRow);
    }

    private static String formatSpecialization(String spec) {
        if (spec == null) return "";
        if (spec.equalsIgnoreCase("General service") || spec.equalsIgnoreCase("Allmän service")) {
            return I18n.get("kanban.specialization.general_service");
        }
        if (spec.equalsIgnoreCase("Brakes") || spec.equalsIgnoreCase("Bromsar")) {
            return I18n.get("kanban.specialization.brakes");
        }
        if (spec.equalsIgnoreCase("Diagnostics") || spec.equalsIgnoreCase("Diagnostik") || spec.equalsIgnoreCase("Diagnostik & Felsökning")) {
            return I18n.get("kanban.specialization.diagnostics");
        }
        return spec;
    }

    private Button createViewButton(String label, KanbanViewMode mode) {
        Button btn = new Button(label);
        btn.getStyleClass().add("kanban-toggle-btn-compact");
        btn.setMinWidth(Region.USE_PREF_SIZE);
        btn.setAlignment(Pos.CENTER);
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

    private Node buildCompactTimeSlotRow(Mechanic mech, TimeSlot slot) {
        Label timeBadge = new Label(String.format("%02d:00", slot.getHour()));
        timeBadge.getStyleClass().add("kanban-time-badge-compact");
        timeBadge.setMinWidth(Region.USE_PREF_SIZE);

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("kanban-slot-row-compact");

        if (slot.isBooked()) {
            row.getStyleClass().add("booked");
            row.setCursor(Cursor.HAND);
            if (isExpanded(slot)) {
                row.setStyle("-fx-border-color: -wac-accent; -fx-border-width: 1px; -fx-border-radius: 4px;");
            }
            int orderOrBookingId = slot.getWorkOrderId() > 0 ? slot.getWorkOrderId() : slot.getBookingId();
            javafx.scene.control.Tooltip.install(row, new javafx.scene.control.Tooltip(
                    I18n.get("kanban.slot.tooltip", orderOrBookingId)));
            row.setOnMouseClicked(e -> {
                toggleExpandSlot(slot);
            });

            Label regBadge = new Label(slot.getVehicleReg() != null ? slot.getVehicleReg() : I18n.get("kanban.slot.booked"));
            regBadge.getStyleClass().addAll("badge", "info", "small");
            regBadge.setMinWidth(Region.USE_PREF_SIZE);

            String desc = slot.getDescription() != null ? slot.getDescription() : I18n.get("table.col.service");
            Label descLabel = new Label(desc);
            descLabel.getStyleClass().add("kanban-slot-desc-compact");
            descLabel.setMinWidth(0);
            descLabel.setMaxWidth(Double.MAX_VALUE);
            descLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            descLabel.setEllipsisString("…");
            descLabel.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(descLabel, Priority.ALWAYS);

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill: #eab308; -fx-font-size: 10px;");
            dot.setMinWidth(Region.USE_PREF_SIZE);

            row.getChildren().addAll(timeBadge, regBadge, descLabel, dot);
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

        VBox slotContainer = new VBox(4);
        slotContainer.getChildren().add(row);

        if (slot.isBooked() && isExpanded(slot)) {
            slotContainer.getChildren().add(buildInlineSlotDrawer(mech, slot));
        }

        return slotContainer;
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

        Button todayBtn = new Button(I18n.get("kanban.nav.today"));
        todayBtn.getStyleClass().addAll("secondary", "small");
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

    private Node buildCompactWeekDayRow(Mechanic mech, DayLoad dl) {
        Locale locale = I18n.isSwedish() ? new Locale("sv", "SE") : Locale.ENGLISH;
        String dayName = dl.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, locale).toUpperCase(locale);
        String dateStr = dayName + " " + dl.getDate().getDayOfMonth();

        Label dayLabel = new Label(dateStr);
        dayLabel.getStyleClass().add("kanban-week-day-name");
        dayLabel.setMinWidth(55);
        dayLabel.setPrefWidth(55);
        dayLabel.setMaxWidth(55);

        // Beläggningspill
        String loadClass = "load-" + dl.getLevel().getCode();
        Label pill = new Label(I18n.get("kanban.load." + dl.getLevel().getCode()));
        pill.getStyleClass().addAll("kanban-load-pill-compact", loadClass);
        pill.setMinWidth(85);
        pill.setPrefWidth(85);
        pill.setMaxWidth(85);
        pill.setAlignment(Pos.CENTER);

        // Siffror (t.ex. 3/9 h)
        Label countLabel = new Label(dl.getBookedHours() + "/" + dl.getTotalHours() + "h");
        countLabel.getStyleClass().add("kanban-week-count-compact");
        countLabel.setMinWidth(45);
        countLabel.setPrefWidth(45);
        countLabel.setMaxWidth(45);
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
                box.setCursor(Cursor.HAND);
                if (isExpanded(slot)) {
                    box.setStyle("-fx-border-color: #ffffff; -fx-border-width: 1.5px; -fx-border-radius: 2px;");
                }
                String desc = slot.getDescription() != null ? slot.getDescription() : "";
                String reg = slot.getVehicleReg() != null ? " (" + slot.getVehicleReg() + ")" : "";
                String clickHint = " · " + I18n.get("kanban.slot.click_to_expand");
                javafx.scene.control.Tooltip.install(box, new javafx.scene.control.Tooltip(
                        timeTooltip + ": " + I18n.get("kanban.slot.booked") + reg + " " + desc + clickHint));
                box.setOnMouseClicked(e -> {
                    e.consume();
                    toggleExpandSlot(slot);
                });
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

        VBox dayItem = new VBox(4);
        dayItem.getChildren().add(row);

        // Om en tidsslott denna dag är utfälld – visa informationen direkt nedvikt under dagen!
        if (expandedSlot != null && expandedSlot.getDate().equals(dl.getDate())) {
            dayItem.getChildren().add(buildInlineSlotDrawer(mech, expandedSlot));
        }

        return dayItem;
    }

    private VBox buildInlineSlotDrawer(Mechanic mech, TimeSlot slot) {
        WorkOrder targetOrder = null;
        if (slot.getWorkOrderId() > 0) {
            for (WorkOrder wo : garage.getWorkOrders()) {
                if (wo.getId() == slot.getWorkOrderId()) {
                    targetOrder = wo;
                    break;
                }
            }
        }
        if (targetOrder == null && slot.getBookingId() > 0) {
            for (WorkOrder wo : garage.getWorkOrders()) {
                if (wo.getBookingId() == slot.getBookingId()) {
                    targetOrder = wo;
                    slot.setWorkOrderId(wo.getId());
                    break;
                }
            }
        }

        Booking booking = null;
        if (targetOrder != null) {
            for (Booking bk : garage.getBookings()) {
                if (bk.getId() == targetOrder.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }
        if (booking == null && slot.getBookingId() > 0) {
            for (Booking bk : garage.getBookings()) {
                if (bk.getId() == slot.getBookingId()) {
                    booking = bk;
                    break;
                }
            }
        }

        final WorkOrder wo = targetOrder;
        final Booking b = booking;

        VBox drawer = new VBox(6);
        drawer.getStyleClass().add("kanban-inline-drawer");
        drawer.setStyle("-fx-background-color: -wac-card; " +
                        "-fx-background-radius: 6px; " +
                        "-fx-border-color: -wac-line -wac-line -wac-line -wac-accent; -fx-border-width: 1px 1px 1px 4px; " +
                        "-fx-border-radius: 6px; -fx-padding: 8px 12px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.08), 6, 0, 0, 2);");

        // Top bar: Header & Stängknapp
        String title = (wo != null)
                ? I18n.get("kanban.drawer.work_order", wo.getId())
                : I18n.get("kanban.drawer.booked");
        Label titleLbl = new Label(slot.getTimeRange() + " · " + title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -wac-accent; -fx-font-size: 11px;");

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -wac-muted; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4 0 4;");
        closeBtn.setOnAction(e -> {
            expandedSlot = null;
            renderBody();
        });

        HBox head = new HBox(6, titleLbl, spr, closeBtn);
        head.setAlignment(Pos.CENTER_LEFT);

        // Details
        String reg = slot.getVehicleReg() != null && !slot.getVehicleReg().isEmpty() ? slot.getVehicleReg() : (b != null ? EntityLookup.vehicleReg(garage, b.getVehicleId()) : "-");
        String cust = slot.getCustomerName() != null && !slot.getCustomerName().isEmpty() ? slot.getCustomerName() : "-";
        String desc = slot.getDescription() != null && !slot.getDescription().isEmpty() ? slot.getDescription() : (b != null ? b.getDescription() : "-");

        Label regBadge = new Label(reg);
        regBadge.getStyleClass().addAll("badge", "info", "small");

        Label custLabel = new Label("👤 " + cust);
        custLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-text;");

        HBox carCustRow = new HBox(8, regBadge, custLabel);
        carCustRow.setAlignment(Pos.CENTER_LEFT);

        Label descLabel = new Label("🔧 " + desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -wac-muted;");
        descLabel.setWrapText(true);

        HBox actionRow = new HBox(8);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        if (wo != null) {
            Label stBadge = new Label(UiFormatters.statusWord(wo.getStatus()));
            stBadge.getStyleClass().addAll("badge", UiFormatters.badgeClass(stBadge.getText()), "small");

            Region spr2 = new Region();
            HBox.setHgrow(spr2, Priority.ALWAYS);

            Button openBtn = new Button(I18n.get("kanban.drawer.open_order"));
            openBtn.getStyleClass().addAll("primary", "small");
            openBtn.setOnAction(e -> {
                if (router != null) {
                    router.navigateToWorkOrder(wo.getId());
                }
            });

            actionRow.getChildren().addAll(stBadge, spr2, openBtn);
        } else {
            Region spr2 = new Region();
            HBox.setHgrow(spr2, Priority.ALWAYS);

            Button createBtn = new Button(I18n.get("kanban.drawer.create_order"));
            createBtn.getStyleClass().addAll("primary", "small");
            createBtn.setOnAction(e -> {
                Booking targetBooking = b;
                if (targetBooking == null) {
                    int vehicleId = 1;
                    for (Vehicle v : garage.getVehicles()) {
                        if (v.getRegistrationNumber().equalsIgnoreCase(slot.getVehicleReg())) {
                            vehicleId = v.getId();
                            break;
                        }
                    }
                    targetBooking = garage.createBooking(vehicleId, slot.getDate(), slot.getDescription());
                    slot.setBookingId(targetBooking.getId());
                }
                WorkOrder createdWo = garage.createWorkOrder(targetBooking.getId(), slot.getMechanicId(), 1);
                if (createdWo != null) {
                    slot.setWorkOrderId(createdWo.getId());
                    if (onRefresh != null) onRefresh.run();
                    if (router != null) {
                        router.navigateToWorkOrder(createdWo.getId());
                    }
                }
            });

            actionRow.getChildren().addAll(spr2, createBtn);
        }

        drawer.getChildren().addAll(head, carCustRow, descLabel, actionRow);
        return drawer;
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
