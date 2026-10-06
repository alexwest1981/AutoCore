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

/** Läsbara namn på relaterade poster, via id. */
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
                return SeedText.resolve(s.getName());
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
                    found = SeedText.resolve(s.getName());
                    break;
                }
            }
            sb.append(found != null ? found : "Service #" + sid);
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
                for (ServiceItem s : garage.getServiceItems()) {
                    if (s.getId() == sid) {
                        min += s.getEstimatedMinutes();
                        break;
                    }
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
                for (ServiceItem s : garage.getServiceItems()) {
                    if (s.getId() == sid) {
                        cost += s.getPrice();
                        break;
                    }
                }
            }
        }
        return cost;
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

    /** Registreringsnumret på bilen som fakturan gäller. Fakturan hör till en arbetsorder. */
    public static String invoiceVehicleReg(GarageSystem garage, Invoice invoice) {
        if (garage == null || invoice == null) return "-";
        int bookingId = bookingIdForWorkOrder(garage, invoice.getWorkOrderId());
        return bookingId > 0 ? bookingVehicleReg(garage, bookingId) : "-";
    }

    /** Registreringsnumret på bilen som betalningen gäller, via fakturan. */
    public static String paymentVehicleReg(GarageSystem garage, Payment payment) {
        if (garage == null || payment == null) return "-";
        for (Invoice invoice : garage.getInvoices()) {
            if (invoice.getId() == payment.getInvoiceId()) {
                return invoiceVehicleReg(garage, invoice);
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

    /** Bokningens valda tid som text, eller ett streck när ingen tid är vald. */
    public static String bookingTime(Booking b) {
        if (b == null || b.getStartTime() == null) return "-";
        if (b.getEndTime() == null) return b.getStartTime().toString();
        return b.getStartTime() + " - " + b.getEndTime();
    }

    /** Arbetsorderns datum, hämtat ur bokningen den skapades från. */
    public static String workOrderDate(GarageSystem garage, WorkOrder wo) {
        Booking b = bookingForWorkOrder(garage, wo);
        return b == null || b.getDate() == null ? "-" : String.valueOf(b.getDate());
    }

    /** Arbetsorderns tid, hämtad ur bokningen den skapades från. */
    public static String workOrderTime(GarageSystem garage, WorkOrder wo) {
        return bookingTime(bookingForWorkOrder(garage, wo));
    }

    /** Bokningen som arbetsordern hör till, eller null när den saknas. */
    private static Booking bookingForWorkOrder(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null || wo.getBookingId() <= 0) return null;
        for (Booking b : garage.getBookings()) {
            if (b.getId() == wo.getBookingId()) return b;
        }
        return null;
    }

    /** Bokningen som arbetsordern hör till, eller 0 om arbetsordern inte finns. */
    public static int bookingIdForWorkOrder(GarageSystem garage, int workOrderId) {
        if (garage == null || workOrderId <= 0) return 0;
        for (WorkOrder wo : garage.getWorkOrders()) {
            if (wo.getId() == workOrderId) {
                return wo.getBookingId();
            }
        }
        return 0;
    }

/** Fakturan som täcker arbetsordern. Den hör till bokningen, inte till en order. */
    public static Invoice invoiceForWorkOrder(GarageSystem garage, int workOrderId) {
        if (garage == null || workOrderId <= 0) return null;

        WorkOrder order = null;
        for (WorkOrder wo : garage.getWorkOrders()) {
            if (wo.getId() == workOrderId) {
                order = wo;
                break;
            }
        }
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

// Fakturan kan ligga på en annan arbetsorder i samma bokning.
        // så rader från andra bokningar får inte räknas hit.
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
            // Det frysta priset är priset som gällde när arbetet utfördes, och det ska
            // visas även innan fakturan finns. Samma ordning som i detaljdialogen:
            // fryst pris, sedan fakturaradens pris, sist katalogen.
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
        for (ServiceItem s : garage.getServiceItems()) {
            if (s.getId() == serviceItemId) {
                return s.getPrice();
            }
        }
        return 0.0;
    }

/** Tjänstenamnen på en arbetsorder, utan priser. Vyn är för mekanikerna. */
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
                for (ServiceItem s : garage.getServiceItems()) {
                    if (s.getId() == sid) {
                        name = s.getName();
                        break;
                    }
                }
            }
            if (name == null) {
                name = "Service #" + sid;
            }
            sb.append(SeedText.resolve(name));
        }
        return sb.toString();
    }

    public static String workOrderServicesWithPrices(GarageSystem garage, WorkOrder wo) {
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
            Double frozenPrice = wo.getCompletedServicePrice(sid.intValue());
            Double price = frozenPrice;
            if (invoice != null && invoice.getLines() != null) {
                for (InvoiceLine line : invoice.getLines()) {
                    if (line.getServiceItemId() == sid) {
                        name = line.getServiceName();
                        if (price == null) {
                            price = line.getPrice();
                        }
                        break;
                    }
                }
            }
            if (name == null) {
                for (ServiceItem s : garage.getServiceItems()) {
                    if (s.getId() == sid) {
                        name = s.getName();
                        if (price == null) {
                            price = s.getPrice();
                        }
                        break;
                    }
                }
            }
            if (name == null) {
                name = "Service #" + sid;
            }
            sb.append(SeedText.resolve(name));
            if (price != null) {
                sb.append(" (").append(UiFormatters.formatMoney(price)).append(")");
            }
        }
        return sb.toString();
    }

    public static double workOrderTotal(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return 0.0;
        if (wo.getServiceItemIds() == null) return 0.0;
        // Summeras per tjänst med samma ordning som workOrderServicePrice, alltså
        // fryst pris först. Då stämmer summan med det fakturan kommer att bygga.
        double total = 0.0;
        for (Integer sid : wo.getServiceItemIds()) {
            total += workOrderServicePrice(garage, wo, sid.intValue());
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

/** Kunden bakom en betalning, via faktura, arbetsorder, bokning och fordon. */
    public static String paymentCustomerName(GarageSystem garage, Payment pay) {
        if (garage == null || pay == null) return "-";
        for (Invoice inv : garage.getInvoices()) {
            if (inv.getId() == pay.getInvoiceId()) {
                return invoiceCustomerName(garage, inv);
            }
        }
        return "-";
    }

/** Total beräknad arbetstid för tjänsterna på arbetsordern. */
    public static int workOrderTotalMinutes(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null || wo.getServiceItemIds() == null) {
            return 0;
        }
        int total = 0;
        for (Integer sid : wo.getServiceItemIds()) {
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == sid.intValue()) {
                    total += s.getEstimatedMinutes();
                    break;
                }
            }
        }
        return total;
    }

}
