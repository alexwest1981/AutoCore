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

/** Dialogen som skapar en reklamation: beskrivningen av felet räcker, resten ärvs från ordern. */
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

        // Vad reklamationen gäller står i klartext, så man ser att rätt order valdes.
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

        // OK är låst tills beskrivningen är ifylld, för en reklamation utan ärende går inte att följa upp.
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
