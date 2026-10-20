package com.wac.autocore.ui.components;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.motion.SegmentedSwitch;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import com.wac.autocore.seed.SeedText;

/**
 * The card's header: avatar and name, the Day/Week/Month view switcher,
 * the specialization and the availability badge, and the edit menu.
 */
class KanbanCardHeader {

    /** The segmented control is built once per card: a control that is rebuilt cannot animate. */
    private static final MechanicKanbanCard.KanbanViewMode[] MODES = {
            MechanicKanbanCard.KanbanViewMode.DAY,
            MechanicKanbanCard.KanbanViewMode.WEEK,
            MechanicKanbanCard.KanbanViewMode.MONTH
    };

    private final MechanicKanbanCard card;
    private final VBox box = new VBox(6);
    private final SegmentedSwitch viewSwitch = new SegmentedSwitch();

    KanbanCardHeader(MechanicKanbanCard card) {
        this.card = card;
        viewSwitch.getStyleClass().add("kanban-toggle-group-compact");
        viewSwitch.setSpacing(2);
        viewSwitch.setMinWidth(Region.USE_PREF_SIZE);
        viewSwitch.setOnSelect(index -> {
            card.currentMode = MODES[index];
            card.renderBody();
        });
    }

    VBox getBox() { return box; }

    void render() {
        box.getChildren().clear();

        GarageSystem garage = card.garage;
        Mechanic mech = card.getActiveMechanic();
        if (mech == null) {
            box.getChildren().add(new Label(I18n.get("kanban.no_mechanic")));
            return;
        }

        // Avatar
        String initials = initials(mech.getName());
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

        // Segmented view switcher: Day | Week | Month, with an indicator that travels
        viewSwitch.setLabels(I18n.get("kanban.view.day"), I18n.get("kanban.view.week"), I18n.get("kanban.view.month"));
        viewSwitch.select(modeIndex(card.currentMode));

        Button optionsBtn = new Button();
        optionsBtn.getStyleClass().addAll("kanban-options-btn", "ghost", "small");
        optionsBtn.setTooltip(new Tooltip(I18n.get("kanban.card.options")));
        optionsBtn.setMinWidth(Region.USE_PREF_SIZE);

        SVGPath dotsIcon = new SVGPath();
        dotsIcon.setContent("M 2 3.2 a 1.2 1.2 0 1 1 0 -2.4 a 1.2 1.2 0 0 1 0 2.4 z M 2 7.2 a 1.2 1.2 0 1 1 0 -2.4 a 1.2 1.2 0 0 1 0 2.4 z M 2 11.2 a 1.2 1.2 0 1 1 0 -2.4 a 1.2 1.2 0 0 1 0 2.4 z");
        dotsIcon.setFill(Color.web("#59635e"));
        dotsIcon.getStyleClass().add("kanban-options-icon");
        optionsBtn.setGraphic(dotsIcon);

        ContextMenu optionsMenu = new ContextMenu();
        MenuItem editItem = new MenuItem(I18n.get("kanban.card.edit_mech"));
        editItem.setOnAction(e -> javafx.application.Platform.runLater(() ->
                ActionDialogs.showEditMechanicDialog(garage, mech, () -> {
                    card.render();
                    if (card.onRefresh != null) card.onRefresh.run();
                })
        ));

        MenuItem deleteItem = new MenuItem(I18n.get("kanban.card.delete_mech"));
        deleteItem.getStyleClass().add("menu-item-danger");
        deleteItem.setOnAction(e -> javafx.application.Platform.runLater(() ->
                ActionDialogs.showDeleteMechanicConfirmation(garage, mech, () -> {
                    if (card.onRefresh != null) card.onRefresh.run();
                })
        ));

        optionsMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);

        optionsBtn.setOnAction(e -> {
            if (optionsMenu.isShowing()) {
                optionsMenu.hide();
            } else {
                optionsMenu.show(optionsBtn, Side.BOTTOM, 0, 4);
            }
        });

        HBox topRow = new HBox(6, mechTitle, spacer, viewSwitch, optionsBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Row 2: specialization and availability badge
        Label specLabel = new Label(MechanicKanbanCard.formatSpecialization(SeedText.resolve(mech.getSpecialization())));
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

        box.getChildren().addAll(topRow, subRow);
    }

    private static int modeIndex(MechanicKanbanCard.KanbanViewMode mode) {
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == mode) return i;
        }
        return 0;
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
