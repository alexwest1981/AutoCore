package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;

import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar livscykeln för en arbetsorder (WorkOrder).
 *
 * Ansvarar för:
 * - Validering av koppling till bokning, mekaniker och tjänster
 * - Skapande av arbetsorder
 * - Tilldelning och låsning/upplåsning av mekanikerns tillgänglighet
 * - Tillståndsövergångar (CREATED -> IN_PROGRESS -> COMPLETED)
 */
public class WorkOrderService {

    public List<WorkOrder> getAll() {
        return Collections.unmodifiableList(Database.getWorkOrders());
    }

    public WorkOrder findById(int id) {
        for (WorkOrder workOrder : Database.getWorkOrders()) {
            if (workOrder.getId() == id) {
                return workOrder;
            }
        }
        return null;
    }

    public WorkOrder createWorkOrder(int bookingId, int mechanicId, int... serviceItemIds) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            System.out.println("Booking with ID " + bookingId + " does not exist.");
            return null;
        }

        Mechanic mechanic = findMechanic(mechanicId);
        if (mechanic == null) {
            System.out.println("Mechanic with ID " + mechanicId + " does not exist.");
            return null;
        }

        if (!mechanic.isAvailable()) {
            System.out.println("Mechanic " + mechanic.getName() + " is not available.");
            return null;
        }

        for (int serviceItemId : serviceItemIds) {
            if (findServiceItem(serviceItemId) == null) {
                System.out.println("Service item with ID " + serviceItemId + " does not exist.");
                return null;
            }
        }

        int id = Database.getWorkOrders().size() + 1;
        WorkOrder workOrder = new WorkOrder(id, bookingId, mechanicId);

        for (int serviceItemId : serviceItemIds) {
            workOrder.addServiceItem(serviceItemId);
        }

        Database.getWorkOrders().add(workOrder);
        booking.setStatus("WORK_ORDER_CREATED");

        System.out.println("Work order created successfully.");
        System.out.println(workOrder);

        return workOrder;
    }

    public boolean startWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!"CREATED".equals(workOrder.getStatus())) {
            System.out.println("Work order cannot be started.");
            return false;
        }

        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
        Booking booking = findBooking(workOrder.getBookingId());

        if (mechanic != null) {
            mechanic.setAvailable(false);
        }

        if (booking != null) {
            booking.setStatus("IN_PROGRESS");
        }

        workOrder.setStatus("IN_PROGRESS");
        System.out.println("Work order " + workOrderId + " has been started.");
        return true;
    }

    public boolean completeWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!"IN_PROGRESS".equals(workOrder.getStatus())) {
            System.out.println("Only work orders in progress can be completed.");
            return false;
        }

        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
        Booking booking = findBooking(workOrder.getBookingId());

        workOrder.setStatus("COMPLETED");

        if (mechanic != null) {
            mechanic.setAvailable(true);
        }

        if (booking != null) {
            booking.setStatus("COMPLETED");
        }

        System.out.println("Work order " + workOrderId + " has been completed.");
        return true;
    }

    public int getTotalEstimatedMinutes(int... serviceItemsIDs) {
        int totalMinutes = 0;

        for (int serviceItemId : serviceItemsIDs){
            ServiceItem item = findServiceItem(serviceItemId);
            if (item != null) {
                totalMinutes += item.getEstimatedMinutes();
            }
        }
        return totalMinutes;
    }

    private Booking findBooking(int id) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == id) {
                return booking;
            }
        }
        return null;
    }

    private Mechanic findMechanic(int id) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == id) {
                return mechanic;
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
