package com.wac.autocore.ui;

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modala dialoger för fordonshantering (skapa, redigera, ta bort).
 */
public final class VehicleDialogs {

    private VehicleDialogs() {}

    public static void showCreateVehicleDialog(GarageSystem garage, Runnable onSuccess) {
        List<Customer> customers = garage.getCustomers();
        if (customers.isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.vehicle.create.title"));
        dialog.setHeaderText(I18n.get("dialog.vehicle.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<Customer> customerBox = new ComboBox<Customer>();
        customerBox.getItems().addAll(customers);
        customerBox.getSelectionModel().selectFirst();
        customerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getId() + " - " + c.getName() + " (" + c.getPhone() + ")";
            }
            @Override
            public Customer fromString(String string) { return null; }
        });

        TextField regField = new TextField();
        regField.setPromptText(I18n.get("dialog.vehicle.reg_prompt"));
        TextField brandField = new TextField();
        brandField.setPromptText(I18n.get("dialog.vehicle.brand_prompt"));
        TextField modelField = new TextField();
        modelField.setPromptText(I18n.get("dialog.vehicle.model_prompt"));
        TextField yearField = new TextField();
        yearField.setPromptText(I18n.get("dialog.vehicle.year_prompt"));

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

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Customer owner = customerBox.getValue();
                String reg = regField.getText().trim().toUpperCase();
                String brand = brandField.getText().trim();
                String model = modelField.getText().trim();
                String yearStr = yearField.getText().trim();

                if (!validateVehicleInput(owner, reg, brand, model, yearStr)) {
                    return;
                }

                int year = Integer.parseInt(yearStr.trim());
                garage.createVehicle(reg, brand, model, year, owner.getId());
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditVehicleDialog(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        if (vehicle == null) return;

        List<Customer> customers = garage.getCustomers();
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.vehicle.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.vehicle.edit.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<Customer> customerBox = new ComboBox<Customer>();
        customerBox.getItems().addAll(customers);
        for (Customer c : customers) {
            if (c.getId() == vehicle.getCustomerId()) {
                customerBox.getSelectionModel().select(c);
                break;
            }
        }
        customerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getId() + " - " + c.getName() + " (" + c.getPhone() + ")";
            }
            @Override
            public Customer fromString(String string) { return null; }
        });

        TextField regField = new TextField(vehicle.getRegistrationNumber());
        regField.setPromptText(I18n.get("dialog.vehicle.reg_prompt"));
        TextField brandField = new TextField(vehicle.getBrand() != null ? vehicle.getBrand() : "");
        brandField.setPromptText(I18n.get("dialog.vehicle.brand_prompt"));
        TextField modelField = new TextField(vehicle.getModel() != null ? vehicle.getModel() : "");
        modelField.setPromptText(I18n.get("dialog.vehicle.model_prompt"));
        TextField yearField = new TextField(String.valueOf(vehicle.getYear()));
        yearField.setPromptText(I18n.get("dialog.vehicle.year_prompt"));

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

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Customer cust = customerBox.getValue();
                String reg = regField.getText().trim().toUpperCase();
                String brand = brandField.getText().trim();
                String model = modelField.getText().trim();
                String yearStr = yearField.getText().trim();

                if (!validateVehicleInput(cust, reg, brand, model, yearStr)) {
                    return;
                }

                int year = Integer.parseInt(yearStr.trim());
                vehicle.setCustomerId(cust.getId());
                vehicle.setRegistrationNumber(reg);
                vehicle.setBrand(brand);
                vehicle.setModel(model);
                vehicle.setYear(year);

                try {
                    garage.updateVehicle(vehicle);
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteVehicleConfirmation(GarageSystem garage, Vehicle vehicle, Runnable onSuccess) {
        if (vehicle == null) return;

        if (!garage.canDeleteVehicle(vehicle.getId())) {
            ActionDialogs.showError(I18n.get("dialog.vehicle.delete.title"),
                    I18n.get("dialog.vehicle.delete.has_active_orders", vehicle.getRegistrationNumber()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.vehicle.delete.title"));
        alert.setHeaderText(I18n.get("dialog.vehicle.delete.header"));
        alert.setContentText(I18n.get("dialog.vehicle.delete.confirm", vehicle.getRegistrationNumber()));
        ActionDialogs.styleDialog(alert);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteVehicle(vehicle.getId());
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    private static boolean validateVehicleInput(Customer customer, String reg, String brand, String model, String yearStr) {
        List<String> missing = new ArrayList<String>();
        if (customer == null) {
            missing.add(I18n.get("dialog.vehicle.customer_select"));
        }
        if (reg == null || reg.trim().isEmpty()) {
            missing.add(I18n.get("dialog.vehicle.reg_nr"));
        }
        if (brand == null || brand.trim().isEmpty()) {
            missing.add(I18n.get("dialog.vehicle.brand"));
        }
        if (model == null || model.trim().isEmpty()) {
            missing.add(I18n.get("dialog.vehicle.model"));
        }
        if (yearStr == null || yearStr.trim().isEmpty()) {
            missing.add(I18n.get("dialog.vehicle.year"));
        }

        if (!missing.isEmpty()) {
            String missingList = String.join(", ", missing);
            ActionDialogs.showError(I18n.get("dialog.confirm.title"),
                    I18n.get("dialog.validation.missing_fields", missingList));
            return false;
        }

        try {
            int y = Integer.parseInt(yearStr.trim());
            if (y < 1900 || y > 2100) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_year"));
                return false;
            }
        } catch (NumberFormatException e) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.invalid_year"));
            return false;
        }

        return true;
    }
}
