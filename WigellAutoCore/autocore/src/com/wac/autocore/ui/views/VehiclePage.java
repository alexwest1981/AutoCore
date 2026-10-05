package com.wac.autocore.ui.views;

import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.EntityLookup;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

@SuppressWarnings("unchecked")
final class VehiclePage {

    private VehiclePage() {
    }

    static VBox build(GarageSystem garage, PageRouter router) {
        FilterableTable<Vehicle> table = TableFactory.create(garage.getVehicles());
        TableView<Vehicle> view = table.getTableView();
        view.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.sizeCol(I18n.get("table.col.reg_nr"), TableFactory.W_REG_NR, Vehicle::getRegistrationNumber),
                TableFactory.sizeCol(I18n.get("table.col.brand"), TableFactory.W_BRAND, Vehicle::getBrand),
                TableFactory.sizeCol(I18n.get("table.col.model"), TableFactory.W_MODEL, Vehicle::getModel),
                TableFactory.sizeCol(I18n.get("table.col.year"), TableFactory.W_YEAR, c -> String.valueOf(c.getYear())),
                TableFactory.textCol(I18n.get("table.col.customer"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX,
                        c -> EntityLookup.customerName(garage, c.getCustomerId())));
        router.setActiveTable(table);

        Button addButton = UiComponents.primaryButton(I18n.get("entity.vehicles.action_create"));
        addButton.setOnAction(e -> ActionDialogs.showCreateVehicleDialog(garage, () -> router.navigate("vehicles")));

        Button editButton = UiComponents.secondaryButton(I18n.get("entity.vehicles.action_edit"));
        Button deleteButton = UiComponents.secondaryButton(I18n.get("entity.vehicles.action_delete"));
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        view.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            editButton.setDisable(selected == null);
            deleteButton.setDisable(selected == null);
        });

        editButton.setOnAction(e -> {
            Vehicle selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showEditVehicleDialog(garage, selected, () -> router.navigate("vehicles"));
            }
        });
        deleteButton.setOnAction(e -> {
            Vehicle selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showDeleteVehicleConfirmation(garage, selected, () -> router.navigate("vehicles"));
            }
        });

        view.setRowFactory(tableView -> {
            TableRow<Vehicle> row = new TableRow<Vehicle>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditVehicleDialog(garage, row.getItem(), () -> router.navigate("vehicles"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.vehicles.title"),
                PageFormatters.meta("entity.vehicles.meta", garage.getVehicles().size()),
                I18n.get("entity.vehicles.subtitle"),
                view, deleteButton, editButton, addButton);
    }
}
