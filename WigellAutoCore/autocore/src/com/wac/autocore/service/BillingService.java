package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.repository.WorkOrderRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar fakturering och rabattberäkning i verkstadssystemet.
 *
 * Ansvarar för:
 * - Summering av priser för utförda tjänster på en slutförd arbetsorder
 * - Tillämpning av VIP-rabatt och kampanjkoder
 * - Skapande och registrering av fakturor
 */
public class BillingService {

    private final InvoiceRepository invoiceRepository = new InvoiceRepository();
    private final WorkOrderRepository workOrderRepository = new WorkOrderRepository();
    private final BookingRepository bookingRepository = new BookingRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();

    public List<Invoice> getAll() {
        try {
            return invoiceRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read invoices: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Invoice findById(int id) {
        try {
            return invoiceRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read invoice " + id + ": " + e.getMessage());
            return null;
        }
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

        Invoice invoice = new Invoice(0, workOrderId, LocalDate.now(), amount);
        invoice.setDiscount(discount);

        try {
            invoiceRepository.save(invoice);
        } catch (SQLException e) {
            System.out.println("Could not save invoice: " + e.getMessage());
            return null;
        }

        System.out.println("Invoice created successfully.");
        System.out.println(invoice);
        System.out.println("Sending invoice notification to customer...");
        System.out.println("Notification sent.");

        return invoice;
    }

    private WorkOrder findWorkOrder(int id) {
        try {
            return workOrderRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read work order " + id + ": " + e.getMessage());
            return null;
        }
    }

    private Booking findBooking(int id) {
        try {
            return bookingRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read booking " + id + ": " + e.getMessage());
            return null;
        }
    }

    private Vehicle findVehicle(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read vehicle " + id + ": " + e.getMessage());
            return null;
        }
    }

    private Customer findCustomer(int id) {
        try {
            return customerRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read customer " + id + ": " + e.getMessage());
            return null;
        }
    }

    private ServiceItem findServiceItem(int id) {
        try {
            return serviceItemRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read service item " + id + ": " + e.getMessage());
            return null;
        }
    }
}
