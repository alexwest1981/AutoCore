package com.wac.autocore.ui;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.MultiSelectComboBox;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;


import java.util.ArrayList;
import java.sql.SQLException;

/**
 * Dialoger för servicepaket: skapa ett paket och visa de som finns.
 */
public final class ServicePackageDialogs {

    private ServicePackageDialogs() {
    }

    public static void showCreatePackageDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.package.create.title"));
        dialog.setHeaderText(I18n.get("dialog.package.create.header"));
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
                garage.createServicePackage(
                        nameField.getText(),
                        descField.getText().trim(),
                        new ArrayList<ServiceItem>(serviceBox.getSelectedItems()));
            } catch (IllegalArgumentException e) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
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

        Button deleteButton = new Button(I18n.get("dialog.package.delete.button"));
        deleteButton.setDisable(true);
        list.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, selected) -> deleteButton.setDisable(selected == null));
        deleteButton.setOnAction(e -> confirmDelete(garage, list));

        VBox content = new VBox(10, list, deleteButton);

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
