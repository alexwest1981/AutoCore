package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.wac.autocore.seed.SeedText;

/** Kanban-tavlan för mekaniker: alla sida vid sida, en ruta per timme. */
public class MechanicKanbanCard {

    public enum KanbanViewMode {
        DAY, WEEK, MONTH
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
        addMechBtn.setMinWidth(Region.USE_PREF_SIZE);
        addMechBtn.setOnAction(e -> javafx.application.Platform.runLater(() -> ActionDialogs.showCreateMechanicDialog(garage, onRefresh)));

        Region spr1 = new Region();
        HBox.setHgrow(spr1, Priority.ALWAYS);

        // Belastningslegend
        HBox legend = buildCompactLegend();

        HBox headLeft = new HBox(12, new VBox(2, title, sub), addMechBtn);
        headLeft.setAlignment(Pos.CENTER_LEFT);

        HBox boardHead = new HBox(12, headLeft, spr1, legend);
        boardHead.setAlignment(Pos.CENTER_LEFT);
        boardHead.setPadding(new Insets(0, 0, 8, 0));

        // Rad med alla mekanikerkort sida vid sida
        HBox cardsRow = new HBox(14);
        cardsRow.setAlignment(Pos.TOP_LEFT);
        cardsRow.setMinWidth(Region.USE_PREF_SIZE);

        // Horisontell scroll om många mekaniker tillkommer
        ScrollPane scroll = new ScrollPane(cardsRow);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

        // Rad 2: Filter-chips & Scroll-kontroller
        HBox filterBox = new HBox(8);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label countLabel = new Label();
        countLabel.getStyleClass().add("kanban-count-label");

        Button scrollLeftBtn = new Button("<");
        scrollLeftBtn.getStyleClass().addAll("kanban-scroll-btn", "small");
        scrollLeftBtn.setTooltip(new Tooltip(I18n.get("kanban.filter.scroll_prev")));
        scrollLeftBtn.setOnAction(e -> scroll.setHvalue(Math.max(0.0, scroll.getHvalue() - 0.35)));

        Button scrollRightBtn = new Button(">");
        scrollRightBtn.getStyleClass().addAll("kanban-scroll-btn", "small");
        scrollRightBtn.setTooltip(new Tooltip(I18n.get("kanban.filter.scroll_next")));
        scrollRightBtn.setOnAction(e -> scroll.setHvalue(Math.min(1.0, scroll.getHvalue() + 0.35)));

        HBox scrollControls = new HBox(8, countLabel, scrollLeftBtn, scrollRightBtn);
        scrollControls.setAlignment(Pos.CENTER_RIGHT);

        Region spr2 = new Region();
        HBox.setHgrow(spr2, Priority.ALWAYS);

        HBox controlBar = new HBox(10, filterBox, spr2, scrollControls);
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(8, 0, 10, 0));

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
                if (activeFilter[0] == null || activeFilter[0].equalsIgnoreCase(SeedText.resolve(m.getSpecialization()))) {
                    MechanicKanbanCard card = new MechanicKanbanCard(garage, i, router, onRefresh);
                    VBox cardView = card.getView();
                    cardView.setMinWidth(360);
                    cardView.setPrefWidth(400);
                    cardView.setMaxWidth(400);
                    HBox.setHgrow(cardView, Priority.NEVER);
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
            String spec = SeedText.resolve(m.getSpecialization());
            if (spec != null && !spec.trim().isEmpty()) {
                specCounts.put(spec, specCounts.containsKey(spec) ? specCounts.get(spec) + 1 : 1);
            }
        }

        // Knapp: Alla
        Button allBtn = new Button(I18n.get("kanban.filter.all") + " (" + mechanics.size() + ")");
        allBtn.getStyleClass().addAll("kanban-filter-chip", "active");
        allBtn.setMinWidth(Region.USE_PREF_SIZE);
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
            chip.setMinWidth(Region.USE_PREF_SIZE);
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
    final GarageSystem garage;
    final com.wac.autocore.ui.navigation.PageRouter router;
    final Runnable onRefresh;
    private final VBox cardContainer;

    private int activeMechanicIndex;
    KanbanViewMode currentMode = KanbanViewMode.DAY;
    LocalDate selectedDate = LocalDate.now();
    TimeSlot expandedSlot;

    void toggleExpandSlot(TimeSlot slot) {
        if (expandedSlot != null && expandedSlot.getDate().equals(slot.getDate()) && expandedSlot.getHour() == slot.getHour()) {
            expandedSlot = null;
        } else {
            expandedSlot = slot;
        }
        renderBody();
    }

    boolean isExpanded(TimeSlot slot) {
        return expandedSlot != null && expandedSlot.getDate().equals(slot.getDate()) && expandedSlot.getHour() == slot.getHour();
    }

