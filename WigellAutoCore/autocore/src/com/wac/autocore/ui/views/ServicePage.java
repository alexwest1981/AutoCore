package com.wac.autocore.ui.views;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ActionDialogs;
import com.wac.autocore.ui.components.TableFactory;
import com.wac.autocore.ui.components.TableFactory.FilterableTable;
import com.wac.autocore.ui.components.UiComponents;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.navigation.PageRouter;
import com.wac.autocore.ui.util.UiFormatters;
import javafx.scene.control.Button;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

@SuppressWarnings("unchecked")
final class ServicePage {

    private ServicePage() {
    }

    static VBox build(GarageSystem garage, PageRouter router) {
        FilterableTable<ServiceItem> table = TableFactory.create(garage.getServiceItems());
        TableView<ServiceItem> view = table.getTableView();
        view.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, c -> SeedText.resolve(c.getName())),
                TableFactory.textCol(I18n.get("table.col.description"), TableFactory.W_TEXT_MIN, TableFactory.W_TEXT_MAX, c -> SeedText.resolve(c.getDescription())),
                TableFactory.sizeCol(I18n.get("table.col.price"), TableFactory.W_MONEY, c -> UiFormatters.formatMoney(c.getPrice())),
                TableFactory.sizeCol(I18n.get("table.col.time"), TableFactory.W_MINUTES, c -> c.getEstimatedMinutes() + " min"));
        router.setActiveTable(table);

        Button addButton = UiComponents.primaryButton(I18n.get("entity.services.action_create"));
        addButton.setOnAction(e -> ActionDialogs.showCreateServiceItemDialog(garage, () -> router.navigate("services")));
        Button editButton = UiComponents.secondaryButton(I18n.get("entity.services.action_edit"));
        Button deleteButton = UiComponents.secondaryButton(I18n.get("entity.services.action_delete"));
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        view.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            editButton.setDisable(selected == null);
            deleteButton.setDisable(selected == null);
        });
        editButton.setOnAction(e -> {
            ServiceItem selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showEditServiceItemDialog(garage, selected, () -> router.navigate("services"));
            }
        });
        deleteButton.setOnAction(e -> {
            ServiceItem selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showDeleteServiceItemConfirmation(garage, selected, () -> router.navigate("services"));
            }
        });
        view.setRowFactory(tableView -> {
            TableRow<ServiceItem> row = new TableRow<ServiceItem>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditServiceItemDialog(garage, row.getItem(), () -> router.navigate("services"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.services.title"),
                PageFormatters.meta("entity.services.meta", garage.getServiceItems().size()),
                I18n.get("entity.services.subtitle"),
                view, deleteButton, editButton, addButton);
    }
}
