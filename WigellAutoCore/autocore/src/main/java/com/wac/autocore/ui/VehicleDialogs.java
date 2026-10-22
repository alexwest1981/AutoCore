package com.wac.autocore.ui;

import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.util.List;

/** Dialogs for managing vehicles. */
public final class VehicleDialogs {

    private VehicleDialogs() {}

    public static void showCreateVehicleDialog(GarageSystem garage, Runnable onSuccess) {
        showVehicleForm(garage, null, onSuccess);
    }

    public static void showEditVehicleDialog(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        if (vehicle == null) return;
        showVehicleForm(garage, vehicle, onSuccess);
    }

    /** One form for both, because create and edit ask for the same fields. */
    private static void showVehicleForm(GarageSystem garage, Vehicle existing, Runnable onSuccess) {
        boolean editing = existing != null;

        List<Customer> customers = garage.getCustomers();
        if (!editing && customers.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get(editing ? "dialog.vehicle.edit.title" : "dialog.vehicle.create.title"));
        dialog.setHeaderText(I18n.get(editing ? "dialog.vehicle.edit.header" : "dialog.vehicle.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<Customer> customerBox = new ComboBox<Customer>();
        customerBox.getItems().addAll(customers);
        if (existing != null) {
            for (Customer c : customers) {
                if (c.getId() == existing.getCustomerId()) {
                    customerBox.getSelectionModel().select(c);
                    break;
                }
            }
        } else {
            // Shows its prompt until an owner is picked, the same as the fields in the booking form.
            customerBox.setPromptText(I18n.get("dialog.vehicle.customer_select"));
        }
        customerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getId() + " - " + c.getName() + " (" + c.getPhone() + ")";
            }
            @Override
            public Customer fromString(String string) { return null; }
        });

        TextField regField = ActionDialogs.field("dialog.vehicle.reg_prompt",
                existing != null ? existing.getRegistrationNumber() : null);
        TextField brandField = ActionDialogs.field("dialog.vehicle.brand_prompt", existing != null ? existing.getBrand() : null);
        TextField modelField = ActionDialogs.field("dialog.vehicle.model_prompt", existing != null ? existing.getModel() : null);
        TextField yearField = ActionDialogs.field("dialog.vehicle.year_prompt",
                existing != null ? String.valueOf(existing.getYear()) : null);

        grid.add(new Label(I18n.get("dialog.vehicle.customer_select") + ":"), 0, 0);
        grid.add(customerBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.vehicle.reg_nr") + ":"), 0, 1);
        grid.add(regField, 1, 1);
        grid.add(new Label(I18n.get("dialog.vehicle.brand") + ":"), 0, 2);
        grid.add(brandField, 1, 2);
        grid.add(new Label(I18n.get("dialog.vehicle.model") + ":"), 0, 3);
        grid.add(modelField, 1, 3);
        grid.add(new Label(I18n.get("dialog.vehicle.year") + ":"), 0, 4);
        grid.add(yearField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, customerBox, regField, brandField, modelField, yearField);

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }

            Customer owner = customerBox.getValue();
            String reg = regField.getText().trim().toUpperCase();
            String brand = brandField.getText().trim();
            String model = modelField.getText().trim();
            String yearStr = yearField.getText().trim();

            if (!Vehicle.isReadableYear(yearStr)) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                        I18n.get("dialog.validation.invalid_year"));
                return;
            }

            int year = Integer.parseInt(yearStr.trim());

            try {
                if (existing != null) {
                    existing.setCustomerId(owner.getId());
                    existing.setRegistrationNumber(reg);
                    existing.setBrand(brand);
                    existing.setModel(model);
                    existing.setYear(year);
                    garage.updateVehicle(existing);
                } else {
                    garage.createVehicle(reg, brand, model, year, owner.getId());
                }
            } catch (RuleViolationException refused) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get(refused.getMessageKey()));
                return;
            } catch (DataAccessException e) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                return;
            }

            if (onSuccess != null) onSuccess.run();
        });
    }

    public static void showDeleteVehicleConfirmation(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        if (vehicle == null) return;

        if (!garage.canDeleteVehicle(vehicle.getId())) {
            ActionDialogs.showError(I18n.get("dialog.vehicle.delete.title"),
                    I18n.get("dialog.vehicle.delete.has_active_orders", vehicle.getRegistrationNumber()));
            return;
        }

        Alert alert = ActionDialogs.confirm(I18n.get("dialog.vehicle.delete.title"),
                I18n.get("dialog.vehicle.delete.header"),
                I18n.get("dialog.vehicle.delete.confirm", vehicle.getRegistrationNumber()));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteVehicle(vehicle.getId());
                } catch (DataAccessException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
