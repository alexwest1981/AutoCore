package com.wac.autocore.ui;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.MultiSelectComboBox;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.sql.SQLException;
import java.util.List;

/**
 * Dialogs for service packages: create a package and list the ones that exist.
 */
public final class ServicePackageDialogs {

    private ServicePackageDialogs() {
    }

    public static void showCreatePackageDialog(GarageSystem garage, Runnable onSuccess){
        showPackageForm(garage, null, onSuccess);
    }

    public static void showEditPackageDialog(GarageSystem garage, ServicePackage servicePackage, Runnable onSuccess) {
        showPackageForm(garage, servicePackage, onSuccess);
    }

    private static void showPackageForm(GarageSystem garage, ServicePackage existing, Runnable onSuccess) {
        boolean editing = existing != null;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get(editing ? "dialog.package.edit.title" : "dialog.package.create.title"));
        dialog.setHeaderText(I18n.get(editing ? "dialog.package.edit.header" : "dialog.package.create.header"));
        ActionDialogs.styleDialog(dialog);

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.package.name_prompt"));

        TextField descField = new TextField();
        descField.setPromptText(I18n.get("dialog.package.desc_prompt"));

        MultiSelectComboBox<ServiceItem> serviceBox = new MultiSelectComboBox<ServiceItem>(
                I18n.get("dialog.package.service_select"),
                s -> SeedText.resolve(s.getName()));
        serviceBox.setItems(garage.getServiceItems());
        serviceBox.setKeyProvider(s -> s.getId());

        if (editing) {
            nameField.setText(existing.getName());
            descField.setText(existing.getDescription() == null ? "" : existing.getDescription());
            serviceBox.setSelectedItems(existing.getServiceItems());
        }

        GridPane grid = ActionDialogs.createGrid();
        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("dialog.package.services_label") + ":"), 0, 2);
        grid.add(serviceBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, nameField);

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            try {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                List<ServiceItem> items = new ArrayList<ServiceItem>(serviceBox.getSelectedItems());

                if (editing){
                    ServicePackage changed = new ServicePackage(existing.getId(), name, desc);
                    changed.setServiceItems(items);
                    garage.updateServicePackage(changed);
                } else {
                    garage.createServicePackage(name, desc, items);
                }
            } catch (SQLException e) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                return;
            } catch (ValidationException rejected) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get(rejected.getMessageKey()));
                return;
            }
            if (onSuccess != null) onSuccess.run();
        });
    }

    public static void showPackagesDialog(GarageSystem garage) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.package.list.title"));
        ActionDialogs.styleDialog(dialog);

        ListView<ServicePackage> list = new ListView<ServicePackage>();
        list.getItems().addAll(garage.getServicePackages());
        list.setCellFactory(lv -> new ListCell<ServicePackage>() {
            @Override
            protected void updateItem(ServicePackage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : describe(item));
            }
        });
        list.setPlaceholder(new Label(I18n.get("dialog.package.list.empty")));
        list.setPrefSize(520, 300);

        Button editButton = new Button(I18n.get("dialog.package.edit.button"));
        editButton.setDisable(true);

        Button deleteButton = new Button(I18n.get("dialog.package.delete.button"));
        deleteButton.setDisable(true);

        list.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, selected) -> {
                   editButton.setDisable(selected == null);
                   deleteButton.setDisable(selected == null);
                });
        deleteButton.setOnAction(e -> confirmDelete(garage, list));
        editButton.setOnAction(e -> {
            ServicePackage selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            showEditPackageDialog(garage, selected,
                    () -> list.getItems().setAll(garage.getServicePackages()));
        });
        HBox buttons = new HBox(10, editButton, deleteButton);
        VBox content = new VBox(10, list, buttons);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static void confirmDelete(GarageSystem garageSystem, ListView<ServicePackage> list) {
        ServicePackage selected = list.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert alert = ActionDialogs.confirm(
                I18n.get("dialog.package.delete.title"),
                I18n.get("dialog.package.delete.header"),
                I18n.get("dialog.package.delete.confirm", selected.getName()));
        alert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            try {
                garageSystem.deleteServicePackage(selected.getId());
            } catch (SQLException e) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                return;
            }
            list.getItems().remove(selected);
        });

    }

    private static String describe(ServicePackage servicePackage) {
        StringBuilder names = new StringBuilder();
        for (ServiceItem item : servicePackage.getServiceItems()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(SeedText.resolve(item.getName()));
        }
        return servicePackage.getName()
                + "  .  " + UiFormatters.formatMoney(servicePackage.getTotalPrice())
                + "  .  " + servicePackage.getTotalEstimatedMinutes() + " min\n"
                + names;
    }

}
