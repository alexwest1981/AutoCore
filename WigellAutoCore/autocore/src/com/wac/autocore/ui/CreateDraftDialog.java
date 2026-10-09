package com.wac.autocore.ui;

import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** The dialog that creates a draft: the vehicle and the description are enough. */
final class CreateDraftDialog {

    private CreateDraftDialog() {}

    static void show(GarageSystem garage, Runnable onSuccess) {
        if (garage.getVehicles().isEmpty()) {
            ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.draft.no_vehicles"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.draft.title"));
        dialog.setHeaderText(I18n.get("dialog.draft.header"));
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setPrefWidth(560);

        GridPane grid = ActionDialogs.createGrid();

        ComboBox<Vehicle> vehicleBox = new ComboBox<Vehicle>();
        vehicleBox.getItems().addAll(garage.getVehicles());
        vehicleBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(vehicleBox, Priority.ALWAYS);
        vehicleBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                if (v == null) return "";
                String owner = EntityLookup.customerName(garage, v.getCustomerId());
                return v.getId() + " - " + v.getRegistrationNumber() + " (" + v.getBrand() + " " + v.getModel() + ") · " + owner;
            }

            @Override
            public Vehicle fromString(String text) {
                return null;
            }
        });
        vehicleBox.setPromptText(I18n.get("dialog.booking.vehicle_select"));

        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        description.setWrapText(true);

        grid.add(new Label(I18n.get("table.col.vehicle") + ":"), 0, 0);
        grid.add(vehicleBox, 1, 0);
        grid.add(new Label(I18n.get("dialog.draft.description") + ":"), 0, 1);
        grid.add(description, 1, 1);

        content.getChildren().add(grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // OK is locked until both vehicle and description are filled in.
        ActionDialogs.requireFilled(dialog, vehicleBox, description);

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            Vehicle v = vehicleBox.getValue();
            if (v == null) {
                return;
            }
            if (garage.createDraft(v.getId(), description.getText()) == null) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.draft.create_failed"));
                return;
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }
}
