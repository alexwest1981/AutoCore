package com.wac.autocore.service;

import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.repository.WorkOrderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Invoices from completed work orders, with discount. */
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final ServiceItemRepository serviceItemRepository;

    public BillingService(InvoiceRepository invoiceRepository,
                          WorkOrderRepository workOrderRepository,
                          BookingRepository bookingRepository,
                          VehicleRepository vehicleRepository,
                          CustomerRepository customerRepository,
                          ServiceItemRepository serviceItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public List<Invoice> getAll() {
        try {
            return invoiceRepository.findAll();
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read invoices", e);
        }
    }

    /** With no new charge: the console and other calls that have no reclamation. */
    public Invoice createInvoice(int workOrderId, String discountCode) {
        return createInvoice(workOrderId, discountCode, null, 0);
    }

    /** Invoices a work order, with a new charge as its own line when the reclamation calls for one. */
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, int extraAmount) {
        WorkOrder workOrder = findWorkOrder(workOrderId);
        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            return null;
        }

        // The whole booking counts here too: a single work order cannot be invoiced while its
        // sibling orders on the same booking are still open.
        if (!allOrdersCompleted(workOrder.getBookingId())) {
            return null;
        }

        // A job should only be invoiced once. The interface stops the same booking being picked
        // twice, but the call can come from below — and then there were two invoices for one job.
        if (hasInvoiceFor(workOrderId)) {
            return null;
        }

        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        orders.add(workOrder);
        return createInvoiceFrom(orders, workOrderId, discountCode, extraName, extraAmount);
    }

    /**
     * Takes back an issued invoice with a credit invoice of its own. The original is left exactly
     * as it was sent, and the two together come to zero.
     */
    public Invoice createCreditInvoice(int invoiceId, String reason) {
        Invoice original = findInvoice(invoiceId);
        if (original == null) {
            throw new NotFoundException("Invoice with ID " + invoiceId + " does not exist.");
        }
        if (original.isCredit()) {
            throw new RuleViolationException(
                    "Invoice " + original.getNumber() + " is a credit invoice and cannot be credited.");
        }
        Invoice alreadyCredited = invoiceRepository.findCreditFor(invoiceId);
        if (alreadyCredited != null) {
            throw new RuleViolationException("Invoice " + original.getNumber()
                    + " is already credited by " + alreadyCredited.getNumber() + ".");
        }

        // Every amount is mirrored, including the discount, so the VAT is given back exactly as it
        // was charged and the pair nets to zero.
        Invoice credit = new Invoice(0, original.getWorkOrderId(), LocalDate.now(), -original.getAmount());
        credit.setDiscount(-original.getDiscount());
        credit.setCreditOf(original.getId());
        credit.setCreditReason(reason);

        for (InvoiceLine line : original.getLines()) {
            InvoiceLine copied = new InvoiceLine(0, 0, line.getServiceItemId(), line.getServiceName(),
                    -line.getPrice(), -line.getDiscount());
            copied.setPackageName(line.getPackageName());
            credit.addLine(copied);
        }

        try {
            invoiceRepository.save(credit);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save credit invoice", e);
        }
        return credit;
    }

    /** Invoices all the work done on a booking on a single invoice. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {
        return createInvoiceForBooking(bookingId, discountCode, null, 0);
    }

    /** The same invoice, but with room for a new charge the reclamation brings with it. */
    public Invoice createInvoiceForBooking(int bookingId, String discountCode, String extraName, int extraAmount) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            throw new NotFoundException("Booking with ID " + bookingId + " does not exist.");
        }

        // One invoice covers all the work on the booking, so if something is left to do it waits.
        if (!allOrdersCompleted(bookingId)) {
            return null;
        }

        List<Integer> invoiced = invoicedServiceIds(bookingId);
        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        int primaryOrderId = 0;

        for (WorkOrder order : getAllWorkOrders()) {
            if (order.getBookingId() != bookingId) {
                continue;
            }
            if (order.getStatus() != WorkOrderStatus.COMPLETED) {
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
            return null;
        }

        return createInvoiceFrom(orders, primaryOrderId, discountCode, extraName, extraAmount);
    }

    /** Bookings with finished work still to invoice, one row per booking. */
    public List<Booking> getInvoiceableBookings() {
        List<Integer> handled = new ArrayList<Integer>();
        List<Booking> bookings = new ArrayList<Booking>();

        for (WorkOrder order : getAllWorkOrders()) {
            if (order.getStatus() != WorkOrderStatus.COMPLETED) {
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
                                      String extraName, int extraAmount) {
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
                    Integer frozenPrice = workOrder.getCompletedServicePrice(serviceItemId);
                    int linePrice = frozenPrice != null ? frozenPrice.intValue() : serviceItem.getPrice();
                    // Reclamation work shows on the invoice but costs nothing. The price is zeroed and
                    // not the discount, because distributeDiscount overwrites each line's discount as
                    // soon as the invoice has a VIP or code discount.
                    if (workOrder.isReclamation()) {
                        linePrice = 0;
                    }
                    InvoiceLine invoiceLine = new InvoiceLine(0, 0, serviceItem.getId(),
                            serviceItem.getName(), linePrice, 0);
                    // The package follows the service from the order, so the invoice can group the lines.
                    invoiceLine.setPackageName(workOrder.getServicePackageName(serviceItem.getId()));
                    lines.add(invoiceLine);
                }
            }
        }

        // A new charge belongs to the reclamation and is billed in full. It goes in before the sum
        // and the discount, so that it counts towards both.
        if (extraName != null && !extraName.trim().isEmpty() && extraAmount > 0) {
            lines.add(new InvoiceLine(0, 0, 0, extraName.trim(), extraAmount, 0));
        }

        if (lines.isEmpty()) {
            return null;
        }

        int amount = 0;
        for (InvoiceLine line : lines) {
            amount += line.getPrice();
        }

        int discount = 0;
        Booking booking = bookingId > 0 ? findBooking(bookingId) : null;
        if (booking != null) {
            Vehicle vehicle = findVehicle(booking.getVehicleId());
            if (vehicle != null) {
                Customer customer = findCustomer(vehicle.getCustomerId());
                if (customer != null && customer.isVip()) {
                    discount += DiscountRules.percentOf(amount, DiscountRules.VIP_PERCENT);
                }
            }
        }

        discount += DiscountRules.forCode(discountCode, amount);

        if (discount > amount) {
            discount = amount;
        }

        DiscountRules.distributeDiscount(lines, amount, discount);

        Invoice invoice = new Invoice(0, primaryOrderId, LocalDate.now(), amount);
        invoice.setDiscount(discount);

        for (InvoiceLine line : lines) {
            invoice.addLine(line);
        }

        try {
            invoiceRepository.save(invoice);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save invoice", e);
        }

        for (InvoiceLine line : invoice.getLines()) {
            line.setInvoiceId(invoice.getId());
        }


        return invoice;
    }

    /**
     * Performed services not yet invoiced.
     *
     * If none is marked but the order is finished, all of them count. That is the decision in
     * SCRUM-352: a finished order says the work is done, and the marking is bookkeeping on top of
     * that. Without this line a completed order with nothing marked would have been invoiced on
     * nothing, or not at all. CompleteWithoutMarkingTest holds it in place.
     */
    private List<Integer> performedServices(WorkOrder workOrder, List<Integer> invoicedServiceIds) {
        List<Integer> performed = workOrder.getCompletedServiceItems();
        if (performed.isEmpty() && workOrder.getStatus() == WorkOrderStatus.COMPLETED) {
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read invoice lines", e);
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
            if (order.getStatus() != WorkOrderStatus.COMPLETED) {
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read work orders", e);
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not check for existing invoices", e);
        }
        return false;
    }

    private Invoice findInvoice(int id) {
        try {
            return invoiceRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read invoice " + id, e);
        }
    }

    private WorkOrder findWorkOrder(int id) {
        try {
            return workOrderRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read work order " + id, e);
        }
    }

    private Booking findBooking(int id) {
        try {
            return bookingRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read booking " + id, e);
        }
    }

    private Vehicle findVehicle(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read vehicle " + id, e);
        }
    }

    private Customer findCustomer(int id) {
        try {
            return customerRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read customer " + id, e);
        }
    }

    private ServiceItem findServiceItem(int id) {
        try {
            return serviceItemRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read service item " + id, e);
        }
    }
}
