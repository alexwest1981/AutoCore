package com.wac.autocore.ui;

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

/**
 * Modala dialoger för mekanikerhantering (skapa, redigera, ta bort).
 */
public final class MechanicDialogs {

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

                String problem = Mechanic.validationProblem(name, phone);
                if (problem != null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation." + problem));
                    return;
                }

                try {
                    garage.createMechanic(name, phone, specializationToStore(chosenSpecialization(specBox)));
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                } catch (IllegalArgumentException rejected) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), rejected.getMessage());
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

                String problem = Mechanic.validationProblem(name, phone);
                if (problem != null) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation." + problem));
                    return;
                }

                mechanic.setName(name);
                mechanic.setPhone(phone);
                mechanic.setSpecialization(specializationToStore(chosenSpecialization(specBox)));
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

    /** De fasta specialiseringarna, som nycklar i demodatans ordlista. Nyckeln sparas, texten visas. */
    private static final String[] SPECIALIZATION_KEYS = {
        "seed.mechanic.general_service.specialization",
        "seed.mechanic.brakes.specialization",
        "seed.mechanic.diagnostics.specialization",
        "seed.mechanic.wheels.specialization",
        "seed.mechanic.engine.specialization",
        "seed.mechanic.climate.specialization"
    };

    /**
     * Förslagen som visas i rullistan för specialisering. Offentlig och utan JavaFX så att provet kan
     * läsa exakt den lista användaren får: den innehöll tidigare tre svenska texter som låg fast i
     * koden och därför stod kvar på svenska även när gränssnittet kördes på engelska.
     */
    public static List<String> suggestSpecializations(GarageSystem garage) {
        List<String> suggestions = new ArrayList<String>();
        for (String key : SPECIALIZATION_KEYS) {
            suggestions.add(SeedText.resolve(key));
        }

        if (garage != null) {
            for (ServiceItem s : garage.getServiceItems()) {
                String serviceName = SeedText.resolve(s.getName());
                if (serviceName != null && !serviceName.trim().isEmpty() && !suggestions.contains(serviceName.trim())) {
                    suggestions.add(serviceName.trim());
                }
            }
        }
        return suggestions;
    }

    /**
     * Värdet som ska sparas: nyckeln när texten är ett av våra fasta val, annars det användaren skrev.
     * Sparas texten blir den kvar i det språk den skrevs i och visas oöversatt efter ett språkbyte.
     */
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
