package com.wac.autocore.ui;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;

/**
 * Modala dialoger för tjänstehantering (skapa, redigera, ta bort).
 */
public final class ServiceItemDialogs {

    private ServiceItemDialogs() {}

    public static void showCreateServiceItemDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.service.create.title"));
        dialog.setHeaderText(I18n.get("dialog.service.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.service.name_prompt"));
        TextField descField = new TextField();
        descField.setPromptText(I18n.get("dialog.service.desc_prompt"));
        TextField priceField = new TextField();
        priceField.setPromptText(I18n.get("dialog.service.price_prompt"));
        TextField timeField = new TextField();
        timeField.setPromptText(I18n.get("dialog.service.time_prompt"));

        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("table.col.price") + ":"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label(I18n.get("table.col.time") + ":"), 0, 3);
        grid.add(timeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String priceStr = priceField.getText().trim();
                String timeStr = timeField.getText().trim();

                if (name.isEmpty() || priceStr.isEmpty() || timeStr.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                double price;
                int time;
                try {
                    price = Double.parseDouble(priceStr.replace(",", "."));
                    time = Integer.parseInt(timeStr);
                } catch (NumberFormatException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                try {
                    garage.createServiceItem(name, desc, price, time);
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditServiceItemDialog(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        if (serviceItem == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.service.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.service.edit.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = new TextField(serviceItem.getName() != null ? serviceItem.getName() : "");
        nameField.setPromptText(I18n.get("dialog.service.name_prompt"));
        TextField descField = new TextField(serviceItem.getDescription() != null ? serviceItem.getDescription() : "");
        descField.setPromptText(I18n.get("dialog.service.desc_prompt"));
        TextField priceField = new TextField(String.valueOf(serviceItem.getPrice()));
        priceField.setPromptText(I18n.get("dialog.service.price_prompt"));
        TextField timeField = new TextField(String.valueOf(serviceItem.getEstimatedMinutes()));
        timeField.setPromptText(I18n.get("dialog.service.time_prompt"));

        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("table.col.price") + ":"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label(I18n.get("table.col.time") + ":"), 0, 3);
        grid.add(timeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String priceStr = priceField.getText().trim();
                String timeStr = timeField.getText().trim();

                if (name.isEmpty() || priceStr.isEmpty() || timeStr.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                double price;
                int time;
                try {
                    price = Double.parseDouble(priceStr.replace(",", "."));
                    time = Integer.parseInt(timeStr);
                } catch (NumberFormatException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                serviceItem.setName(name);
                serviceItem.setDescription(desc);
                serviceItem.setPrice(price);
                serviceItem.setEstimatedMinutes(time);

                try {
                    garage.updateServiceItem(serviceItem);
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteServiceItemConfirmation(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        if (serviceItem == null) return;

        if (!garage.canDeleteServiceItem(serviceItem.getId())) {
            ActionDialogs.showError(I18n.get("dialog.service.delete.title"),
                    I18n.get("dialog.service.delete.has_active_orders", serviceItem.getName()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.service.delete.title"));
        alert.setHeaderText(I18n.get("dialog.service.delete.header"));
        alert.setContentText(I18n.get("dialog.service.delete.confirm", serviceItem.getName()));
        ActionDialogs.styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteServiceItem(serviceItem.getId());
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
