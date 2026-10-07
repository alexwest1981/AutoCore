package com.wac.autocore.ui.views;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

@SuppressWarnings("unchecked")
final class MechanicPage {

    private MechanicPage() {
    }

    static VBox build(GarageSystem garage, PageRouter router) {
        FilterableTable<Mechanic> table = TableFactory.create(garage.getMechanics());
        TableView<Mechanic> view = table.getTableView();
        view.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, Mechanic::getName),
                TableFactory.sizeCol(I18n.get("table.col.phone"), TableFactory.W_PHONE, Mechanic::getPhone),
                TableFactory.textCol(I18n.get("table.col.specialisation"), TableFactory.W_SPEC_MIN, TableFactory.W_SPEC_MAX, c -> SeedText.resolve(c.getSpecialization())),
                TableFactory.sizeBadge(I18n.get("table.col.available"), TableFactory.W_FLAG,
                        c -> c.isAvailable() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addButton = UiComponents.primaryButton(I18n.get("entity.mechanics.action_create"));
        addButton.setOnAction(e -> ActionDialogs.showCreateMechanicDialog(garage, () -> router.navigate("mechanics")));
        Button editButton = UiComponents.secondaryButton(I18n.get("entity.mechanics.action_edit"));
        Button deleteButton = UiComponents.secondaryButton(I18n.get("entity.mechanics.action_delete"));
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        view.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            editButton.setDisable(selected == null);
            deleteButton.setDisable(selected == null);
        });
        editButton.setOnAction(e -> {
            Mechanic selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showEditMechanicDialog(garage, selected, () -> router.navigate("mechanics"));
            }
        });
        deleteButton.setOnAction(e -> {
            Mechanic selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showDeleteMechanicConfirmation(garage, selected, () -> router.navigate("mechanics"));
            }
        });
        view.setRowFactory(tableView -> {
            TableRow<Mechanic> row = new TableRow<Mechanic>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditMechanicDialog(garage, row.getItem(), () -> router.navigate("mechanics"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.mechanics.title"),
                PageFormatters.meta("entity.mechanics.meta", garage.getMechanics().size()),
                I18n.get("entity.mechanics.subtitle"),
                table, deleteButton, editButton, addButton);
    }
}
