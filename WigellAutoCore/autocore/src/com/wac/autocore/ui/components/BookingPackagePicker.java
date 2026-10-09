package com.wac.autocore.ui.components;

import javafx.collections.ListChangeListener;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.i18n.I18n;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The packages are picked as chips, the same way the services are, so more than one package can sit
 * on a booking and each is taken off with its own cross. Their services belong to the booking, but
 * not to the service picker: that field is for the services booked on top of a package.
 */
class BookingPackagePicker {

    private final List<ServicePackage> items;
    private final MultiSelectComboBox<ServicePackage> multi;
    private Runnable onChanged;

    BookingPackagePicker(GarageSystem garage, boolean locked) {

        this.items = garage.getServicePackages();

        this.multi = new MultiSelectComboBox<ServicePackage>(
                I18n.get("dialog.booking.package_prompt"),
                pkg -> SeedText.resolve(pkg.getName()));

        multi.setItems(items);
        multi.setKeyProvider(pkg -> pkg.getId());

        multi.getSelectedItems().addListener((ListChangeListener<ServicePackage>) c -> {

            if (onChanged != null) {
                onChanged.run();
            }
        });

        if (locked || items.isEmpty()) {
            multi.setDisable(true);
        }
    }

    /** Runs when the picked packages change, so the form can redraw what follows from them. */
    void setOnChanged(Runnable listener) {
        this.onChanged = listener;
    }

    /** The services on the booking, whoever put them there. A package that holds nothing but those
     *  services is greyed out, since picking it would add nothing. A package that is already picked
     *  is never greyed: it is what put its own services on the booking. */
    void setServicesOnBooking(List<ServiceItem> services) {

        Set<Integer> chosenIds = new HashSet<Integer>();

        for (ServicePackage pkg : multi.getSelectedItems()) {
            chosenIds.add(pkg.getId());
        }

        Set<Integer> onBooking = new HashSet<Integer>();

        for (ServiceItem item : services) {
            onBooking.add(item.getId());
        }

        List<ServicePackage> covered = new ArrayList<ServicePackage>();

        for (ServicePackage pkg : items) {

            if (pkg.getServiceItems().isEmpty() || chosenIds.contains(pkg.getId())) {
                continue;
            }

            boolean allThere = true;

            for (ServiceItem item : pkg.getServiceItems()) {

                if (!onBooking.contains(item.getId())) {
                    allThere = false;
                    break;
                }
            }

            if (allThere) {
                covered.add(pkg);
            }
        }

        multi.setBlockedItems(covered);
    }

    /** Drops a picked package. Its services leave the booking with it, since they are on it only
     *  because the package is. */
    void remove(ServicePackage picked) {
        multi.removeSelectedItem(picked);
    }

    MultiSelectComboBox<ServicePackage> getMulti() {
        return multi;
    }

    List<ServicePackage> getChosen() {
        return new ArrayList<ServicePackage>(multi.getSelectedItems());
    }
}
