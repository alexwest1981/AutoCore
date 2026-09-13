package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar fakturering och rabattberäkning i verkstadssystemet.
 *
 * Ansvarar för:
 * - Summering av priser för utförda tjänster på en slutförd arbetsorder
 * - Tillämpning av VIP-rabatt och kampanjkoder (förberett för Strategy Pattern)
 * - Skapande och registrering av fakturor
 */
public class BillingService {

    public List<Invoice> getAll() {
        return Collections.unmodifiableList(Database.getInvoices());
    }

    public Invoice findById(int id) {
        for (Invoice invoice : Database.getInvoices()) {
            if (invoice.getId() == id) {
                return invoice;
            }
        }
        return null;
    }

    public Invoice createInvoice(int workOrderId, String discountCode) {
        WorkOrder workOrder = findWorkOrder(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return null;
        }

        if (!"COMPLETED".equals(workOrder.getStatus())) {
            System.out.println("Invoice can only be created for a completed work order.");
            return null;
        }

        double amount = 0.0;
        for (Integer serviceItemId : workOrder.getServiceItemIds()) {
            ServiceItem serviceItem = findServiceItem(serviceItemId);
            if (serviceItem != null) {
                amount += serviceItem.getPrice();
            }
        }

        double discount = 0.0;
        Booking booking = findBooking(workOrder.getBookingId());
        if (booking != null) {
            Vehicle vehicle = findVehicle(booking.getVehicleId());
            if (vehicle != null) {
                Customer customer = findCustomer(vehicle.getCustomerId());
                if (customer != null && customer.isVip()) {
                    discount += amount * 0.10;
                    System.out.println("VIP discount applied: 10%");
                }
            }
        }

        if (discountCode != null && !discountCode.trim().isEmpty()) {
            if (discountCode.equalsIgnoreCase("WELCOME10")) {
                discount += amount * 0.10;
                System.out.println("Discount code WELCOME10 applied.");
            } else if (discountCode.equalsIgnoreCase("SERVICE200")) {
                discount += 200.0;
                System.out.println("Discount code SERVICE200 applied.");
            } else {
                System.out.println("Unknown discount code. No code discount applied.");
            }
        }

        if (discount > amount) {
            discount = amount;
        }

        int id = Database.getInvoices().size() + 1;
        Invoice invoice = new Invoice(id, workOrderId, LocalDate.now(), amount);
        invoice.setDiscount(discount);

        Database.getInvoices().add(invoice);

        System.out.println("Invoice created successfully.");
        System.out.println(invoice);
        System.out.println("Sending invoice notification to customer...");
        System.out.println("Notification sent.");

        return invoice;
    }

    private WorkOrder findWorkOrder(int id) {
        for (WorkOrder order : Database.getWorkOrders()) {
            if (order.getId() == id) {
                return order;
            }
        }
        return null;
    }

    private Booking findBooking(int id) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == id) {
                return booking;
            }
        }
        return null;
    }

    private Vehicle findVehicle(int id) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == id) {
                return vehicle;
            }
        }
        return null;
    }

    private Customer findCustomer(int id) {
        for (Customer customer : Database.getCustomers()) {
            if (customer.getId() == id) {
                return customer;
            }
        }
        return null;
    }

    private ServiceItem findServiceItem(int id) {
        for (ServiceItem item : Database.getServiceItems()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }
}
