package com.wac.autocore.ui;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.EntityLookup;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** The dialog that creates a reclamation: the description of the fault is enough, the rest is inherited from the order. */
final class CreateReclamationDialog {

    private CreateReclamationDialog() {}

    static void show(GarageSystem garage, WorkOrder original, Runnable onSuccess) {
        if (original == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.reclamation.title"));
        dialog.setHeaderText(I18n.get("dialog.reclamation.header") + " #" + original.getId());
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(14);
        content.setPadding(new Insets(16, 20, 16, 20));
        content.setPrefWidth(560);

        // What the reclamation concerns is spelled out, so you can see the right order was picked.
        Label originalLabel = new Label(
                I18n.get("table.col.booking") + ": #" + original.getBookingId()
                        + " · " + I18n.get("table.col.vehicle") + ": " + EntityLookup.workOrderVehicleReg(garage, original)
                        + " · " + I18n.get("table.col.services") + ": " + EntityLookup.workOrderServices(garage, original));
        originalLabel.setWrapText(true);
        originalLabel.getStyleClass().add("srow-sub");

        GridPane grid = ActionDialogs.createGrid();

        TextArea description = new TextArea();
        description.setPrefRowCount(3);
        description.setWrapText(true);
        description.setPromptText(I18n.get("dialog.reclamation.description_prompt"));

        grid.add(new Label(I18n.get("dialog.reclamation.description") + ":"), 0, 0);
        grid.add(description, 1, 0);

        content.getChildren().addAll(originalLabel, grid);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // OK is locked until the description is filled in, since a reclamation with no case cannot be followed up.
        ActionDialogs.requireFilled(dialog, description);

        dialog.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }
            if (garage.createReclamation(original.getId(), description.getText()) == null) {
                ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.reclamation.create_failed"));
                return;
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }
}
