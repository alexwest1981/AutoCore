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

/**
 * Modala dialoger för kundhantering (skapa, redigera, ta bort).
 */
public final class CustomerDialogs {

    private CustomerDialogs() {}

    public static void showCreateCustomerDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.customer.create.title"));
        dialog.setHeaderText(I18n.get("dialog.customer.create.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.customer.name_prompt"));
        TextField phoneField = new TextField();
        phoneField.setPromptText(I18n.get("dialog.customer.phone_prompt"));
        TextField emailField = new TextField();
        emailField.setPromptText(I18n.get("dialog.customer.email_prompt"));

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("dialog.customer.email") + ":"), 0, 2);
        grid.add(emailField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String email = emailField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                garage.createCustomer(name, phone, email);
                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showEditCustomerDialog(GarageSystem garage, Customer customer, Runnable onSuccess) {
        if (customer == null) return;

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.customer.edit.title"));
        dialog.setHeaderText(I18n.get("dialog.customer.edit.header"));
        ActionDialogs.styleDialog(dialog);

        GridPane grid = ActionDialogs.createGrid();

        TextField nameField = new TextField(customer.getName());
        nameField.setPromptText(I18n.get("dialog.customer.name_prompt"));
        TextField phoneField = new TextField(customer.getPhone() != null ? customer.getPhone() : "");
        phoneField.setPromptText(I18n.get("dialog.customer.phone_prompt"));
        TextField emailField = new TextField(customer.getEmail() != null ? customer.getEmail() : "");
        emailField.setPromptText(I18n.get("dialog.customer.email_prompt"));
        CheckBox vipBox = new CheckBox(I18n.get("table.col.vip"));
        vipBox.setSelected(customer.isVip());

        grid.add(new Label(I18n.get("dialog.customer.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("dialog.customer.phone") + ":"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label(I18n.get("dialog.customer.email") + ":"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label(I18n.get("table.col.vip") + ":"), 0, 3);
        grid.add(vipBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String email = emailField.getText().trim();

                if (name.isEmpty() || phone.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                customer.setName(name);
                customer.setPhone(phone);
                customer.setEmail(email);
                customer.setVip(vipBox.isSelected());

                try {
                    garage.updateCustomer(customer);
                } catch (SQLException e) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                    return;
                }

                if (onSuccess != null) onSuccess.run();
            }
        });
    }

    public static void showDeleteCustomerConfirmation(GarageSystem garage, Customer customer, Runnable onSuccess) {
        if (customer == null) return;

        if (!garage.canDeleteCustomer(customer.getId())) {
            ActionDialogs.showError(I18n.get("dialog.customer.delete.title"),
                    I18n.get("dialog.customer.delete.has_active_orders", customer.getName()));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.get("dialog.customer.delete.title"));
        alert.setHeaderText(I18n.get("dialog.customer.delete.header"));
        alert.setContentText(I18n.get("dialog.customer.delete.confirm", customer.getName()));
        ActionDialogs.styleDialog(alert);

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
