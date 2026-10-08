package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.util.List;

/** The guards against deleting a row that something else points at. */
public class RemovalRules {

    private final WorkOrderService workOrderService;
    private final BookingService bookingService;
    private final BillingService billingService;
    private final VehicleService vehicleService;

    public RemovalRules(WorkOrderService workOrderService,
                        BookingService bookingService,
                        BillingService billingService,
                        VehicleService vehicleService) {
        this.workOrderService = workOrderService;
        this.bookingService = bookingService;
        this.billingService = billingService;
        this.vehicleService = vehicleService;
    }

    private List<WorkOrder> workOrders() {
        return workOrderService.getAll();
    }

    private List<Booking> bookings() {
        return bookingService.getAll();
    }

    private List<Invoice> invoices() {
        return billingService.getAll();
    }

    private List<Vehicle> vehicles() {
        return vehicleService.getAll();
    }

    public boolean canDeleteMechanic(int mechanicId) {
        for (WorkOrder wo : workOrders()) {
            if (wo.getMechanicId() == mechanicId && !"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                return false;
            }
        }
        return true;
    }

    public boolean canDeleteCustomer(int customerId) {
        for (Vehicle v : vehicles()) {
            if (v.getCustomerId() == customerId) {
                if (!canDeleteVehicle(v.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canDeleteVehicle(int vehicleId) {
        for (Booking b : bookings()) {
            if (b.getVehicleId() == vehicleId && !"CANCELLED".equalsIgnoreCase(b.getStatus())) {
                return false;
            }
        }
        for (WorkOrder wo : workOrders()) {
            if (!"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                for (Booking b : bookings()) {
                    if (b.getId() == wo.getBookingId() && b.getVehicleId() == vehicleId) {
                        return false;
                    }
                }
            }
        }
        // A vehicle whose jobs have been invoiced cannot be deleted. The invoice points at the
        // work, the work at the booking, and the booking at the vehicle. The customer is refused through their vehicles.
        for (Booking b : bookings()) {
            if (b.getVehicleId() == vehicleId) {
                for (WorkOrder wo : workOrders()) {
                    if (wo.getBookingId() == b.getId() && hasInvoiceForWorkOrder(wo.getId())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** True if the work order has an invoice. The invoice points at the work, the work at the booking. */
    public boolean hasInvoiceForWorkOrder(int workOrderId) {
        for (Invoice invoice : invoices()) {
            if (invoice.getWorkOrderId() == workOrderId) {
                return true;
            }
        }
        return false;
    }

    public boolean canCancelOrDeleteBooking(int bookingId) {
        for (WorkOrder wo : workOrders()) {
            if (wo.getBookingId() == bookingId) {
                if (!"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                    return false;
                }
                // A finished order must not leave an invoice pointing at a booking that no longer
                // exists, so the deletion is refused for as long as the invoice is there.
                if (hasInvoiceForWorkOrder(wo.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canDeleteServiceItem(int serviceItemId) {
        for (WorkOrder wo : workOrders()) {
            if (!"COMPLETED".equalsIgnoreCase(wo.getStatus()) && wo.getServiceItemIds() != null) {
                for (int id : wo.getServiceItemIds()) {
                    if (id == serviceItemId) {
                        return false;
                    }
                }
            }
        }
        // The service cannot be deleted either while it sits in a booking, whatever the work
        // order's status. Otherwise the booking row points at a service that does not exist.
        for (Booking b : bookings()) {
            if (b.getServiceItemIds() != null) {
                for (int id : b.getServiceItemIds()) {
                    if (id == serviceItemId) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
