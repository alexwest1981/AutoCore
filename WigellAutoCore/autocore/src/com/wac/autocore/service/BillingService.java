package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Invoices from completed work orders, with discount. */
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

    /** With no new charge: the console and other calls that have no reclamation. */
    public Invoice createInvoice(int workOrderId, String discountCode) {
        return createInvoice(workOrderId, discountCode, null, 0.0);
    }

    /** Invoices a work order, with a new charge as its own line when the reclamation calls for one. */
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, double extraAmount) {
        WorkOrder workOrder = findWorkOrder(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return null;
        }

        if (!"COMPLETED".equals(workOrder.getStatus())) {
            System.out.println("Invoice can only be created for a completed work order.");
            return null;
        }

        // The whole booking counts here too: a single work order cannot be invoiced while its
        // sibling orders on the same booking are still open.
        if (!allOrdersCompleted(workOrder.getBookingId())) {
            System.out.println("Booking with ID " + workOrder.getBookingId()
                    + " still has work orders that are not completed.");
            return null;
        }

        // A job should only be invoiced once. The interface stops the same booking being picked
        // twice, but the call can come from below — and then there were two invoices for one job.
        if (hasInvoiceFor(workOrderId)) {
            System.out.println("Invoice already exists for work order " + workOrderId + ".");
            return null;
        }

        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        orders.add(workOrder);
        return createInvoiceFrom(orders, workOrderId, discountCode, extraName, extraAmount);
    }

    /** Invoices all the work done on a booking on a single invoice. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        return createInvoiceForBooking(bookingId, discountCode, null, 0.0);
    }

    /** The same invoice, but with room for a new charge the reclamation brings with it. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode, String extraName, double extraAmount) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            System.out.println("Booking with ID " + bookingId + " does not exist.");
            return null;
        }

        // One invoice covers all the work on the booking, so if something is left to do it waits.
        if (!allOrdersCompleted(bookingId)) {
            System.out.println("Booking with ID " + bookingId
                    + " still has work orders that are not completed.");
            return null;
        }

        List<Integer> invoiced = invoicedServiceIds(bookingId);
        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        int primaryOrderId = 0;

        for (WorkOrder order : getAllWorkOrders()) {
            if (order.getBookingId() != bookingId) {
                continue;
            }
            if (!"COMPLETED".equals(order.getStatus())) {
                continue;
            }
            if (performedServices(order, invoiced).isEmpty()) {
                continue;
            }
            orders.add(order);
            if (primaryOrderId == 0 || order.getId() < primaryOrderId) {
                primaryOrderId = order.getId();
            }
        }

        if (orders.isEmpty()) {
            System.out.println("Booking with ID " + bookingId
                    + " has no performed work left to invoice.");
            return null;
        }

        return createInvoiceFrom(orders, primaryOrderId, discountCode, extraName, extraAmount);
    }

    /** Bookings with finished work still to invoice, one row per booking. */
    public List<Booking> getInvoiceableBookings() {
        List<Integer> handled = new ArrayList<Integer>();
        List<Booking> bookings = new ArrayList<Booking>();

        for (WorkOrder order : getAllWorkOrders()) {
            if (!"COMPLETED".equals(order.getStatus())) {
                continue;
            }
            if (!allOrdersCompleted(order.getBookingId())) {
                continue;
            }
            if (performedServices(order, invoicedServiceIds(order.getBookingId())).isEmpty()) {
                continue;
            }
            if (handled.contains(Integer.valueOf(order.getBookingId()))) {
                continue;
            }
            Booking booking = findBooking(order.getBookingId());
            if (booking == null) {
                continue;
            }
            handled.add(Integer.valueOf(order.getBookingId()));
            bookings.add(booking);
        }
        return bookings;
    }

    /** Builds the invoice from the performed services. Lines already invoiced are skipped. */
    private Invoice createInvoiceFrom(List<WorkOrder> orders, int primaryOrderId, String discountCode,
                                      String extraName, double extraAmount) {
        WorkOrder primary = findWorkOrder(primaryOrderId);
        int bookingId = primary != null ? primary.getBookingId() : 0;
        List<Integer> invoiced = invoicedServiceIds(bookingId);
        List<InvoiceLine> lines = new ArrayList<InvoiceLine>();

        for (WorkOrder workOrder : orders) {
            for (Integer serviceItemId : performedServices(workOrder, invoiced)) {
                ServiceItem serviceItem = findServiceItem(serviceItemId);
                if (serviceItem != null) {
                    // Use the stored price when there is one.
                    // Older work orders have none and get the catalogue price, as before D2.
                    Double frozenPrice = workOrder.getCompletedServicePrice(serviceItemId);
                    double linePrice = frozenPrice != null ? frozenPrice.doubleValue() : serviceItem.getPrice();
                    // Reclamation work shows on the invoice but costs nothing. The price is zeroed and
                    // not the discount, because distributeDiscount overwrites each line's discount as
                    // soon as the invoice has a VIP or code discount.
                    if (workOrder.isReclamation()) {
                        linePrice = 0.0;
                    }
                    InvoiceLine invoiceLine = new InvoiceLine(0, 0, serviceItem.getId(),
                            serviceItem.getName(), linePrice, 0.0);
                    // The package follows the service from the order, so the invoice can group the lines.
                    invoiceLine.setPackageName(workOrder.getServicePackageName(serviceItem.getId()));
                    lines.add(invoiceLine);
                }
            }
        }

        // A new charge belongs to the reclamation and is billed in full. It goes in before the sum
        // and the discount, so that it counts towards both.
        if (extraName != null && !extraName.trim().isEmpty() && extraAmount > 0) {
            lines.add(new InvoiceLine(0, 0, 0, extraName.trim(), extraAmount, 0.0));
        }

        if (lines.isEmpty()) {
            System.out.println("Invoice cannot be created: No performed services found.");
            return null;
        }

        double amount = 0.0;
        for (InvoiceLine line : lines) {
            amount += line.getPrice();
        }

        double discount = 0.0;
        Booking booking = bookingId > 0 ? findBooking(bookingId) : null;
        if (booking != null) {
            Vehicle vehicle = findVehicle(booking.getVehicleId());
            if (vehicle != null) {
                Customer customer = findCustomer(vehicle.getCustomerId());
                if (customer != null && customer.isVip()) {
                    discount += amount * (DiscountRules.VIP_PERCENT / 100.0);
                    System.out.println("VIP discount applied: 10%");
                }
            }
        }

        discount += DiscountRules.forCode(discountCode, amount);

        if (discount > amount) {
            discount = amount;
        }
        discount = DiscountRules.roundToOre(discount);

        DiscountRules.distributeDiscount(lines, amount, discount);

        Invoice invoice = new Invoice(0, primaryOrderId, LocalDate.now(), amount);
        invoice.setDiscount(discount);

        for (InvoiceLine line : lines) {
            invoice.addLine(line);
        }

        try {
            invoiceRepository.save(invoice);
        } catch (SQLException e) {
            System.out.println("Could not save invoice: " + e.getMessage());
            return null;
        }

        for (InvoiceLine line : invoice.getLines()) {
            line.setInvoiceId(invoice.getId());
        }

        System.out.println("Invoice created successfully.");
        System.out.println(invoice);
        System.out.println("Sending invoice notification to customer...");
        System.out.println("Notification sent.");

        return invoice;
    }

    /** Performed services not yet invoiced. If none is marked but the order is finished, all count. */
    private List<Integer> performedServices(WorkOrder workOrder, List<Integer> invoicedServiceIds) {
        List<Integer> performed = workOrder.getCompletedServiceItems();
        if (performed.isEmpty() && "COMPLETED".equals(workOrder.getStatus())) {
            performed = workOrder.getServiceItemIds();
        }

        List<Integer> remaining = new ArrayList<Integer>();
        for (Integer serviceItemId : performed) {
            if (!invoicedServiceIds.contains(serviceItemId)) {
                remaining.add(serviceItemId);
            }
        }
        return remaining;
    }

    /** The services already on an invoice. A service is invoiced once. */
    private List<Integer> invoicedServiceIds(int bookingId) {
        List<Integer> invoiced = new ArrayList<Integer>();
        List<Integer> bookingOrderIds = bookingOrderIds(bookingId);
        try {
            for (Invoice invoice : invoiceRepository.findAll()) {
                // The service catalogue is shared, so an invoice line with the same service id may
                // belong to a different booking. Only invoices on this booking's orders count.
                if (!bookingOrderIds.contains(Integer.valueOf(invoice.getWorkOrderId()))) {
                    continue;
                }
                for (InvoiceLine line : invoice.getLines()) {
                    if (line.getServiceItemId() > 0
                            && !invoiced.contains(Integer.valueOf(line.getServiceItemId()))) {
                        invoiced.add(Integer.valueOf(line.getServiceItemId()));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not read invoice lines: " + e.getMessage());
        }
        return invoiced;
    }

    /** True if every work order is finished. A booking with no work orders does not count as done. */
    private boolean allOrdersCompleted(int bookingId) {
        boolean foundAny = false;
        for (WorkOrder order : getAllWorkOrders()) {
            if (order.getBookingId() != bookingId) {
                continue;
            }
            foundAny = true;
            if (!"COMPLETED".equals(order.getStatus())) {
                return false;
            }
        }
        return foundAny;
    }

    /** True if any booking has work orders that are not finished yet. */
    public boolean hasBookingWithUnfinishedWork() {
        List<Integer> seen = new ArrayList<Integer>();
        for (WorkOrder order : getAllWorkOrders()) {
            Integer bookingId = Integer.valueOf(order.getBookingId());
            if (seen.contains(bookingId)) {
                continue;
            }
            seen.add(bookingId);
            if (!allOrdersCompleted(order.getBookingId())) {
                return true;
            }
        }
        return false;
    }

    /** The ids of the work orders that belong to the booking. */
    private List<Integer> bookingOrderIds(int bookingId) {
        List<Integer> orderIds = new ArrayList<Integer>();
        for (WorkOrder order : getAllWorkOrders()) {
            if (order.getBookingId() == bookingId) {
                orderIds.add(Integer.valueOf(order.getId()));
            }
        }
        return orderIds;
    }

    private List<WorkOrder> getAllWorkOrders() {
        try {
            return workOrderRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read work orders: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /** True if the work order already has an invoice. The check goes against the database, not the view. */
    private boolean hasInvoiceFor(int workOrderId) {
        try {
            List<Invoice> invoices = invoiceRepository.findAll();
            for (int i = 0; i < invoices.size(); i++) {
                if (invoices.get(i).getWorkOrderId() == workOrderId) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not check for existing invoices: " + e.getMessage());
        }
        return false;
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
