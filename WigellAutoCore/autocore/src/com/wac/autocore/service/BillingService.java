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

/** Fakturor från slutförda arbetsordrar, med rabatt. */
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

        // Även här gäller hela bokningen: en enskild arbetsorder får inte faktureras medan
        // syskonarbetsordrarna på samma bokning är kvar.
        if (!allOrdersCompleted(workOrder.getBookingId())) {
            System.out.println("Booking with ID " + workOrder.getBookingId()
                    + " still has work orders that are not completed.");
            return null;
        }

        // Ett jobb ska bara faktureras en gång. Gränssnittet hindrar att samma bokning väljs om,
        // men anropet kan komma underifrån — och då blev det två fakturor på samma arbete.
        if (hasInvoiceFor(workOrderId)) {
            System.out.println("Invoice already exists for work order " + workOrderId + ".");
            return null;
        }

        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        orders.add(workOrder);
        return createInvoiceFrom(orders, workOrderId, discountCode);
    }

/** Fakturerar allt utfört arbete på en bokning i en faktura. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            System.out.println("Booking with ID " + bookingId + " does not exist.");
            return null;
        }

        // En faktura täcker allt arbete på bokningen, så är något kvar att göra väntar den.
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

        return createInvoiceFrom(orders, primaryOrderId, discountCode);
    }

/** Bokningar med utfört arbete kvar att fakturera, en rad per bokning. */
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

/** Bygger fakturan av de utförda tjänsterna. Redan fakturerade rader hoppas över. */
    private Invoice createInvoiceFrom(List<WorkOrder> orders, int primaryOrderId, String discountCode) {
        WorkOrder primary = findWorkOrder(primaryOrderId);
        int bookingId = primary != null ? primary.getBookingId() : 0;
        List<Integer> invoiced = invoicedServiceIds(bookingId);
        List<InvoiceLine> lines = new ArrayList<InvoiceLine>();

        for (WorkOrder workOrder : orders) {
            for (Integer serviceItemId : performedServices(workOrder, invoiced)) {
                ServiceItem serviceItem = findServiceItem(serviceItemId);
                if (serviceItem != null) {
                    // Använd det sparade priset när det finns ett.
                    // Äldre arbetsordrar saknar det och får katalogens pris, som före D2.
                    Double frozenPrice = workOrder.getCompletedServicePrice(serviceItemId);
                    double linePrice = frozenPrice != null ? frozenPrice.doubleValue() : serviceItem.getPrice();
                    lines.add(new InvoiceLine(0, 0, serviceItem.getId(),
                            serviceItem.getName(), linePrice, 0.0));
                }
            }
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
/** Utförda tjänster som inte fakturerats. Är ingen markerad men ordern slutförd gäller alla. */
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

/** Tjänsterna som redan står på en faktura. En tjänst faktureras en gång. */
    private List<Integer> invoicedServiceIds(int bookingId) {
        List<Integer> invoiced = new ArrayList<Integer>();
        List<Integer> bookingOrderIds = bookingOrderIds(bookingId);
        try {
            for (Invoice invoice : invoiceRepository.findAll()) {
                // Tjänstekatalogen är delad, så en fakturarad med samma tjänst-id kan höra till en
                // helt annan bokning. Bara fakturor på den här bokningens arbetsordrar räknas.
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

/** Sant om varje arbetsorder är slutförd. En bokning utan arbetsordrar räknas inte som klar. */
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

    /** Sant om någon bokning har arbetsordrar som ännu inte är slutförda. */
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

    /** Id:n för arbetsordrarna som hör till bokningen. */
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

/** Sant om arbetsordern redan har en faktura. Kontrollen går mot databasen, inte mot vyn. */
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
