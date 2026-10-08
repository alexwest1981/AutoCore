package com.wac.autocore.ui;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;
import com.wac.autocore.ui.util.UiFormatters;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import com.wac.autocore.seed.SeedText;

/** Marks which jobs on the order are done. The price is frozen at the same moment. */
final class MarkPerformedDialog {

    private MarkPerformedDialog() {}

    static void show(GarageSystem garage, WorkOrder workOrder, Runnable onSuccess) {
        if (workOrder == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle(I18n.get("dialog.workorder.mark_performed_title") + " #" + workOrder.getId());
        dialog.setHeaderText(I18n.get("dialog.workorder.mark_performed_header"));
        ActionDialogs.styleDialog(dialog);

        VBox content = new VBox(12);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(580);

        VBox serviceBox = new VBox(6);
        List<CheckBox> boxes = new ArrayList<CheckBox>();
        for (Integer serviceItemId : workOrder.getServiceItemIds()) {
            String name = "Service #" + serviceItemId;
            double price = 0.0;
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == serviceItemId.intValue()) {
                    name = SeedText.resolve(s.getName());
                    price = s.getPrice();
                    break;
                }
            }
            CheckBox box = new CheckBox(name + " (" + UiFormatters.formatMoney(price) + ")");

            boolean alreadyDone = workOrder.getCompletedServiceItems().contains(serviceItemId);
            Double frozen = workOrder.getCompletedServicePrice(serviceItemId.intValue());
            if (alreadyDone && frozen != null) {
                box.setText(name + " (" + UiFormatters.formatMoney(frozen.doubleValue()) + ")");
            }

            box.setUserData(serviceItemId);
            box.setSelected(alreadyDone);
            box.setDisable(alreadyDone);
            boxes.add(box);
            serviceBox.getChildren().add(box);
        }

        content.getChildren().add(serviceBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<Integer> selected = new ArrayList<Integer>();
                for (CheckBox box : boxes) {
                    if (box.isSelected()) {
                        selected.add((Integer) box.getUserData());
                    }
                }
                if (selected.isEmpty()) {
                    ActionDialogs.showError(I18n.get("dialog.confirm.title"), I18n.get("dialog.validation.required"));
                    return;
                }

                int[] ids = new int[selected.size()];
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = selected.get(i);
                }

                garage.markServicesAsCompleted(workOrder.getId(), ids);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
        });
    }
}
