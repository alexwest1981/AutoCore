package com.wac.autocore.ui;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.wac.autocore.seed.SeedText;

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
        ComboBox<String> specBox = createSpecializationBox(garage, null);

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String spec = specBox.getEditor().getText() != null ? specBox.getEditor().getText().trim() : "";
                if (spec.isEmpty() && specBox.getValue() != null) {
                    spec = specBox.getValue().trim();
                }

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
        ComboBox<String> specBox = createSpecializationBox(garage, SeedText.resolve(mechanic.getSpecialization()));
        CheckBox availBox = new CheckBox(I18n.get("dialog.mechanic.available"));
        availBox.setSelected(mechanic.isAvailable());

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specBox, 1, 2);
        grid.add(new Label(I18n.get("table.col.status") + ":"), 0, 3);
        grid.add(availBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String spec = specBox.getEditor().getText() != null ? specBox.getEditor().getText().trim() : "";
                if (spec.isEmpty() && specBox.getValue() != null) {
                    spec = specBox.getValue().trim();
                }

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

    private static ComboBox<String> createSpecializationBox(GarageSystem garage, String currentSpec) {
        ComboBox<String> box = new ComboBox<String>();
        box.setEditable(true);
        box.setPromptText(I18n.get("dialog.mechanic.spec_prompt"));

        List<String> suggestions = new ArrayList<String>();
        suggestions.add(I18n.get("dialog.mechanic.default_spec"));
        suggestions.add(I18n.get("kanban.specialization.brakes"));
        suggestions.add(I18n.get("kanban.specialization.diagnostics"));
        suggestions.add("Däck & Hjul");
        suggestions.add("Motor & Drivlina");
        suggestions.add("AC & Klimat");

        if (garage != null) {
            for (ServiceItem s : garage.getServiceItems()) {
                String serviceName = SeedText.resolve(s.getName());
                if (serviceName != null && !serviceName.trim().isEmpty() && !suggestions.contains(serviceName.trim())) {
                    suggestions.add(serviceName.trim());
                }
            }
        }

        box.getItems().addAll(suggestions);
        if (currentSpec != null && !currentSpec.trim().isEmpty()) {
            box.getEditor().setText(currentSpec);
            box.setValue(currentSpec);
        }
        return box;
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