    final KanbanSlotDrawer slotDrawer = new KanbanSlotDrawer(this);
    final KanbanDayView dayView = new KanbanDayView(this);
    final KanbanWeekView weekView = new KanbanWeekView(this);
    final KanbanMonthView monthView = new KanbanMonthView(this);

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
        this.cardContainer.setMinWidth(360);
        this.cardContainer.setPrefWidth(400);

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
        nameLabel.setMinWidth(0);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setTooltip(new Tooltip(mech.getName()));
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        HBox mechTitle = new HBox(6, avatar, nameLabel);
        mechTitle.setAlignment(Pos.CENTER_LEFT);
        mechTitle.setMinWidth(0);
        HBox.setHgrow(mechTitle, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Segmenterad vyväxlare: Dag | Vecka | Månad
        Button dayBtn = createViewButton(I18n.get("kanban.view.day"), KanbanViewMode.DAY);
        Button weekBtn = createViewButton(I18n.get("kanban.view.week"), KanbanViewMode.WEEK);
        Button monthBtn = createViewButton(I18n.get("kanban.view.month"), KanbanViewMode.MONTH);

        HBox toggleGroup = new HBox(2, dayBtn, weekBtn, monthBtn);
        toggleGroup.getStyleClass().add("kanban-toggle-group-compact");
        toggleGroup.setMinWidth(Region.USE_PREF_SIZE);

        Button optionsBtn = new Button();
        optionsBtn.getStyleClass().addAll("kanban-options-btn", "ghost", "small");
        optionsBtn.setTooltip(new Tooltip(I18n.get("kanban.card.options")));
        optionsBtn.setMinWidth(Region.USE_PREF_SIZE);

        SVGPath dotsIcon = new SVGPath();
        dotsIcon.setContent("M 2 3.2 a 1.2 1.2 0 1 1 0 -2.4 a 1.2 1.2 0 0 1 0 2.4 z M 2 7.2 a 1.2 1.2 0 1 1 0 -2.4 a 1.2 1.2 0 0 1 0 2.4 z M 2 11.2 a 1.2 1.2 0 1 1 0 -2.4 a 1.2 1.2 0 0 1 0 2.4 z");
        dotsIcon.setFill(javafx.scene.paint.Color.web("#59635e"));
        dotsIcon.getStyleClass().add("kanban-options-icon");
        optionsBtn.setGraphic(dotsIcon);

        ContextMenu optionsMenu = new ContextMenu();
        MenuItem editItem = new MenuItem(I18n.get("kanban.card.edit_mech"));
        editItem.setOnAction(e -> javafx.application.Platform.runLater(() ->
                ActionDialogs.showEditMechanicDialog(garage, mech, () -> {
                    render();
                    if (onRefresh != null) onRefresh.run();
                })
        ));

        MenuItem deleteItem = new MenuItem(I18n.get("kanban.card.delete_mech"));
        deleteItem.getStyleClass().add("menu-item-danger");
        deleteItem.setOnAction(e -> javafx.application.Platform.runLater(() ->
                ActionDialogs.showDeleteMechanicConfirmation(garage, mech, () -> {
                    if (onRefresh != null) onRefresh.run();
                })
        ));

        optionsMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);

        optionsBtn.setOnAction(e -> {
            if (optionsMenu.isShowing()) {
                optionsMenu.hide();
            } else {
                optionsMenu.show(optionsBtn, javafx.geometry.Side.BOTTOM, 0, 4);
            }
        });

        HBox topRow = new HBox(6, mechTitle, spacer, toggleGroup, optionsBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Rad 2: Specialisering och tillgänglighetsbadge
        Label specLabel = new Label(formatSpecialization(SeedText.resolve(mech.getSpecialization())));
        specLabel.getStyleClass().add("kanban-mech-sub-compact");
        specLabel.setMinWidth(0);
        specLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        HBox.setHgrow(specLabel, Priority.ALWAYS);

        Region spr2 = new Region();
        HBox.setHgrow(spr2, Priority.ALWAYS);

        Label statusBadge = new Label(mech.isAvailable() ? I18n.get("table.col.available") : I18n.get("table.col.unavailable"));
        statusBadge.getStyleClass().addAll("badge", mech.isAvailable() ? "green" : "yellow", "small");
        statusBadge.setMinWidth(Region.USE_PREF_SIZE);

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

    void renderBody() {
        bodyContent.getChildren().clear();
        Mechanic mech = getActiveMechanic();
        if (mech == null) return;

        switch (currentMode) {
            case DAY:
                bodyContent.getChildren().add(dayView.build(mech));
                break;
            case WEEK:
                bodyContent.getChildren().add(weekView.build(mech));
                break;
            case MONTH:
                bodyContent.getChildren().add(monthView.build(mech));
                break;
        }
    }

    // =========================================================================
    // 1. KOMPAKT DAGSVY (07:00 - 16:00)
    // =========================================================================
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
