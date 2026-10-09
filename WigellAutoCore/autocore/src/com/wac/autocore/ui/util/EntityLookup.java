package com.wac.autocore.ui.util;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

import java.util.List;
import com.wac.autocore.seed.SeedText;

/** Readable names for related records, looked up by id. */
public final class EntityLookup {

    private EntityLookup() {}

    public static String customerName(GarageSystem garage, int id) {
        Customer customer = garage == null ? null : garage.findCustomer(id);
        return customer == null ? "Customer #" + id : customer.getName();
    }

    public static String vehicleReg(GarageSystem garage, int id) {
        Vehicle vehicle = garage == null ? null : garage.findVehicle(id);
        return vehicle == null ? "Vehicle #" + id : vehicle.getRegistrationNumber();
    }

    public static String mechanicName(GarageSystem garage, int id) {
        if (id <= 0) {
            return "-";
        }
        Mechanic mechanic = garage == null ? null : garage.findMechanic(id);
        return mechanic == null ? "Mechanic #" + id : mechanic.getName();
    }

    public static String serviceName(GarageSystem garage, int id) {
        if (garage == null || id <= 0) {
            return "-";
        }
        ServiceItem service = garage.findServiceItem(id);
        return service == null ? "Service #" + id : SeedText.resolve(service.getName());
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
            ServiceItem service = garage.findServiceItem(sid.intValue());
            sb.append(service == null ? "Service #" + sid : SeedText.resolve(service.getName()));
        }
        return sb.toString();
    }

    public static String bookingServices(GarageSystem garage, com.wac.autocore.model.Booking b) {
        if (b == null) return "-";
        if (b.getServiceItems() != null && !b.getServiceItems().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ServiceItem s : b.getServiceItems()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(SeedText.resolve(s.getName()));
            }
            return sb.toString();
        }
        if (b.getServiceItemIds() != null && !b.getServiceItemIds().isEmpty()) {
            return serviceNames(garage, b.getServiceItemIds());
        }
        if (b.getServiceItemId() > 0) {
            return serviceName(garage, b.getServiceItemId());
        }
        return "-";
    }

    public static int bookingTotalMinutes(GarageSystem garage, com.wac.autocore.model.Booking b) {
        if (b == null) return 0;
        int min = b.getTotalEstimatedMinutes();
        if (min > 0) return min;
        if (garage != null && b.getServiceItemIds() != null) {
            for (int sid : b.getServiceItemIds()) {
                ServiceItem service = garage.findServiceItem(sid);
                if (service != null) {
                    min += service.getEstimatedMinutes();
                }
            }
        }
        return min;
    }

    public static double bookingTotalPrice(GarageSystem garage, com.wac.autocore.model.Booking b) {
        if (b == null) return 0.0;
        double cost = b.getTotalEstimatedCost();
        if (cost > 0.0) return cost;
        if (garage != null && b.getServiceItemIds() != null) {
            for (int sid : b.getServiceItemIds()) {
                ServiceItem service = garage.findServiceItem(sid);
                if (service != null) {
                    cost += service.getPrice();
                }
            }
        }
        return cost;
    }

    public static String bookingVehicleReg(GarageSystem garage, int bookingId) {
        if (garage == null) return "Booking #" + bookingId;
        Booking b = garage.findBooking(bookingId);
        return b == null ? "Booking #" + bookingId : vehicleReg(garage, b.getVehicleId());
    }

    public static String bookingCustomerName(GarageSystem garage, int bookingId) {
        if (garage == null) return "-";
        Booking b = garage.findBooking(bookingId);
        Vehicle v = b == null ? null : garage.findVehicle(b.getVehicleId());
        return v == null ? "-" : customerName(garage, v.getCustomerId());
    }

    /** The registration number of the car the invoice is for. The invoice belongs to a work order. */
    public static String invoiceVehicleReg(GarageSystem garage, Invoice invoice) {
        if (garage == null || invoice == null) return "-";
        int bookingId = bookingIdForWorkOrder(garage, invoice.getWorkOrderId());
        return bookingId > 0 ? bookingVehicleReg(garage, bookingId) : "-";
    }

    /** The registration number of the car the payment is for, via the invoice. */
    public static String paymentVehicleReg(GarageSystem garage, Payment payment) {
        if (garage == null || payment == null) return "-";
        Invoice invoice = garage.findInvoice(payment.getInvoiceId());
        return invoice == null ? "-" : invoiceVehicleReg(garage, invoice);
    }

    /** The booking number the work order belongs to, or a dash for a draft. */
    public static String workOrderBookingRef(WorkOrder wo) {
        if (wo == null || wo.getBookingId() <= 0) return "-";
        return String.valueOf(wo.getBookingId());
    }

    public static String workOrderVehicleReg(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return "-";
        if (wo.getVehicleId() > 0) return vehicleReg(garage, wo.getVehicleId());
        return bookingVehicleReg(garage, wo.getBookingId());
    }

    // The customer who owns the vehicle. A draft has no booking, only a vehicle.
    private static String vehicleCustomerName(GarageSystem garage, int vehicleId) {
        Vehicle v = garage.findVehicle(vehicleId);
        return v == null ? "-" : customerName(garage, v.getCustomerId());
    }

    public static String workOrderCustomerName(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return "-";
        if (wo.getVehicleId() > 0) return vehicleCustomerName(garage, wo.getVehicleId());
        return bookingCustomerName(garage, wo.getBookingId());
    }

    /** The booking's chosen time as text, or a dash when no time is chosen. */
    public static String bookingTime(Booking b) {
        if (b == null || b.getStartTime() == null) return "-";
        if (b.getEndTime() == null) return b.getStartTime().toString();
        return b.getStartTime() + " - " + b.getEndTime();
    }

    /** The work order's date, taken from the booking it was created from. */
    public static String workOrderDate(GarageSystem garage, WorkOrder wo) {
        Booking b = bookingForWorkOrder(garage, wo);
        return b == null || b.getDate() == null ? "-" : String.valueOf(b.getDate());
    }

    /** The work order's time, taken from the booking it was created from. */
    public static String workOrderTime(GarageSystem garage, WorkOrder wo) {
        return bookingTime(bookingForWorkOrder(garage, wo));
    }

    /** The booking the work order belongs to, or null when there is none. */
    private static Booking bookingForWorkOrder(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null || wo.getBookingId() <= 0) return null;
        return garage.findBooking(wo.getBookingId());
    }

    /** The booking the work order belongs to, or 0 if the work order does not exist. */
    public static int bookingIdForWorkOrder(GarageSystem garage, int workOrderId) {
        if (garage == null || workOrderId <= 0) return 0;
        WorkOrder order = garage.findWorkOrder(workOrderId);
        return order == null ? 0 : order.getBookingId();
    }

    /** The invoice covering the work order. It belongs to the booking, not to one order. */
    public static Invoice invoiceForWorkOrder(GarageSystem garage, int workOrderId) {
        if (garage == null || workOrderId <= 0) return null;

        WorkOrder order = garage.findWorkOrder(workOrderId);
        if (order == null) return null;

        List<Integer> performed = order.getCompletedServiceItems();
        if (performed.isEmpty()) {
            performed = order.getServiceItemIds();
        }

        for (Invoice inv : garage.getInvoices()) {
            if (inv.getWorkOrderId() == workOrderId) {
                return inv;
            }
        }

        // The invoice may sit on another work order in the same booking,
        // so rows from other bookings must not be counted in here.
        for (Invoice inv : garage.getInvoices()) {
            if (bookingIdForWorkOrder(garage, inv.getWorkOrderId()) != order.getBookingId()) {
                continue;
            }
            for (InvoiceLine line : inv.getLines()) {
                if (line.getServiceItemId() > 0
                        && performed.contains(Integer.valueOf(line.getServiceItemId()))) {
                    return inv;
                }
            }
        }
        return null;
    }

    public static double workOrderServicePrice(GarageSystem garage, WorkOrder wo, int serviceItemId) {
        if (garage == null) return 0.0;
        if (wo != null) {
            // The frozen price is the price that applied when the work was done, and it is
            // shown even before the invoice exists. Same order as in the details dialog:
            // frozen price, then the invoice line's price, last the catalogue.
            Double frozenPrice = wo.getCompletedServicePrice(serviceItemId);
            if (frozenPrice != null) {
                return frozenPrice.doubleValue();
            }
            Invoice inv = invoiceForWorkOrder(garage, wo.getId());
            if (inv != null && inv.getLines() != null) {
                for (InvoiceLine line : inv.getLines()) {
                    if (line.getServiceItemId() == serviceItemId) {
                        return line.getPrice();
                    }
                }
            }
        }
        ServiceItem service = garage.findServiceItem(serviceItemId);
        return service == null ? 0.0 : service.getPrice();
    }

    /** The service names on a work order, without prices. The view is for the mechanics. */
    public static String workOrderServices(GarageSystem garage, WorkOrder wo) {
        if (wo == null || wo.getServiceItemIds() == null || wo.getServiceItemIds().isEmpty() || garage == null) {
            return "-";
        }
        Invoice invoice = invoiceForWorkOrder(garage, wo.getId());
        StringBuilder sb = new StringBuilder();
        for (Integer sid : wo.getServiceItemIds()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String name = null;
            if (invoice != null && invoice.getLines() != null) {
                for (InvoiceLine line : invoice.getLines()) {
                    if (line.getServiceItemId() == sid) {
                        name = line.getServiceName();
                        break;
                    }
                }
            }
            if (name == null) {
                ServiceItem service = garage.findServiceItem(sid.intValue());
                name = service == null ? null : service.getName();
            }
            if (name == null) {
                name = "Service #" + sid;
            }
            sb.append(SeedText.resolve(name));
        }
        return sb.toString();
    }

    public static double workOrderTotal(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return 0.0;
        if (wo.getServiceItemIds() == null) return 0.0;
        // Summed per service in the same order as workOrderServicePrice, that is
        // frozen price first. Then the sum matches what the invoice will add up to.
        double total = 0.0;
        for (Integer sid : wo.getServiceItemIds()) {
            total += workOrderServicePrice(garage, wo, sid.intValue());
        }
        return total;
    }

    public static String invoiceCustomerName(GarageSystem garage, Invoice inv) {
        if (garage == null || inv == null) return "-";
        WorkOrder wo = garage.findWorkOrder(inv.getWorkOrderId());
        return wo == null ? "-" : workOrderCustomerName(garage, wo);
    }

    /** The customer behind a payment, via invoice, work order, booking and vehicle. */
    public static String paymentCustomerName(GarageSystem garage, Payment pay) {
        if (garage == null || pay == null) return "-";
        Invoice inv = garage.findInvoice(pay.getInvoiceId());
        return inv == null ? "-" : invoiceCustomerName(garage, inv);
    }

    /** Total estimated work time for the services on the work order. */
    public static int workOrderTotalMinutes(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null || wo.getServiceItemIds() == null) {
            return 0;
        }
        int total = 0;
        for (Integer sid : wo.getServiceItemIds()) {
            ServiceItem service = garage.findServiceItem(sid.intValue());
            if (service != null) {
                total += service.getEstimatedMinutes();
            }
        }
        return total;
    }

}
