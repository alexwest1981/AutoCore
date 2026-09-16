package com.wac.autocore.ui.util;

import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;

import java.util.List;

/**
 * Hjälpmetoder för att slå upp läsbara namn på relaterade entiteter via ID.
 */
public final class EntityLookup {

    private EntityLookup() {}

    public static String customerName(GarageSystem garage, int id) {
        if (garage == null) {
            return "Customer #" + id;
        }
        for (Customer c : garage.getCustomers()) {
            if (c.getId() == id) {
                return c.getName();
            }
        }
        return "Customer #" + id;
    }

    public static String vehicleReg(GarageSystem garage, int id) {
        if (garage == null) {
            return "Vehicle #" + id;
        }
        for (Vehicle v : garage.getVehicles()) {
            if (v.getId() == id) {
                return v.getRegistrationNumber();
            }
        }
        return "Vehicle #" + id;
    }

    public static String mechanicName(GarageSystem garage, int id) {
        if (garage == null) {
            return "Mechanic #" + id;
        }
        for (Mechanic m : garage.getMechanics()) {
            if (m.getId() == id) {
                return m.getName();
            }
        }
        return "Mechanic #" + id;
    }

    public static String serviceNames(GarageSystem garage, List<Integer> ids) {
        if (ids == null || ids.isEmpty() || garage == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer sid : ids) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String found = null;
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == sid) {
                    found = s.getName();
                    break;
                }
            }
            sb.append(found != null ? found : "Service #" + sid);
        }
        return sb.toString();
    }
}
