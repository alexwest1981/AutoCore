package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.util.List;

/** Skydden mot att ta bort en rad som något annat pekar på. */
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
        // Ett fordon vars jobb har fakturerats får inte tas bort. Fakturan pekar på arbetet,
        // arbetet på bokningen och bokningen på fordonet. Kunden nekas via sina fordon.
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

    /** Sant om arbetsordern har en faktura. Fakturan pekar på arbetet, arbetet på bokningen. */
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
                // En slutförd order får inte lämna en faktura som pekar på en bokning
                // som inte finns, alltså nekas borttagningen så länge fakturan finns.
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
        // Tjänsten får inte heller tas bort så länge den ligger i en bokning, oavsett
        // arbetsorderns status. Annars pekar bokningsraden på en tjänst som inte finns.
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
