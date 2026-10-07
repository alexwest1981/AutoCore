package com.wac.autocore.ui.components;

import javafx.scene.control.ComboBox;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

class BookingPackagePicker {

    private final ComboBox<ServicePackage> box = new ComboBox<ServicePackage>();

    BookingPackagePicker(GarageSystem garage, MultiSelectComboBox<ServiceItem> serviceMulti, boolean locked) {
        box.getItems().addAll(garage.getServicePackages());
        box.setPromptText(I18n.get("dialog.booking.package_prompt"));
        box.setMaxWidth(Double.MAX_VALUE);

        box.valueProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                return;
            }
            for (ServiceItem item : selected.getServiceItems()) {
                serviceMulti.addSelectedItem(item);
            }
        });

        if (locked || box.getItems().isEmpty()) {
            box.setDisable(true);
        }
    }

    ComboBox<ServicePackage> getBox() {
        return box;
    }

    ServicePackage getSelected() {
        return box.getValue();
    }
}
