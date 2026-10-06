package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.wac.autocore.seed.SeedText;

/** Kanban-tavlan för mekaniker: alla sida vid sida, en ruta per timme. */
public final class KanbanBoard {

    private KanbanBoard() {}

    public static VBox build(GarageSystem garage, PageRouter router, Runnable onRefresh) {
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
            String displaySpec = MechanicKanbanCard.formatSpecialization(specKey) + " (" + entry.getValue() + ")";
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
}
