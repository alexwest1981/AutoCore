package com.wac.autocore.ui;

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

import java.sql.SQLException;

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

        TextField nameField = ActionDialogs.field("dialog.customer.name_prompt", editing ? existing.getName() : null);
        TextField phoneField = ActionDialogs.field("dialog.customer.phone_prompt", editing ? existing.getPhone() : null);
        TextField emailField = ActionDialogs.field("dialog.customer.email_prompt", editing ? existing.getEmail() : null);

        CheckBox vipBox = new CheckBox(I18n.get("table.col.vip"));
        if (editing) {
            vipBox.setSelected(existing.isVip());
        }

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("dialog.customer.email") + ":"), 0, 2);
        grid.add(emailField, 1, 2);
        // The vip flag belongs to a customer that already exists, so the create form leaves it out.
        if (editing) {
            grid.add(new Label(I18n.get("table.col.vip") + ":"), 0, 3);
            grid.add(vipBox, 1, 3);
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
                if (editing) {
                    existing.setName(name);
                    existing.setPhone(phone);
                    existing.setEmail(email);
                    existing.setVip(vipBox.isSelected());
                    garage.updateCustomer(existing);
                } else {
                    garage.createCustomer(name, phone, email);
                }
            } catch (SQLException e) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
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
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }
                if (onSuccess != null) onSuccess.run();
            }
        });
    }
}
