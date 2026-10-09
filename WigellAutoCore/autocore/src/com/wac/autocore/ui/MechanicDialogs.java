package com.wac.autocore.ui;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
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

/** Dialogs for managing mechanics. */
public final class MechanicDialogs {

    private MechanicDialogs() {}

    public static void showCreateMechanicDialog(GarageSystem garage, Runnable onSuccess) {
        showMechanicForm(garage, null, onSuccess);
    }

    public static void showEditMechanicDialog(GarageSystem garage, Mechanic mechanic, Runnable onSuccess) {
        if (mechanic == null) return;

        showMechanicForm(garage, mechanic, onSuccess);
    }

    /** One form for both, because create and edit ask for the same fields. */
    private static void showMechanicForm(GarageSystem garage, Mechanic existing, Runnable onSuccess) {
        boolean editing = existing != null;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get(editing ? "dialog.mechanic.edit.title" : "dialog.mechanic.create.title"));
        dialog.setHeaderText(I18n.get(editing ? "dialog.mechanic.edit.header" : "dialog.mechanic.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = ActionDialogs.field("dialog.mechanic.name_prompt", editing ? existing.getName() : null);
        TextField phoneField = ActionDialogs.field("dialog.mechanic.phone_prompt", editing ? existing.getPhone() : null);
        ComboBox<String> specBox = createSpecializationBox(garage,
                editing ? SeedText.resolve(existing.getSpecialization()) : null);

        CheckBox availBox = new CheckBox(I18n.get("dialog.mechanic.available"));
        if (editing) {
            availBox.setSelected(existing.isAvailable());
        }

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("table.col.specialisation") + ":"), 0, 2);
        grid.add(specBox, 1, 2);
        // The availability belongs to a mechanic that already exists, so the create form leaves it out.
        if (editing) {
            grid.add(new Label(I18n.get("table.col.status") + ":"), 0, 3);
            grid.add(availBox, 1, 3);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, nameField, phoneField);

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }

            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();

            String problem = Mechanic.validationProblem(name, phone);
            if (problem != null) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation." + problem));
                return;
            }

            String specialization = specializationToStore(chosenSpecialization(specBox));

            try {
                if (editing) {
                    existing.setName(name);
                    existing.setPhone(phone);
                    existing.setSpecialization(specialization);
                    existing.setAvailable(availBox.isSelected());
                    garage.updateMechanic(existing);
                } else {
                    garage.createMechanic(name, phone, specialization);
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

    /** The fixed specializations as keys in the seed dictionary. */
    private static final String[] SPECIALIZATION_KEYS = {
        "seed.mechanic.general_service.specialization",
        "seed.mechanic.brakes.specialization",
        "seed.mechanic.diagnostics.specialization",
        "seed.mechanic.wheels.specialization",
        "seed.mechanic.engine.specialization",
        "seed.mechanic.climate.specialization"
    };

    /** The specializations in the drop-down. The test reads the same list the user does. */
    public static List<String> suggestSpecializations(GarageSystem garage) {
        List<String> suggestions = new ArrayList<String>();
        for (String key : SPECIALIZATION_KEYS) {
            suggestions.add(SeedText.resolve(key));
        }

        if (garage != null) {
            for (ServiceItem s : garage.getServiceItems()) {
                String serviceName = SeedText.resolve(s.getName());
                if (serviceName != null && !serviceName.trim().isEmpty() && !suggestions.contains(serviceName.trim()))
                    suggestions.add(serviceName.trim());
            }
        }
        return suggestions;
    }

    /** The key when the text is a fixed choice, otherwise the text the user typed. */
    public static String specializationToStore(String chosen) {
        String text = chosen == null ? "" : chosen.trim();
        if (text.isEmpty()) {
            return SPECIALIZATION_KEYS[0];
        }
        for (String key : SPECIALIZATION_KEYS) {
            if (isOneOfOurChoices(key, text)) {
                return key;
            }
        }
        return text;
    }

    private static boolean isOneOfOurChoices(String key, String text) {
        if (text.equalsIgnoreCase(SeedText.get(key))) {
            return true;
        }
        for (String lang : new String[] {SeedText.LANG_SV, SeedText.LANG_EN}) {
            String value = SeedText.loadDictionary(lang).get(key);
            if (value != null && text.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String chosenSpecialization(ComboBox<String> box) {
        String typed = box.getEditor().getText();
        if (typed != null && !typed.trim().isEmpty()) {
            return typed.trim();
        }
        return box.getValue() != null ? box.getValue().trim() : "";
    }

    private static ComboBox<String> createSpecializationBox(GarageSystem garage, String currentSpec) {
        ComboBox<String> box = new ComboBox<String>();
        box.setEditable(true);
        box.setPromptText(I18n.get("dialog.mechanic.spec_prompt"));

        box.getItems().addAll(suggestSpecializations(garage));
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

        Alert alert = ActionDialogs.confirm(I18n.get("dialog.mechanic.delete.title"),
                I18n.get("dialog.mechanic.delete.header"),
                I18n.get("dialog.mechanic.delete.confirm", mechanic.getName()));

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
