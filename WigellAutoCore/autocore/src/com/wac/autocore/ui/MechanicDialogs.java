package com.wac.autocore.ui;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;

/**
 * Modala dialoger för mekanikerhantering (skapa, redigera, ta bort).
 */
public final class MechanicDialogs {

    private static final MechanicRepository mechanicRepository = new MechanicRepository();

    private MechanicDialogs() {}

    public static void showCreateMechanicDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.mechanic.create.title"));
        dialog.setHeaderText(I18n.get("dialog.mechanic.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.mechanic.name_prompt"));
        TextField phoneField = new TextField();
        phoneField.setPromptText(I18n.get("dialog.mechanic.phone_prompt"));
        TextField specField = new TextField();
        specField.setPromptText(I18n.get("dialog.mechanic.spec_prompt"));

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String spec = specField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                Mechanic mechanic = new Mechanic(0, name, phone, spec.isEmpty() ? I18n.get("dialog.mechanic.default_spec") : spec);

                try {
                    mechanicRepository.save(mechanic);
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditMechanicDialog(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        if (mechanic == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.mechanic.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.mechanic.edit.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = new TextField(mechanic.getName());
        nameField.setPromptText(I18n.get("dialog.mechanic.name_prompt"));
        TextField phoneField = new TextField(mechanic.getPhone() != null ? mechanic.getPhone() : "");
        phoneField.setPromptText(I18n.get("dialog.mechanic.phone_prompt"));
        TextField specField = new TextField(mechanic.getSpecialization() != null ? mechanic.getSpecialization() : "");
        specField.setPromptText(I18n.get("dialog.mechanic.spec_prompt"));
        CheckBox availBox = new CheckBox(I18n.get("dialog.mechanic.available"));
        availBox.setSelected(mechanic.isAvailable());

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specField, 1, 2);
        grid.add(new Label(I18n.get("table.col.status") + ":"), 0, 3);
        grid.add(availBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String spec = specField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                mechanic.setName(name);
                mechanic.setPhone(phone);
                mechanic.setSpecialization(spec.isEmpty() ? I18n.get("dialog.mechanic.default_spec") : spec);
                mechanic.setAvailable(availBox.isSelected());

                try {
                    garage.updateMechanic(mechanic);
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteMechanicConfirmation(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        if (mechanic == null) return;

        if (!garage.canDeleteMechanic(mechanic.getId())) {
            ActionDialogs.showError(I18n.get("dialog.mechanic.delete.title"),
                    I18n.get("dialog.mechanic.delete.has_active_orders", mechanic.getName()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.mechanic.delete.title"));
        alert.setHeaderText(I18n.get("dialog.mechanic.delete.header"));
        alert.setContentText(I18n.get("dialog.mechanic.delete.confirm", mechanic.getName()));
        ActionDialogs.styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteMechanic(mechanic.getId());
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
