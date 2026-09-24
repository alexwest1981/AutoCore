package com.wac.autocore.ui.util;

import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
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

    public static String serviceName(GarageSystem garage, int id) {
        if (garage == null || id <= 0) {
            return "-";
        }
        for (ServiceItem s : garage.getServiceItems()) {
            if (s.getId() == id) {
                return s.getName();
            }
        }
        return "Service #" + id;
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

    public static String bookingVehicleReg(GarageSystem garage, int bookingId) {
        if (garage == null) return "Booking #" + bookingId;
        for (com.wac.autocore.model.Booking b : garage.getBookings()) {
            if (b.getId() == bookingId) {
                return vehicleReg(garage, b.getVehicleId());
            }
        }
        return "Booking #" + bookingId;
    }

    public static String bookingCustomerName(GarageSystem garage, int bookingId) {
        if (garage == null) return "-";
        for (com.wac.autocore.model.Booking b : garage.getBookings()) {
            if (b.getId() == bookingId) {
                for (Vehicle v : garage.getVehicles()) {
                    if (v.getId() == b.getVehicleId()) {
                        return customerName(garage, v.getCustomerId());
                    }
                }
            }
        }
        return "-";
    }

    public static String workOrderVehicleReg(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return "-";
        return bookingVehicleReg(garage, wo.getBookingId());
    }

    public static String workOrderCustomerName(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return "-";
        return bookingCustomerName(garage, wo.getBookingId());
    }

    public static double workOrderTotal(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null || wo.getServiceItemIds() == null) return 0.0;
        double total = 0.0;
        for (Integer sid : wo.getServiceItemIds()) {
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == sid) {
                    total += s.getPrice();
                    break;
                }
            }
        }
        return total;
    }

    public static String invoiceCustomerName(GarageSystem garage, Invoice inv) {
        if (garage == null || inv == null) return "-";
        for (WorkOrder wo : garage.getWorkOrders()) {
            if (wo.getId() == inv.getWorkOrderId()) {
                return workOrderCustomerName(garage, wo);
            }
        }
        return "-";
    }
}
