package com.wac.autocore.ui.views;

import com.wac.autocore.model.Customer;
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
final class CustomerPage {

    private CustomerPage() {
    }

    static VBox build(GarageSystem garage, PageRouter router) {
        FilterableTable<Customer> table = TableFactory.create(garage.getCustomers());
        TableView<Customer> view = table.getTableView();
        view.getColumns().addAll(
                TableFactory.idCol(c -> String.valueOf(c.getId())),
                TableFactory.textCol(I18n.get("table.col.name"), TableFactory.W_PERSON_MIN, TableFactory.W_PERSON_MAX, Customer::getName),
                TableFactory.sizeCol(I18n.get("table.col.phone"), TableFactory.W_PHONE, Customer::getPhone),
                TableFactory.textCol(I18n.get("table.col.email"), TableFactory.W_EMAIL_MIN, TableFactory.W_EMAIL_MAX, Customer::getEmail),
                TableFactory.sizeBadge(I18n.get("table.col.vip"), TableFactory.W_FLAG,
                        c -> c.isVip() ? I18n.get("common.yes") : I18n.get("common.no")));
        router.setActiveTable(table);

        Button addButton = UiComponents.primaryButton(I18n.get("entity.customers.action_create"));
        addButton.setOnAction(e -> ActionDialogs.showCreateCustomerDialog(garage, () -> router.navigate("customers")));

        Button editButton = UiComponents.secondaryButton(I18n.get("entity.customers.action_edit"));
        Button deleteButton = UiComponents.secondaryButton(I18n.get("entity.customers.action_delete"));
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        view.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            editButton.setDisable(selected == null);
            deleteButton.setDisable(selected == null);
        });

        editButton.setOnAction(e -> {
            Customer selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showEditCustomerDialog(garage, selected, () -> router.navigate("customers"));
            }
        });
        deleteButton.setOnAction(e -> {
            Customer selected = view.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ActionDialogs.showDeleteCustomerConfirmation(garage, selected, () -> router.navigate("customers"));
            }
        });

        view.setRowFactory(tableView -> {
            TableRow<Customer> row = new TableRow<Customer>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ActionDialogs.showEditCustomerDialog(garage, row.getItem(), () -> router.navigate("customers"));
                }
            });
            return row;
        });

        return UiComponents.buildEntityPage(
                I18n.get("entity.customers.title"),
                PageFormatters.meta("entity.customers.meta", garage.getCustomers().size()),
                I18n.get("entity.customers.subtitle"),
                view, deleteButton, editButton, addButton);
    }
}
