package com.wac.autocore.ui;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import com.wac.autocore.seed.SeedText;

/** Dialoger för tjänstehantering. */
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
        ComboBox<String> specBox = requirementBox(garage);

        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("table.col.price") + ":"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label(I18n.get("table.col.time") + ":"), 0, 3);
        grid.add(timeField, 1, 3);
        grid.add(new Label(I18n.get("dialog.service.spec_label") + ":"), 0, 4);
        grid.add(specBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, nameField, priceField, timeField);

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

                Double price = parsePrice(priceStr);
                Integer time = parseMinutes(timeStr);
                if (price == null || time == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                try {
                    garage.createServiceItem(name, desc, price, time, requirementKey(specBox));
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

        TextField nameField = new TextField(serviceItem.getName() != null ? SeedText.resolve(serviceItem.getName()) : "");
        nameField.setPromptText(I18n.get("dialog.service.name_prompt"));
        TextField descField = new TextField(serviceItem.getDescription() != null ? SeedText.resolve(serviceItem.getDescription()) : "");
        descField.setPromptText(I18n.get("dialog.service.desc_prompt"));
        TextField priceField = new TextField(String.valueOf(serviceItem.getPrice()));
        priceField.setPromptText(I18n.get("dialog.service.price_prompt"));
        TextField timeField = new TextField(String.valueOf(serviceItem.getEstimatedMinutes()));
        timeField.setPromptText(I18n.get("dialog.service.time_prompt"));
        ComboBox<String> specBox = requirementBox(garage);
        showRequirement(specBox, serviceItem);

        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("table.col.price") + ":"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label(I18n.get("table.col.time") + ":"), 0, 3);
        grid.add(timeField, 1, 3);
        grid.add(new Label(I18n.get("dialog.service.spec_label") + ":"), 0, 4);
        grid.add(specBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, nameField, priceField, timeField);

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

                Double price = parsePrice(priceStr);
                Integer time = parseMinutes(timeStr);
                if (price == null || time == null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_number"));
                    return;
                }

                serviceItem.setName(name);
                serviceItem.setDescription(desc);
                serviceItem.setPrice(price);
                serviceItem.setEstimatedMinutes(time);
                serviceItem.setSpecialization(requirementKey(specBox));

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

    static Double parsePrice(String priceStr) {
        if (priceStr == null) return null;
        String clean = priceStr.replaceAll("[^0-9,.]", "").replace(",", ".").trim();
        if (clean.isEmpty()) return null;
        try {
            double val = Double.parseDouble(clean);
            return val > 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Integer parseMinutes(String timeStr) {
        if (timeStr == null) return null;
        String clean = timeStr.replaceAll("[^0-9,.]", "").replace(",", ".").trim();
        if (clean.isEmpty()) return null;
        try {
            if (clean.contains(".")) {
                double val = Double.parseDouble(clean);
                return val > 0 ? (int) Math.round(val) : null;
            }
            int val = Integer.parseInt(clean);
            return val > 0 ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void showDeleteServiceItemConfirmation(GarageSystem garage, ServiceItem serviceItem, Runnable onSuccess) {
        if (serviceItem == null) return;

        if (!garage.canDeleteServiceItem(serviceItem.getId())) {
            ActionDialogs.showError(I18n.get("dialog.service.delete.title"),
                    I18n.get("dialog.service.delete.has_active_orders", SeedText.resolve(serviceItem.getName())));
            return;
        }

        Alert alert = ActionDialogs.confirm(I18n.get("dialog.service.delete.title"),
                I18n.get("dialog.service.delete.header"),
                I18n.get("dialog.service.delete.confirm", SeedText.resolve(serviceItem.getName())));

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

/** Specialiseringen tjänsten kräver. Första valet betyder alla mekaniker. */
    static ComboBox<String> requirementBox(GarageSystem garage) {
        ComboBox<String> box = new ComboBox<String>();
        box.getItems().add(I18n.get("dialog.service.spec_any"));
        box.getItems().addAll(MechanicDialogs.suggestSpecializations(garage));
        box.getSelectionModel().selectFirst();
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Nyckeln som ska sparas: tom när tjänsten inte kräver någon särskild specialisering. */
    static String requirementKey(ComboBox<String> box) {
        String chosen = box.getValue();
        if (chosen == null || chosen.equals(I18n.get("dialog.service.spec_any"))) {
            return "";
        }
        return MechanicDialogs.specializationToStore(chosen);
    }

    /** Visar tjänstens nuvarande krav i rullistan. */
    static void showRequirement(ComboBox<String> box, ServiceItem serviceItem) {
        String stored = serviceItem.getSpecialization();
        if (stored == null || stored.trim().isEmpty()) {
            box.getSelectionModel().selectFirst();
            return;
        }
        String text = com.wac.autocore.seed.SeedText.resolve(stored);
        for (String option : box.getItems()) {
            if (option.equals(text)) {
                box.getSelectionModel().select(option);
                return;
            }
        }
        box.getItems().add(text);
        box.getSelectionModel().select(text);
    }
}
