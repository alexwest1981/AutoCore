package com.wac.autocore.service;

import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The work order's life cycle from created to finished. */
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final MechanicRepository mechanicRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final VehicleRepository vehicleRepository;
    private final MechanicSchedule mechanicSchedule;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            BookingRepository bookingRepository,
                            MechanicRepository mechanicRepository,
                            ServiceItemRepository serviceItemRepository,
                            VehicleRepository vehicleRepository,
                            MechanicSchedule mechanicSchedule) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.mechanicRepository = mechanicRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.mechanicSchedule = mechanicSchedule;
    }

    public List<WorkOrder> getAll() {
        try {
            return workOrderRepository.findAll();
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read work orders", e);
        }
    }

    public WorkOrder findById(int id) {
        try {
            return workOrderRepository.findById(id);
        } catch (DataAccessException e) {
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
            throw new RuleViolationException("Unknown work order type: " + type, "dialog.workorder.unknown_type");
        }

        Booking booking = findBooking(bookingId);
        if (booking == null) {
            throw new NotFoundException("Booking with ID " + bookingId + " does not exist.");
        }

        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            throw new RuleViolationException("No services were given for work order on booking " + bookingId, "dialog.workorder.no_services");
        }

        for (WorkOrder existingOrder : getAll()) {

            if (existingOrder.getStatus() == WorkOrderStatus.CANCELLED) {
                continue;
            }

            if (existingOrder.getBookingId() != bookingId) {
                continue;
            }

            for (Integer serviceItemId : serviceItemIds) {

                if (existingOrder.getServiceItemIds().contains(serviceItemId)) {
                    throw new RuleViolationException("Service " + serviceItemId + " already sits on work order " +
                            existingOrder.getId(), "dialog.workorder.service_already_claimed");
                }
            }
        }

        Mechanic mechanic = findMechanic(mechanicId);
        if (mechanic == null) {
            throw new NotFoundException("Mechanic with ID " + mechanicId + " does not exist.");
        }

        if (!mechanic.isAvailable()) {
            throw new RuleViolationException("Mechanic " + mechanicId + " is not available.", "dialog.workorder.mechanic_busy");
        }

        WorkOrder workOrder = new WorkOrder(0, bookingId, mechanicId);
        workOrder.setType(type);
        // The order is about the car the booking is for, so it carries the same vehicle. Without
        // this the work order's own vehicle stayed at zero and only the booking's copy was shown.
        workOrder.setVehicleId(booking.getVehicleId());

        for (Integer serviceItemId : serviceItemIds) {
            workOrder.addServiceItem(serviceItemId);
            // The package name follows the service from the booking, so the invoice can group it.
            workOrder.setServicePackage(serviceItemId, booking.getServicePackageName(serviceItemId));
        }

        try {
            workOrderRepository.save(workOrder);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save work order", e);
        }

        workOrder.setStatus(WorkOrderStatus.CREATED);
        booking.setStatus(BookingStatus.WORK_ORDER_CREATED);
        saveBooking(booking);


        return workOrder;
    }

    // A new order that redoes performed work, so the original stays as history. The guard against
    // one service on two orders does not apply here: the work is redone and is never charged.
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {
        WorkOrder original = findById(originalWorkOrderId);

        if (original == null) {
            throw new NotFoundException("Work order with ID " + originalWorkOrderId + " does not exist.");
        }

        // Only work that has actually been performed can be reclaimed.
        if (original.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new RuleViolationException("Work order " + originalWorkOrderId + " is not completed.",
                    "dialog.workorder.reclamation_not_completed");
        }

        Mechanic mechanic = findMechanic(original.getMechanicId());
        if (mechanic == null) {
            throw new NotFoundException("Mechanic with ID " + original.getMechanicId() + " does not exist.");
        }

        if (!mechanic.isAvailable()) {
            throw new RuleViolationException("Mechanic " + original.getMechanicId() + " is not available.",
                    "dialog.workorder.mechanic_busy");
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save reclamation", e);
        }
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save draft work order", e);
        }

        return workOrder;
    }

    public WorkOrder updateDraft(DraftChanges changes) {

        WorkOrder workOrder = findById(changes.getWorkOrderId());

        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + changes.getWorkOrderId() + " does not exist.");
        }

        workOrder.setVehicleId(changes.getVehicleId());
        workOrder.setDescription(changes.getDescription());
        workOrder.setMechanicId(changes.getMechanicId());
        workOrder.setServiceItemIds(changes.getServiceItemIds());
        workOrder.setPlannedDate(changes.getPlannedDate());
        workOrder.setPlannedTime(changes.getPlannedTime());
        workOrder.setCustomerInstructions(changes.getCustomerInstructions());
        workOrder.setOtherComments(changes.getOtherComments());

        try {
            workOrderRepository.save(workOrder);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save draft work order", e);
        }

        return workOrder;
    }

    /** True if the service already sits on a work order for the same booking. The catalogue is shared. */
    private boolean isClaimed(int bookingId, int serviceItemId) {
        for (WorkOrder order : getAll()) {

            if (order.getStatus() == WorkOrderStatus.CANCELLED) {
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

    public boolean startWorkOrder(int workOrderId) {

        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        // Refused before anything is saved, so a no leaves the mechanic and the booking as they were.
        workOrder.changeStatusTo(WorkOrderStatus.IN_PROGRESS);

        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
        Booking booking = findBooking(workOrder.getBookingId());

        if (mechanic != null) {
            mechanic.setAvailable(false);
            saveMechanic(mechanic);
        }

        if (booking != null) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            saveBooking(booking);
        }

        // The price is frozen when the work starts, not when it is finished. A price change
        // while the job is running must not reach this order.
        Map<Integer, Integer> priser = new LinkedHashMap<Integer, Integer>(workOrder.getCompletedServicePrices());

        for (Integer serviceItemId : workOrder.getServiceItemIds()) {

            if (!priser.containsKey(serviceItemId)) {
                ServiceItem serviceItem = findServiceItem(serviceItemId.intValue());

                if (serviceItem != null) {
                    priser.put(serviceItemId, Integer.valueOf(serviceItem.getPrice()));
                }
            }
        }

        workOrder.setCompletedServicePrices(priser);
        saveWorkOrder(workOrder);
        return true;
    }

    public boolean completeWorkOrder(int workOrderId) {

        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        // The price is frozen even when the order is finished without anyone having marked the
        // jobs. The price goes on the line without marking the service as performed, because the
        // invoice should still only be built on the jobs that were actually marked.
        Map<Integer, Integer> priser = new LinkedHashMap<Integer, Integer>(workOrder.getCompletedServicePrices());

        for (Integer serviceItemId : workOrder.getServiceItemIds()) {

            if (!priser.containsKey(serviceItemId)) {

                ServiceItem serviceItem = findServiceItem(serviceItemId.intValue());

                if (serviceItem != null) {
                    priser.put(serviceItemId, Integer.valueOf(serviceItem.getPrice()));
                }
            }
        }

        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
        Booking booking = findBooking(workOrder.getBookingId());

        workOrder.setCompletedServicePrices(priser);
        workOrder.changeStatusTo(WorkOrderStatus.COMPLETED);

        saveWorkOrder(workOrder);

        if (mechanic != null) {
            mechanic.setAvailable(true);
            saveMechanic(mechanic);
        }

        if (booking != null) {
            booking.setStatus(BookingStatus.COMPLETED);
            saveBooking(booking);
        }
        return true;
    }

    // A draft becomes confirmed only once the customer has said yes to the price and the time.
    public boolean confirmWorkOrder(int workOrderId) {

        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        workOrder.changeStatusTo(WorkOrderStatus.CONFIRMED);
        saveWorkOrder(workOrder);
        return true;
    }

    // A cancelled draft should not leave a booked time behind, so the booking is cancelled and the schedule freed.
    public boolean cancelWorkOrder(int workOrderId) {

        WorkOrder workOrder = findById(workOrderId);

        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        // Refused before anything is saved, so a no cannot cancel the booking or free its time.
        workOrder.changeStatusTo(WorkOrderStatus.CANCELLED);

        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
        Booking booking = findBooking(workOrder.getBookingId());

        if (mechanic != null) {
            mechanic.setAvailable(true);
            saveMechanic(mechanic);
        }

        if (booking != null) {
            booking.setStatus(BookingStatus.CANCELLED);
            saveBooking(booking);
            mechanicSchedule.cancelSlotForBooking(booking.getId());
            mechanicSchedule.syncFromDatabase();
        }

        saveWorkOrder(workOrder);
        return true;
    }

    /** Marks the services performed and stores the price right now. */
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null) {
            throw new NotFoundException("Work order with ID " + workOrderId + " does not exist.");
        }

        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new RuleViolationException("Services on work order " + workOrderId
                    + " cannot be marked as performed. Its status is " + workOrder.getStatus() + ".");
        }

        if (serviceItemIds == null || serviceItemIds.length == 0) {
            throw new RuleViolationException("No services were given for work order " + workOrderId + ".");
        }

        for (int serviceItemId : serviceItemIds) {
            ServiceItem serviceItem = findServiceItem(serviceItemId);
            if (serviceItem == null) {
                throw new NotFoundException("Service item with ID " + serviceItemId + " does not exist.");
            }
            // The price is frozen once. If the service is already marked it keeps its old price.
            Integer alreadyFrozen = workOrder.getCompletedServicePrice(serviceItemId);
            int frozenPrice = alreadyFrozen != null ? alreadyFrozen.intValue() : serviceItem.getPrice();
            workOrder.markServiceAsCompleted(serviceItemId, frozenPrice);
        }

        saveWorkOrder(workOrder);

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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read booking " + id, e);
        }
    }

    private Mechanic findMechanic(int id) {
        try {
            return mechanicRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read mechanic " + id, e);
        }
    }

    private ServiceItem findServiceItem(int id) {
        try {
            return serviceItemRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read service item " + id, e);
        }
    }

    private Vehicle findVehicle(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read vehicle " + id, e);
        }
    }

    private void saveBooking(Booking booking) {
        try {
            bookingRepository.save(booking);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not update booking " + booking.getId(), e);
        }
    }

    private void saveMechanic(Mechanic mechanic) {
        try {
            mechanicRepository.save(mechanic);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not update mechanic " + mechanic.getId(), e);
        }
    }

    private void saveWorkOrder(WorkOrder workOrder) {
        try {
            workOrderRepository.save(workOrder);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not update work order " + workOrder.getId(), e);
        }
    }
}
