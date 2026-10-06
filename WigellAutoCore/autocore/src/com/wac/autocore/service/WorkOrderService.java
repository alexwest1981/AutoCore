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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Arbetsorderns livscykel från skapad till klar. */
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

        // Tjänsterna som ingen arbetsorder tagit hand om än. Är alla redan tagna finns det inget
        // kvar att göra, och då ska ingen ny order skapas — samma spärr mot dubbel fakturering som
        // förut, men per tjänst i stället för per bokning.
        List<Integer> remaining = new ArrayList<Integer>();
        for (Integer serviceItemId : booking.getServiceItemIds()) {
            if (!isClaimed(bookingId, serviceItemId)) {
                remaining.add(serviceItemId);
            }
        }
        if (remaining.isEmpty()) {
            System.out.println("Booking with ID " + bookingId
                    + " has no services left for a new work order.");
            return null;
        }
        return createWorkOrder(bookingId, mechanicId, remaining);
    }

/** Skapar en arbetsorder för ett urval av tjänsterna. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            System.out.println("Booking with ID " + bookingId + " does not exist.");
            return null;
        }

        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            System.out.println("Booking with ID " + bookingId + " has no services to perform.");
            return null;
        }

        for (WorkOrder existingOrder : getAll()) {
            if (existingOrder.getBookingId() != bookingId) {
                continue;
            }
            for (Integer serviceItemId : serviceItemIds) {
                if (existingOrder.getServiceItemIds().contains(serviceItemId)) {
                    System.out.println("Service " + serviceItemId + " is already on work order "
                            + existingOrder.getId() + ".");
                    return null;
                }
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

        for (Integer serviceItemId : serviceItemIds) {
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

/** Sant om tjänsten redan ligger på en arbetsorder för samma bokning. Katalogen är delad. */
    private boolean isClaimed(int bookingId, int serviceItemId) {
        for (WorkOrder order : getAll()) {
            if (order.getBookingId() != bookingId) {
                continue;
            }
            if (order.getServiceItemIds().contains(Integer.valueOf(serviceItemId))) {
                return true;
            }
        }
        return false;
    }

    // Enda stället som avgör vilka byten som är tillåtna. Allt som inte står här nekas.
    private boolean canChangeStatus(String from, String to) {
        if ("CREATED".equals(from) && "IN_PROGRESS".equals(to)) {
            return true;
        }

        if ("IN_PROGRESS".equals(from) && "COMPLETED".equals(to)) {
            return true;
        }

        if ("CREATED".equals(from) && "CANCELLED".equals(to)) {
            return true;
        }

        if ("IN_PROGRESS".equals(from) && "CANCELLED".equals(to)) {
            return true;
        }

        return false;
    }


    public boolean startWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!canChangeStatus(workOrder.getStatus(), "IN_PROGRESS")) {
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

        if (!canChangeStatus(workOrder.getStatus(), "COMPLETED")) {
            System.out.println("Only work orders in progress can be completed.");
            return false;
        }

        // Priset frysas även när ordern slutförs utan att någon har markerat arbetena.
        // Priset läggs på raden utan att tjänsten markeras som utförd, för fakturan ska
        // fortfarande bara byggas på de arbeten som faktiskt markerats.
        Map<Integer, Double> priser = new LinkedHashMap<Integer, Double>(workOrder.getCompletedServicePrices());
        for (Integer serviceItemId : workOrder.getServiceItemIds()) {
            if (!priser.containsKey(serviceItemId)) {
                ServiceItem serviceItem = findServiceItem(serviceItemId.intValue());
                if (serviceItem != null) {
                    priser.put(serviceItemId, Double.valueOf(serviceItem.getPrice()));
                }
            }
        }
        workOrder.setCompletedServicePrices(priser);

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

    // Ett avbrutet utkast ska inte lämna kvar en bokad tid, så bokningen avbryts och schemat frigörs.
    public boolean cancelWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!canChangeStatus(workOrder.getStatus(), "CANCELLED")) {
            System.out.println("Only drafts and work orders in progress can be cancelled.");
            return false;
        }

        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
        Booking booking = findBooking(workOrder.getBookingId());

        if (mechanic != null) {
            mechanic.setAvailable(true);
            saveMechanic(mechanic);
        }

        if (booking != null) {
            booking.setStatus("CANCELLED");
            saveBooking(booking);
            MechanicSchedule.getInstance().cancelSlotForBooking(booking.getId());
            MechanicSchedule.getInstance().syncFromDatabase();
        }

        workOrder.setStatus("CANCELLED");
        saveWorkOrder(workOrder);

        System.out.println("Work order " + workOrderId + " has been cancelled.");

        return true;
    }

/** Markerar tjänsterna utförda och sparar priset just nu. */
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!"IN_PROGRESS".equals(workOrder.getStatus())) {
            System.out.println("Services can only be marked as performed on a work order in progress.");
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
