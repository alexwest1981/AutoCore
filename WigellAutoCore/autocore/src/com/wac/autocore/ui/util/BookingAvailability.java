package com.wac.autocore.ui.util;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** The available and busy hours for the mechanics. */
public final class BookingAvailability {

    public static final LocalTime CLOSING_TIME = LocalTime.of(17, 0);
    public static final int MAX_WORK_MINUTES_PER_DAY = 10 * 60; // 07:00 till 17:00 = 600 minuter

    private BookingAvailability() {}

    /** True when the whole range touches a time already booked for the mechanic. */
    public static boolean isRangeBooked(GarageSystem garage, Mechanic mechanic, LocalDate date,
                                        LocalTime start, LocalTime end, int excludeBookingId) {
        if (mechanic == null || date == null || start == null || end == null) {
            return false;
        }
        // A broken row (end before start) must not silence the whole check.
        LocalTime rangeEnd = end.isAfter(start) ? end : start.plusHours(1);

        // Checked against the schedule (MechanicSchedule). An hour slot is [hour, hour + 1).
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<TimeSlot> slots = schedule.getSlotsForDay(mechanic.getId(), date);
        for (TimeSlot s : slots) {
            if (!s.isBooked()) {
                continue;
            }
            if (excludeBookingId > 0 && s.getBookingId() == excludeBookingId) {
                continue;
            }
            LocalTime slotStart = LocalTime.of(s.getHour(), 0);
            if (slotStart.isBefore(rangeEnd) && start.isBefore(slotStart.plusHours(1))) {
                return true;
            }
        }

        // Checked against the stored bookings in GarageSystem
        if (garage != null) {
            for (Booking b : garage.getBookings()) {
                if (excludeBookingId > 0 && b.getId() == excludeBookingId) {
                    continue;
                }
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                    continue;
                }
                if (b.getMechanicId() != mechanic.getId() || !date.equals(b.getDate()) || b.getStartTime() == null) {
                    continue;
                }
                LocalTime bStart = b.getStartTime();
                LocalTime bEnd = b.getEndTime() != null ? b.getEndTime() : bStart.plusHours(1);
                if (!bEnd.isAfter(bStart)) {
                    bEnd = bStart.plusHours(1);
                }
                if (start.isBefore(bEnd) && bStart.isBefore(rangeEnd)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** True if there is at least one free start time for the whole job that day. */
    public static boolean hasAvailableSlotOnDate(GarageSystem garage, Mechanic mechanic,
                                                 List<Mechanic> qualifiedMechanics,
                                                 LocalDate date, int durationMinutes, int excludeBookingId) {
        if (date == null || durationMinutes <= 0) {
            return false;
        }
        if (date.isBefore(LocalDate.now())) {
            return false;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        if (durationMinutes > MAX_WORK_MINUTES_PER_DAY) {
            return false; // Överstiger hela arbetsdagen (10 timmar = 600 min)
        }

        List<Mechanic> candidates = new ArrayList<Mechanic>();
        if (mechanic != null && mechanic.getId() > 0) {
            candidates.add(mechanic);
        } else if (qualifiedMechanics != null && !qualifiedMechanics.isEmpty()) {
            candidates.addAll(qualifiedMechanics);
        } else if (garage != null) {
            candidates.addAll(garage.getMechanics());
        }

        if (candidates.isEmpty()) {
            for (int h = 7; h <= 16; h++) {
                LocalTime start = LocalTime.of(h, 0);
                if (!start.plusMinutes(durationMinutes).isAfter(CLOSING_TIME)) {
                    return true;
                }
            }
            return false;
        }

        // A job that requires a given team needs the whole team free. If neither a mechanic nor
        // qualified mechanics are given it is enough that one of the team is free, not all —
        // otherwise days with plenty of room disappeared.
        boolean needsWholeTeam = mechanic != null && mechanic.getId() > 0
                || qualifiedMechanics != null && !qualifiedMechanics.isEmpty();

        for (int h = 7; h <= 16; h++) {
            LocalTime start = LocalTime.of(h, 0);
            LocalTime end = start.plusMinutes(durationMinutes);
            if (end.isAfter(CLOSING_TIME)) {
                break;
            }
            boolean slotAvailable = needsWholeTeam;
            for (Mechanic m : candidates) {
                boolean booked = m.getId() > 0
                        && isRangeBooked(garage, m, date, start, end, excludeBookingId);
                if (needsWholeTeam && booked) {
                    slotAvailable = false;
                    break;
                }
                if (!needsWholeTeam && !booked) {
                    slotAvailable = true;
                    break;
                }
            }
            if (slotAvailable) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTeamBooked(GarageSystem garage, List<Mechanic> team,
                                       LocalDate date, LocalTime startTime, LocalTime endTime,
                                       int excludeBookingId) {
        if (team == null || team.isEmpty()) {
            return false;
        }
        for (Mechanic m : team) {
            if (m.getId() > 0 && isRangeBooked(garage, m, date, startTime, endTime, excludeBookingId)) {
                return true;
            }
        }
        return false;
    }
}

