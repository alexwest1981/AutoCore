package com.wac.autocore.service;

import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Handles bookings and validates their vehicles and times. */
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final MechanicRules mechanicRules;
    private final WorkOrderService workOrderService;

    public BookingService(WorkOrderService workOrderService, MechanicRules mechanicRules,
                          BookingRepository bookingRepository, VehicleRepository vehicleRepository,
                          ServiceItemRepository serviceItemRepository) {
        this.workOrderService = workOrderService;
        this.mechanicRules = mechanicRules;
        this.bookingRepository = bookingRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public List<Booking> getAll() {
        try {
            return bookingRepository.findAll();
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read bookings", e);
        }
    }

    public Booking findById(int id) {
        try {
            return bookingRepository.findById(id);
        } catch (DataAccessException e) {
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
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save booking", e);
        }


        return booking;
    }

    // Drop-in: the customer is standing in the shop with no booked time. The services go in as
    // objects, because WorkOrderPlan hands them out mechanic by mechanic when the orders are made.
    public Booking createDropInBooking(int vehicleId, List<ServiceItem> services, List<Mechanic> team) {
        return createDropInBooking(vehicleId, services, team, LocalDate.now(), LocalTime.now());
    }

    /**
     * The same drop-in, on a day and from an hour the caller decides. The window runs with the first
     * and asks the clock; a proof runs with the second, so its answer is the same at nine in the
     * morning as at midnight.
     */
    public Booking createDropInBooking(int vehicleId, List<ServiceItem> services, List<Mechanic> team,
                                       LocalDate date, LocalTime from) {

        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle with ID " + vehicleId + " does not exist.");
        }
        if (services == null || services.isEmpty() || team == null || team.isEmpty()) {
            throw new RuleViolationException(
                    "A drop-in booking needs at least one service item and one mechanic.");
        }

        List<Integer> mechanicIds = mechanicIdsOf(team);

        // The shop is closed on weekends, and a drop-in happens the day it happens.
        if (!OpeningHours.isOpenOn(date)) {
            throw new RuleViolationException("Drop-in on a closed day: " + date,
                    "dialog.booking.date_weekend");
        }

        Booking booking = new Booking(vehicleId, date, "seed.booking.drop_in.description");
        booking.setServiceItems(services);
        booking.setMechanicIds(mechanicIds);

        // A drop-in happens when the customer walks in, so the booking starts from the current hour.
        // If that one is taken the team gets the next free one, otherwise two customers collide.
        LocalTime startTime = firstFreeHourForTeam(date, mechanicIds, from);
        if (startTime == null) {
            return null;
        }
        booking.setStartTime(startTime);
        // The car is done when the busiest mechanic is done, not when every service's time has been
        // added up — otherwise a single drop-in eats the whole team's day.
        booking.setEndTime(startTime.plusMinutes(mechanicRules.busyMinutes(services, team)));

        try {
            bookingRepository.save(booking);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save booking", e);
        }


        return booking;
    }

    /** The start time a drop-in would get right now, or null when the team's hours are out for the day.
     *  The dialog shows the time before the customer agrees, and then works out the same thing the booking does. */
    public LocalTime dropInStartTime(List<ServiceItem> services, List<Mechanic> team) {
        if (services == null || services.isEmpty() || team == null || team.isEmpty()) {
            return null;
        }
        return firstFreeHourForTeam(LocalDate.now(), mechanicIdsOf(team), LocalTime.now());
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
    private LocalTime firstFreeHourForTeam(LocalDate date, List<Integer> mechanicIds, LocalTime notBefore) {
        List<Booking> bookedToday = new ArrayList<Booking>();
        for (Booking booking : getAll()) {
            if (date.equals(booking.getDate()) && booking.getStartTime() != null
                    && booking.getStatus() != BookingStatus.CANCELLED) {
                bookedToday.add(booking);
            }
        }

        // The working day lives in OpeningHours, the same hours the Kanban draws.
        int fromHour = Math.max(notBefore.getHour(), OpeningHours.firstHour());
        for (int hour = fromHour; hour < OpeningHours.closingHour(); hour++) {
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

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description,
                                 LocalTime startTime,
                                 int mechanicId,
                                 int serviceItemId) {

        int estimatedMinutes = workOrderService.getTotalEstimatedMinutes(serviceItemId);
        LocalTime endTime = (estimatedMinutes > 0) ? startTime.plusMinutes(estimatedMinutes) : startTime.plusMinutes(60);

        Booking booking = new Booking(vehicleId, date, description, startTime, endTime, mechanicId, serviceItemId);
        return saveBooking(booking);
    }

    /** A new booking and a changed one take the same road: the rules first, then the row. */
    public Booking saveBooking(Booking booking) {
        if (findVehicle(booking.getVehicleId()) == null) {
            throw new NotFoundException("Vehicle with ID " + booking.getVehicleId() + " does not exist.");
        }

        refuseUnlessBookable(booking);

        try {
            bookingRepository.save(booking);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save booking", e);
        }
        return booking;
    }

    // A change is tested as if the old booking were gone: its own row is left out of the busy check.
    private void refuseUnlessBookable(Booking booking) {

        // A cancelled booking holds no time, so there is nothing to test.
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        LocalDate date = booking.getDate();
        if (date != null && !OpeningHours.isOpenOn(date)) {
            throw new RuleViolationException("Booking on a closed day: " + date, "dialog.booking.date_weekend");
        }

        List<Mechanic> team = teamOf(booking);
        for (ServiceItem service : servicesOf(booking)) {
            if (!anyQualified(team, service)) {
                throw new RuleViolationException("No mechanic in the team can do service " + service.getId(),
                        "dialog.booking.no_mechanic_for_spec");
            }
        }

        LocalTime start = booking.getStartTime();
        if (date == null || start == null) {
            return;
        }
        LocalTime end = endOf(booking);

        if (end.isAfter(OpeningHours.CLOSES)) {
            throw new RuleViolationException("Booking ends after closing time: " + end,
                    "dialog.booking.closing_time_exceeded");
        }

        if (isTeamBusy(booking, date, start, end)) {
            throw new RuleViolationException("The team is booked between " + start + " and " + end,
                    "dialog.booking.slot_busy_error");
        }
    }

    /** True when another booking holds one of the same mechanics at an overlapping time. */
    private boolean isTeamBusy(Booking booking, LocalDate date, LocalTime start, LocalTime end) {
        List<Integer> team = booking.getMechanicIds();

        for (Booking other : getAll()) {
            // The booking's own row is the old version of itself, not a collision.
            if (other.getId() == booking.getId()) {
                continue;
            }
            if (other.getStatus() == BookingStatus.CANCELLED) {
                continue;
            }
            if (!date.equals(other.getDate()) || other.getStartTime() == null) {
                continue;
            }
            if (!sharesMechanic(other, team)) {
                continue;
            }
            if (start.isBefore(endOf(other)) && other.getStartTime().isBefore(end)) {
                return true;
            }
        }
        return false;
    }

    /** The end time, or one hour after the start when the row has none that makes sense. */
    private static LocalTime endOf(Booking booking) {
        LocalTime start = booking.getStartTime();
        LocalTime end = booking.getEndTime();
        return end != null && end.isAfter(start) ? end : start.plusHours(1);
    }

    private List<Mechanic> teamOf(Booking booking) {
        List<Integer> ids = booking.getMechanicIds();
        List<Mechanic> team = new ArrayList<Mechanic>();
        for (Mechanic mechanic : mechanicRules.getAll()) {
            if (ids.contains(Integer.valueOf(mechanic.getId()))) {
                team.add(mechanic);
            }
        }
        return team;
    }

    private List<ServiceItem> servicesOf(Booking booking) {
        List<Integer> ids = booking.getServiceItemIds();
        List<ServiceItem> services = new ArrayList<ServiceItem>();
        for (ServiceItem service : serviceItemRepository.findAll()) {
            if (ids.contains(Integer.valueOf(service.getId()))) {
                services.add(service);
            }
        }
        return services;
    }

    private boolean anyQualified(List<Mechanic> team, ServiceItem service) {
        for (Mechanic mechanic : team) {
            if (mechanicRules.isMechanicQualified(mechanic, service)) {
                return true;
            }
        }
        return false;
    }

    private Vehicle findVehicle(int id) {
        try {
            Vehicle v = vehicleRepository.findById(id);
            return v;
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read vehicle", e);
        }
    }

    /** The booking's price, worked out from its services. */
    public int priceOf(Booking booking) {
        if (booking == null || booking.getServiceItemIds() == null) {
            return 0;
        }
        List<ServiceItem> services = new ArrayList<ServiceItem>();
        for (int id : booking.getServiceItemIds()) {
            ServiceItem service = serviceItemRepository.findById(id);
            if (service != null) {
                services.add(service);
            }
        }
        return ServiceItem.totalPrice(services);
    }
}
