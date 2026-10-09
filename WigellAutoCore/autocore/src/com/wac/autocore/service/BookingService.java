package com.wac.autocore.service;

import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Handles bookings and validates their vehicles and times. */
public class BookingService {

    private final BookingRepository bookingRepository = new BookingRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();
    private final MechanicRules mechanicRules = new MechanicRules();
    private final WorkOrderService workOrderService;

    public BookingService(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public List<Booking> getAll() {
        try {
            return bookingRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read bookings: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Booking findById(int id) {
        try {
            return bookingRepository.findById(id);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read booking", e);
        }
    }

    public Booking createBooking(int vehicleId, LocalDate date, String description) {

        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle with ID " + vehicleId + " does not exist.");
        }

        Booking booking = new Booking(vehicleId, date, description);
        try {
            bookingRepository.save(booking);
        } catch (SQLException e) {
            System.out.println("Could not save booking: " + e.getMessage());
            return null;
        }

        System.out.println("Booking created successfully.");
        System.out.println(booking);

        return booking;
    }

    // Drop-in: the customer is standing in the shop with no booked time. The services go in as
    // objects, because WorkOrderPlan hands them out mechanic by mechanic when the orders are made.
    public Booking createDropInBooking(int vehicleId, List<ServiceItem> services, List<Mechanic> team) {

        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle with ID " + vehicleId + " does not exist.");
        }
        if (services == null || services.isEmpty() || team == null || team.isEmpty()) {
            System.out.println("A drop-in booking needs at least one service item and one mechanic.");
            return null;
        }

        List<Integer> mechanicIds = mechanicIdsOf(team);

        LocalDate date = LocalDate.now();
        Booking booking = new Booking(vehicleId, date, "seed.booking.drop_in.description");
        booking.setServiceItems(services);
        booking.setMechanicIds(mechanicIds);

        // A drop-in happens when the customer walks in, so the booking starts from the current hour.
        // If that one is taken the team gets the next free one, otherwise two customers collide.
        LocalTime startTime = firstFreeHourForTeam(date, mechanicIds);
        if (startTime == null) {
            System.out.println("The team has no free hour left today.");
            return null;
        }
        booking.setStartTime(startTime);
        // The car is done when the busiest mechanic is done, not when every service's time has been
        // added up — otherwise a single drop-in eats the whole team's day.
        booking.setEndTime(startTime.plusMinutes(mechanicRules.busyMinutes(services, team)));

        try {
            bookingRepository.save(booking);
        } catch (SQLException e) {
            System.out.println("Could not save booking: " + e.getMessage());
            return null;
        }

        System.out.println(booking);

        return booking;
    }

    /** The start time a drop-in would get right now, or null when the team's hours are out for the day.
     *  The dialog shows the time before the customer agrees, and then works out the same thing the booking does. */
    public LocalTime dropInStartTime(List<ServiceItem> services, List<Mechanic> team) {
        if (services == null || services.isEmpty() || team == null || team.isEmpty()) {
            return null;
        }
        return firstFreeHourForTeam(LocalDate.now(), mechanicIdsOf(team));
    }

    private List<Integer> mechanicIdsOf(List<Mechanic> team) {
        List<Integer> ids = new ArrayList<Integer>();
        for (Mechanic mechanic : team) {
            ids.add(Integer.valueOf(mechanic.getId()));
        }
        return ids;
    }

    // The first hour today where the whole team is free, counted from the current hour, or null when
    // the day is full. Everyone in the team has to be free, otherwise one mechanic holds two jobs at once.
    private LocalTime firstFreeHourForTeam(LocalDate date, List<Integer> mechanicIds) {
        List<Booking> bookedToday = new ArrayList<Booking>();
        for (Booking booking : getAll()) {
            if (date.equals(booking.getDate()) && booking.getStartTime() != null
                    && !"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                bookedToday.add(booking);
            }
        }

        // The working day lives in MechanicSchedule, the same hours the Kanban draws.
        int fromHour = Math.max(LocalTime.now().getHour(), MechanicSchedule.START_HOUR);
        for (int hour = fromHour; hour < MechanicSchedule.END_HOUR; hour++) {
            if (isHourFree(bookedToday, mechanicIds, hour)) {
                return LocalTime.of(hour, 0);
            }
        }

        return null;
    }

    // The hour [h, h+1) is free when none of the team's bookings touch it.
    private boolean isHourFree(List<Booking> bookedToday, List<Integer> mechanicIds, int hour) {
        LocalTime hourStart = LocalTime.of(hour, 0);
        LocalTime hourEnd = hourStart.plusHours(1);
        for (Booking booking : bookedToday) {
            if (!sharesMechanic(booking, mechanicIds)) {
                continue;
            }
            LocalTime start = booking.getStartTime();
            LocalTime end = booking.getEndTime() != null && booking.getEndTime().isAfter(start)
                    ? booking.getEndTime() : start.plusHours(1);
            if (hourStart.isBefore(end) && start.isBefore(hourEnd)) {
                return false;
            }
        }
        return true;
    }

    private boolean sharesMechanic(Booking booking, List<Integer> mechanicIds) {
        for (Integer mechanicId : booking.getMechanicIds()) {
            if (mechanicIds.contains(mechanicId)) {
                return true;
            }
        }
        return false;
    }

    public Booking createBooking(int vehicleId, LocalDate date, String description, LocalTime startTime, int mechanicId, int serviceItemId) throws SQLException {
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle with ID " + vehicleId + " does not exist.");
        }

        ServiceItem serviceItem = serviceItemRepository.findAll().stream()
                .filter(item -> item.getId() == serviceItemId)
                .findFirst()
                .orElse(null);
        if (serviceItem == null) {
            System.out.println("Service item with ID " + serviceItemId + " does not exist ");
        }

        int estimatedMinutes = workOrderService.getTotalEstimatedMinutes(serviceItemId);

        LocalTime endTime = (estimatedMinutes > 0) ? startTime.plusMinutes(estimatedMinutes) : startTime.plusMinutes(60);

        if (isMechanicOccupied(mechanicId, date, startTime, endTime)) {
            throw new IllegalArgumentException("Mekanikern är redan bokad under denna tid (" + startTime + " - " + endTime + ")!");
        }

        Booking booking = new Booking(vehicleId, date, description, startTime, endTime, mechanicId, serviceItemId);

        bookingRepository.save(booking);

        System.out.println("CREATED");
        System.out.println(booking);

        return booking;
    }

    private Vehicle findVehicle(int id) {
        try {
            Vehicle v = vehicleRepository.findById(id);
            return v;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read vehicle", e);
        }
    }
    private boolean isMechanicOccupied(int mechanicId, LocalDate date, LocalTime newStart, LocalTime newEnd) throws SQLException {

        if (mechanicId == 0) {
            return false;
        }
        return bookingRepository.findAll().stream()
                .filter(b -> b.getMechanicId() == mechanicId && date.equals(b.getDate()))
                .anyMatch(b -> {
                    LocalTime existingStart = b.getStartTime();
                    LocalTime existingEnd = b.getEndTime();

                    if (existingStart == null || existingEnd == null) {
                        return false;
                    }
                    return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
                });
    }
}
