package com.wac.autocore.ui.views;

import com.wac.autocore.model.Booking;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.i18n.I18n;

import java.util.List;

final class PageFormatters {

    private PageFormatters() {
    }

    static String meta(String key, int count) {
        return I18n.get(count == 1 ? key + ".one" : key, count);
    }

    static String bookingMechanicNames(GarageSystem garage, Booking booking) {
        List<Integer> ids = booking.getMechanicIds();
        StringBuilder names = new StringBuilder();
        for (Integer id : ids) {
            if (id == null || id.intValue() <= 0) {
                continue;
            }
            String name = EntityLookup.mechanicName(garage, id.intValue());
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(name);
        }
        return names.length() == 0 ? "-" : names.toString();
    }
}
