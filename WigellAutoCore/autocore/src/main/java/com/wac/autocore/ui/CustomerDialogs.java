package com.wac.autocore.ui;

import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;


/** Dialogs for managing customers. */
public final class CustomerDialogs {

    private CustomerDialogs() {}

    public static void showCreateCustomerDialog(GarageSystem garage, Runnable onSuccess) {
        showCustomerForm(garage, null, onSuccess);
    }

    public static void showEditCustomerDialog(GarageSystem garage, Customer customer, Runnable onSuccess) {
        if (customer == null) return;

        showCustomerForm(garage, customer, onSuccess);
    }

    /** One form for both, because create and edit ask for the same three fields. */
    private static void showCustomerForm(GarageSystem garage, Customer existing, Runnable onSuccess) {
        boolean editing = existing != null;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get(editing ? "dialog.customer.edit.title" : "dialog.customer.create.title"));
        dialog.setHeaderText(I18n.get(editing ? "dialog.customer.edit.header" : "dialog.customer.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = ActionDialogs.field("dialog.customer.name_prompt", existing != null ? existing.getName() : null);
        TextField phoneField = ActionDialogs.field("dialog.customer.phone_prompt", existing != null ? existing.getPhone() : null);
        TextField emailField = ActionDialogs.field("dialog.customer.email_prompt", existing != null ? existing.getEmail() : null);
        TextField addressField = ActionDialogs.field("dialog.customer.address_prompt", existing != null ? existing.getAddress() : null);
        TextField postalField = ActionDialogs.field("dialog.customer.postal_code_prompt",
                existing != null ? existing.getPostalCode() : null);
        TextField cityField = ActionDialogs.field("dialog.customer.city_prompt", existing != null ? existing.getCity() : null);

        CheckBox vipBox = new CheckBox(I18n.get("table.col.vip"));
        if (existing != null) {
            vipBox.setSelected(existing.isVip());
        }

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("dialog.customer.email") + ":"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label(I18n.get("dialog.customer.address") + ":"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label(I18n.get("dialog.customer.postal_code") + ":"), 0, 4);
        grid.add(postalField, 1, 4);
        grid.add(new Label(I18n.get("dialog.customer.city") + ":"), 0, 5);
        grid.add(cityField, 1, 5);
        // The vip flag belongs to a customer that already exists, so the create form leaves it out.
        if (editing) {
            grid.add(new Label(I18n.get("table.col.vip") + ":"), 0, 6);
            grid.add(vipBox, 1, 6);
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
            String email = emailField.getText().trim();

            String problem = Customer.validationProblem(name, phone, email);
            if (problem != null) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation." + problem));
                return;
            }

            try {
                if (existing != null) {
                    existing.setName(name);
                    existing.setPhone(phone);
                    existing.setEmail(email);
                    existing.setAddress(addressField.getText().trim());
                    existing.setPostalCode(postalField.getText().trim());
                    existing.setCity(cityField.getText().trim());
                    existing.setVip(vipBox.isSelected());
                    garage.updateCustomer(existing);
                } else {
                    // The address is part of the customer and not a detail on the invoice, so it is
                    // saved with the row itself. The creation rule is unchanged.
                    Customer created = garage.createCustomer(name, phone, email);
                    created.setAddress(addressField.getText().trim());
                    created.setPostalCode(postalField.getText().trim());
                    created.setCity(cityField.getText().trim());
                    garage.updateCustomer(created);
                }
            } catch (DataAccessException e) {
                ActionDialogs.showAppError(e);
                return;
            }

            if (onSuccess != null) onSuccess.run();
        });
    }

    public static void showDeleteCustomerConfirmation(GarageSystem garage, Customer customer, Runnable onSuccess) {
        if (customer == null) return;

        if (!garage.canDeleteCustomer(customer.getId())) {
            ActionDialogs.showError(I18n.get("dialog.customer.delete.title"),
                    I18n.get("dialog.customer.delete.has_active_orders", customer.getName()));
            return;
        }

        Alert alert = ActionDialogs.confirm(I18n.get("dialog.customer.delete.title"),
                I18n.get("dialog.customer.delete.header"),
                I18n.get("dialog.customer.delete.confirm", customer.getName()));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    garage.deleteCustomer(customer.getId());
                } catch (DataAccessException e) {
                    ActionDialogs.showAppError(e);
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
