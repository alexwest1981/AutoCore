package com.wac.autocore.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

class BookingPackagePicker {

    private final ComboBox<ServicePackage> box = new ComboBox<ServicePackage>();
    /** The packages picked so far. Their services belong to the booking, but not to the service
     *  picker: that field is for the services booked on top of a package. */
    private final ObservableList<ServicePackage> chosen = FXCollections.observableArrayList();
    private Runnable onChanged;

    BookingPackagePicker(GarageSystem garage, boolean locked) {
        box.getItems().addAll(garage.getServicePackages());
        box.setPromptText(I18n.get("dialog.booking.package_prompt"));
        box.setMaxWidth(Double.MAX_VALUE);

        box.valueProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                return;
            }
            if (!chosen.contains(selected)) {
                chosen.add(selected);
            }
            if (onChanged != null) {
                onChanged.run();
            }
        });

        if (locked || box.getItems().isEmpty()) {
            box.setDisable(true);
        }
    }

    /** Runs when a package is picked, so the form can block that package's services in the picker. */
    void setOnChanged(Runnable listener) {
        this.onChanged = listener;
    }

    ComboBox<ServicePackage> getBox() {
        return box;
    }

    ObservableList<ServicePackage> getChosen() {
        return chosen;
    }
}
