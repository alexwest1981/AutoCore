package com.wac.autocore.ui;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.components.MultiSelectComboBox;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;


import java.util.ArrayList;

public final class ServicePackageDialogs {

    private ServicePackageDialogs() {
    }

    public static void showCreatePackageDialog(GarageSystem garage, Runnable onSuccess) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.package.create.title"));
        dialog.setHeaderText(I18n.get("dialog.package.create.header"));
        ActionDialogs.styleDialog(dialog);

        TextField nameField = new TextField();
        nameField.setPromptText(I18n.get("dialog.package.name_prompt"));
        TextField descField = new TextField();
        descField.setPromptText(I18n.get("dialog.package.desc_prompt"));

        MultiSelectComboBox<ServiceItem> serviceBox = new MultiSelectComboBox<ServiceItem>(
                I18n.get("dialog.package.service_select"),
                s -> SeedText.resolve(s.getName()));
        serviceBox.setItems(garage.getServiceItems());
        serviceBox.setKeyProvider(s -> s.getId());

        GridPane grid = ActionDialogs.createGrid();
        grid.add(new Label(I18n.get("table.col.name") + ":"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18n.get("table.col.description") + ":"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label(I18n.get("dialog.package.services_label") + ":"), 0, 2);
        grid.add(serviceBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ActionDialogs.requireFilled(dialog, nameField);

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            try {
                garage.createServicePackage(
                        nameField.getText(),
                        descField.getText().trim(),
                        new ArrayList<ServiceItem>(serviceBox.getSelectedItems()));
            } catch (IllegalArgumentException e) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), e.getMessage());
                return;
            }
            if (onSuccess != null) onSuccess.run();
        });
    }

    public static void showPackagesDialog(GarageSystem garage) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.package.list.title"));
        ActionDialogs.styleDialog(dialog);

        ListView<String> list = new ListView<String>();
        for (ServicePackage servicePackage : garage.getServicePackages()) {
            list.getItems().add(describe(servicePackage));
        }
        list.setPlaceholder(new Label(I18n.get("dialog.package.list.empty")));
        list.setPrefSize(520, 300);

        dialog.getDialogPane().setContent(list);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    /** Dialoger för servicepaket: skapa ett paket och visa de som finns. */
    private static String describe(ServicePackage servicePackage) {
        StringBuilder names = new StringBuilder();
        for (ServiceItem item : servicePackage.getServiceItems()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(SeedText.resolve(item.getName()));
        }
        return servicePackage.getName()
                + "  .  " + UiFormatters.formatMoney(servicePackage.getTotalPrice())
                + "  .  " + servicePackage.getTotalEstimatedMinutes() + " min\n"
                + names;
    }

}
