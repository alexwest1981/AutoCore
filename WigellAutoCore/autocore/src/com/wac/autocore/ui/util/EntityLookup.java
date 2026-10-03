package com.wac.autocore.ui.util;

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
import com.wac.autocore.ui.i18n.I18n;

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

    public static String workOrderVehicleReg(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return "-";
        return bookingVehicleReg(garage, wo.getBookingId());
    }

    public static String workOrderCustomerName(GarageSystem garage, WorkOrder wo) {
        if (garage == null || wo == null) return "-";
        return bookingCustomerName(garage, wo.getBookingId());
    }

    public static Invoice invoiceForWorkOrder(GarageSystem garage, int workOrderId) {
        if (garage == null || workOrderId <= 0) return null;
        for (Invoice inv : garage.getInvoices()) {
            if (inv.getWorkOrderId() == workOrderId) {
                return inv;
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

    /**
     * Tjänstenamnen på en arbetsorder, utan priser. Arbetsordervyn är till för mekanikerna, som
     * behöver se vad som ska göras, inte vad det kostar. Namnet hämtas från fakturaraden när den
     * finns, annars från tjänsten, precis som i workOrderServicesWithPrices.
     */
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

    /**
     * Kunden bakom en betalning. Kedjan går betalning, faktura, arbetsorder, bokning, fordon, kund,
     * och varje steg använder samma hjälpmetoder som fakturavyn, så samma kund visas i båda vyerna.
     */
    public static String paymentCustomerName(GarageSystem garage, Payment pay) {
        if (garage == null || pay == null) return "-";
        for (Invoice inv : garage.getInvoices()) {
            if (inv.getId() == pay.getInvoiceId()) {
                return invoiceCustomerName(garage, inv);
            }
        }
        return "-";
    }

    /**
     * SCRUM-157 (C2): Total beräknad arbetstid för samtliga tjänster på arbetsordern.
     */
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

    /**
     * SCRUM-157 (C2): Formaterar arbetsorderns tjänster med status (utförd vs att utföra).
     */
    public static String workOrderServicesWithStatus(GarageSystem garage, WorkOrder wo) {
        if (wo == null || wo.getServiceItemIds() == null || wo.getServiceItemIds().isEmpty() || garage == null) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (Integer sid : wo.getServiceItemIds()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String name = null;
            for (ServiceItem s : garage.getServiceItems()) {
                if (s.getId() == sid.intValue()) {
                    name = SeedText.resolve(s.getName());
                    break;
                }
            }
            if (name == null) {
                name = "Service #" + sid;
            }
            sb.append(name);
            boolean done = wo.getCompletedServiceItems() != null && wo.getCompletedServiceItems().contains(sid);
            if (done) {
                sb.append(" [✔ ").append(I18n.get("status.completed")).append("]");
            } else {
                sb.append(" [").append(I18n.get("status.to_be_performed")).append("]");
            }
        }
        return sb.toString();
    }
}
