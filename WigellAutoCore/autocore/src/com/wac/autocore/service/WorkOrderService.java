package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.WorkOrderRepository;

import java.sql.SQLException;
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

    private final WorkOrderRepository workOrderRepository = new WorkOrderRepository();
    private final BookingRepository bookingRepository = new BookingRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();

    public List<WorkOrder> getAll() {
        try {
            return workOrderRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read work orders: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public WorkOrder findById(int id) {
        try {
            return workOrderRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read work order " + id + ": " + e.getMessage());
            return null;
        }
    }

    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            System.out.println("Booking with ID " + bookingId + " does not exist.");
            return null;
        }

        List<Integer> bookingServiceIds = booking.getServiceItemIds();
        if (bookingServiceIds.isEmpty()) {
            System.out.println("Booking with ID " + bookingId + " has no services to perform.");
            return null;
        }

        // En bokning ska bara kunna få en arbetsorder. Utan den här spärren kan
        // samma bokning faktureras två gånger.
        for (WorkOrder existingOrder : getAll()) {
            if (existingOrder.getBookingId() == bookingId) {
                System.out.println("Booking with ID " + bookingId + " already has work order "
                        + existingOrder.getId() + ".");
                return null;
            }
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

        WorkOrder workOrder = new WorkOrder(0, bookingId, mechanicId);

        // SCRUM-156 (C1): arbetsordern får de tjänster som bokningen innehåller,
        // inte ett urval som görs i stunden.
        for (Integer serviceItemId : bookingServiceIds) {
            workOrder.addServiceItem(serviceItemId);
        }

        try {
            workOrderRepository.save(workOrder);
        } catch (SQLException e) {
            System.out.println("Could not save work order: " + e.getMessage());
            return null;
        }

        workOrder.setStatus("CREATED");
        booking.setStatus("WORK_ORDER_CREATED");
        saveBooking(booking);

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
            saveMechanic(mechanic);
        }

        if (booking != null) {
            booking.setStatus("IN_PROGRESS");
            saveBooking(booking);
        }

        workOrder.setStatus("IN_PROGRESS");
        saveWorkOrder(workOrder);

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
        saveWorkOrder(workOrder);

        if (mechanic != null) {
            mechanic.setAvailable(true);
            saveMechanic(mechanic);
        }

        if (booking != null) {
            booking.setStatus("COMPLETED");
            saveBooking(booking);
        }

        System.out.println("Work order " + workOrderId + " has been completed.");
        return true;
    }

    /**
     * SCRUM-160 (D2): markerar tjänster som utförda och fryser priset som gäller i det ögonblicket.
     * Priset läses ur tjänstekatalogen här, så att en senare prisändring inte rör den här arbetsordern.
     */
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (serviceItemIds == null || serviceItemIds.length == 0) {
            System.out.println("No services given for work order " + workOrderId + ".");
            return false;
        }

        for (int serviceItemId : serviceItemIds) {
            ServiceItem serviceItem = findServiceItem(serviceItemId);
            if (serviceItem == null) {
                System.out.println("Service item with ID " + serviceItemId + " does not exist.");
                return false;
            }
            // Priset frysas en gång. Är tjänsten redan markerad behåller den sitt gamla pris.
            Double alreadyFrozen = workOrder.getCompletedServicePrice(serviceItemId);
            double frozenPrice = alreadyFrozen != null ? alreadyFrozen.doubleValue() : serviceItem.getPrice();
            workOrder.markServiceAsCompleted(serviceItemId, frozenPrice);
        }

        saveWorkOrder(workOrder);

        System.out.println("Services marked as performed on work order " + workOrderId
                + ", with the price that applied now.");
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
        try {
            return bookingRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read booking " + id + ": " + e.getMessage());
            return null;
        }
    }

    private Mechanic findMechanic(int id) {
        try {
            return mechanicRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read mechanic " + id + ": " + e.getMessage());
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

    private void saveBooking(Booking booking) {
        try {
            bookingRepository.save(booking);
        } catch (SQLException e) {
            System.out.println("Could not update booking " + booking.getId() + ": " + e.getMessage());
        }
    }

    private void saveMechanic(Mechanic mechanic) {
        try {
            mechanicRepository.save(mechanic);
        } catch (SQLException e) {
            System.out.println("Could not update mechanic " + mechanic.getId() + ": " + e.getMessage());
        }
    }

    private void saveWorkOrder(WorkOrder workOrder) {
        try {
            workOrderRepository.save(workOrder);
        } catch (SQLException e) {
            System.out.println("Could not update work order " + workOrder.getId() + ": " + e.getMessage());
        }
    }
}
