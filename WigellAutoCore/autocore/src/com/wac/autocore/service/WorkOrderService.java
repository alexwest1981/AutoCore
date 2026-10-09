package com.wac.autocore.service;

import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The work order's life cycle from created to finished. */
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository = new WorkOrderRepository();
    private final BookingRepository bookingRepository = new BookingRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();

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
            throw new DataAccessException("Could not read work order " + id, e);
        }
    }

    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {
        Booking booking = findBooking(bookingId);
        if (booking == null) {
            throw new NotFoundException("Booking with ID " + bookingId + " does not exist.");
        }

        // The services no work order has taken yet. If all are already taken there is nothing
        // left to do, and then no new order should be created — the same guard against double
        // invoicing as before, but per service instead of per booking.
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

    /** Creates a work order for a selection of the services. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds) {
        return createWorkOrder(bookingId, mechanicId, serviceItemIds, WorkOrder.TYPES.get(0));
    }

    /** Creates a work order of a given type. Standard, reclamation or internal work. */
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds, String type) {

        if (type == null || !WorkOrder.TYPES.contains(type)) {
            System.out.println("Unknown work order type: " + type);
            return null;
        }

        Booking booking = findBooking(bookingId);
        if (booking == null) {
            throw new NotFoundException("Booking with ID " + bookingId + " does not exist.");
        }

        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            System.out.println("Booking with ID " + bookingId + " has no services to perform.");
            return null;
        }

        for (WorkOrder existingOrder : getAll()) {

            if ("CANCELLED".equals(existingOrder.getStatus())) {
                continue;
            }

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
            throw new NotFoundException("Mechanic with ID " + mechanicId + " does not exist.");
        }

        if (!mechanic.isAvailable()) {
            System.out.println("Mechanic " + mechanic.getName() + " is not available.");
            return null;
        }

        WorkOrder workOrder = new WorkOrder(0, bookingId, mechanicId);
        workOrder.setType(type);

        for (Integer serviceItemId : serviceItemIds) {
            workOrder.addServiceItem(serviceItemId);
            // The package name follows the service from the booking, so the invoice can group it.
            workOrder.setServicePackage(serviceItemId, booking.getServicePackageName(serviceItemId));
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

    // A reclamation is a work order of its own that redoes earlier performed work and points it
    // out with originalWorkOrderId. Same booking, same mechanic, same services — but a new order.
    //
    // The guard in createWorkOrder that stops a service from sitting on two orders does not apply
    // here. The service is to be redone, and the reclamation is not charged to the customer
    // anyway, so it cannot be invoiced twice.
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {
        WorkOrder original = findById(originalWorkOrderId);

        if (original == null) {
            throw new NotFoundException("Work order with ID " + originalWorkOrderId + " does not exist.");
        }

        // Only work that has actually been performed can be reclaimed.
        if (!"COMPLETED".equalsIgnoreCase(original.getStatus())) {
            System.out.println("Work order " + originalWorkOrderId + " is not completed.");
            return null;
        }

        Mechanic mechanic = findMechanic(original.getMechanicId());
        if (mechanic == null) {
            throw new NotFoundException("Mechanic with ID " + original.getMechanicId() + " does not exist.");
        }

        if (!mechanic.isAvailable()) {
            System.out.println("Mechanic " + mechanic.getName() + " is not available.");
            return null;
        }

        WorkOrder reclamation = new WorkOrder(0, original.getBookingId(), original.getMechanicId());
        reclamation.setType(WorkOrder.RECLAMATION);
        reclamation.setOriginalWorkOrderId(originalWorkOrderId);
        reclamation.setVehicleId(original.getVehicleId());
        reclamation.setDescription(description);

        for (Integer serviceItemId : original.getServiceItemIds()) {
            reclamation.addServiceItem(serviceItemId);
        }

        try {
            workOrderRepository.save(reclamation);
        } catch (SQLException e) {
            System.out.println("Could not save reclamation: " + e.getMessage());
            return null;
        }

        System.out.println("Reclamation created successfully.");
        System.out.println(reclamation);

        return reclamation;
    }

    // Creates a draft: the vehicle and the description, the rest is filled in later.
    public WorkOrder createDraft(int vehicleId, String description) {
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle with ID " + vehicleId + " does not exist.");
        }

        WorkOrder workOrder = new WorkOrder(0, 0, 0);
        workOrder.setVehicleId(vehicleId);
        workOrder.setDescription(description);

        try {
            workOrderRepository.save(workOrder);
        } catch (SQLException e) {
            System.out.println("Could not save draft work order: " + e.getMessage());
            return null;
        }

        System.out.println("Draft work order created successfully.");
        return workOrder;
    }

    public WorkOrder updateDraft(int workOrderId, int vehicleId, String description, int mechanicId,
            List<Integer> serviceItemIds, String plannedDate, String customerInstructions, String otherComments) {

        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        workOrder.setVehicleId(vehicleId);
        workOrder.setDescription(description);
        workOrder.setMechanicId(mechanicId);
        workOrder.setServiceItemIds(serviceItemIds);
        workOrder.setPlannedDate(plannedDate);
        workOrder.setCustomerInstructions(customerInstructions);
        workOrder.setOtherComments(otherComments);

        try {
            workOrderRepository.save(workOrder);
        } catch (SQLException e) {
            System.out.println("Could not save draft work order: " + e.getMessage());
            return null;
        }

        return workOrder;
    }

    /** True if the service already sits on a work order for the same booking. The catalogue is shared. */
    private boolean isClaimed(int bookingId, int serviceItemId) {
        for (WorkOrder order : getAll()) {

            if ("CANCELLED".equals(order.getStatus())) {
                continue;
            }

            if (order.getBookingId() != bookingId) {
                continue;
            }

            if (order.getServiceItemIds().contains(Integer.valueOf(serviceItemId))) {
                return true;
            }
        }

        return false;
    }

    // The only place that decides which status changes are allowed. Anything not listed is refused.
    private boolean canChangeStatus(String from, String to) {

        if ("IN_PROGRESS".equals(from) && "COMPLETED".equals(to)) {
            return true;
        }

        if ("CREATED".equals(from) && "CANCELLED".equals(to)) {
            return true;
        }

        if ("IN_PROGRESS".equals(from) && "CANCELLED".equals(to)) {
            return true;
        }

        if ("CANCELLED".equals(from) && "IN_PROGRESS".equals(to)) {
            return true;
        }

        if ("CREATED".equals(from) && "CONFIRMED".equals(to)) {
            return true;
        }

        if ("CONFIRMED".equals(from) && "IN_PROGRESS".equals(to)) {
            return true;
        }

        if ("CONFIRMED".equals(from) && "CANCELLED".equals(to)) {
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
            throw new RuleViolationException("Work order " + workOrderId + " cannot be started. Its status is "
                    + workOrder.getStatus() + ".");
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
            throw new RuleViolationException("Work order " + workOrderId + " cannot be completed. Its status is "
                    + workOrder.getStatus() + ".");
        }

        // The price is frozen even when the order is finished without anyone having marked the
        // jobs. The price goes on the line without marking the service as performed, because the
        // invoice should still only be built on the jobs that were actually marked.
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

    // A draft becomes confirmed only once the customer has said yes to the price and the time.
    public boolean confirmWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!canChangeStatus(workOrder.getStatus(), "CONFIRMED")) {
            throw new RuleViolationException("Work order " + workOrderId + " cannot be confirmed. Its status is "
                    + workOrder.getStatus() + ".");
        }

        workOrder.setStatus("CONFIRMED");
        saveWorkOrder(workOrder);

        System.out.println("Work order " + workOrderId + " has been confirmed.");
        return true;
    }

    // A cancelled draft should not leave a booked time behind, so the booking is cancelled and the schedule freed.
    public boolean cancelWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return false;
        }

        if (!canChangeStatus(workOrder.getStatus(), "CANCELLED")) {
            throw new RuleViolationException("Work order " + workOrderId + " cannot be cancelled. Its status is "
                    + workOrder.getStatus() + ".");
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

    /** Marks the services performed and stores the price right now. */
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
            // The price is frozen once. If the service is already marked it keeps its old price.
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
            throw new DataAccessException("Could not read booking " + id, e);
        }
    }

    private Mechanic findMechanic(int id) {
        try {
            return mechanicRepository.findById(id);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read mechanic " + id, e);
        }
    }

    private ServiceItem findServiceItem(int id) {
        try {
            return serviceItemRepository.findById(id);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read service item " + id, e);
        }
    }

    private Vehicle findVehicle(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read vehicle " + id, e);
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
