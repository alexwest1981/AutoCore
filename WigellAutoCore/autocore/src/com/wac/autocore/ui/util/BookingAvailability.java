package com.wac.autocore.ui.util;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hjälpklass för att kontrollera tillgänglighet och upptagna timmar för mekaniker.
 */
public final class BookingAvailability {

    public static final LocalTime OPENING_TIME = LocalTime.of(7, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(17, 0);
    public static final int MAX_WORK_MINUTES_PER_DAY = 10 * 60; // 07:00 till 17:00 = 600 minuter

    private BookingAvailability() {}

    /**
     * Kontrollerar om en specifik timme är upptagen för en mekaniker ett visst datum.
     */
    public static boolean isHourBooked(GarageSystem garage, Mechanic mechanic, LocalDate date, int hour, int excludeBookingId) {
        LocalTime slotStart = LocalTime.of(hour, 0);
        return isRangeBooked(garage, mechanic, date, slotStart, slotStart.plusHours(1), excludeBookingId);
    }

    /**
     * True när intervallet [start, end) rör en tid som redan är bokad för mekanikern den dagen.
     *
     * Kontrollen måste omfatta hela den tid bokningen tar, inte bara den timme den startar i: ett
     * jobb på 210 minuter som börjar 12:00 går in i en bokning 13:00-16:30, och en kontroll av bara
     * starttimmen släppte igenom den överlappningen.
     */
    public static boolean isRangeBooked(GarageSystem garage, Mechanic mechanic, LocalDate date,
                                        LocalTime start, LocalTime end, int excludeBookingId) {
        if (mechanic == null || date == null || start == null || end == null) {
            return false;
        }
        // En trasig rad (slut före start) får inte tysta hela kontrollen.
        LocalTime rangeEnd = end.isAfter(start) ? end : start.plusHours(1);

        // 1. Kontrollera mot schemat (MechanicSchedule). En timlucka är [timme, timme + 1).
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<MechanicSchedule.TimeSlot> slots = schedule.getSlotsForDay(mechanic.getId(), date);
        for (MechanicSchedule.TimeSlot s : slots) {
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

        // 2. Kontrollera mot sparade bokningar i GarageSystem
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

    /**
     * Kontrollerar om en specifik starttid och tidslängd kan bokas för mekanikern den dagen.
     * Returnerar false om arbetet slutar efter stängningstid (17:00), infaller utanför öppettider
     * eller krockar med befintlig bokning.
     */
    public static boolean isSlotAvailable(GarageSystem garage, Mechanic mechanic, LocalDate date,
                                          LocalTime startTime, int durationMinutes, int excludeBookingId) {
        if (date == null || startTime == null || durationMinutes <= 0) {
            return false;
        }
        if (date.isBefore(LocalDate.now())) {
            return false;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        if (startTime.isBefore(OPENING_TIME) || endTime.isAfter(CLOSING_TIME)) {
            return false;
        }
        if (mechanic != null && mechanic.getId() > 0) {
            return !isRangeBooked(garage, mechanic, date, startTime, endTime, excludeBookingId);
        }
        return true;
    }

    /**
     * Kontrollerar om det finns minst en ledig starttid för den angivna tidslängden ett visst datum.
     * Tar hänsyn till stängningstid (17:00), helger, historiska datum och befintliga bokningar.
     */
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

        for (int h = 7; h <= 16; h++) {
            LocalTime start = LocalTime.of(h, 0);
            LocalTime end = start.plusMinutes(durationMinutes);
            if (end.isAfter(CLOSING_TIME)) {
                break;
            }
            boolean slotAvailable = true;
            for (Mechanic m : candidates) {
                if (m.getId() > 0 && isRangeBooked(garage, m, date, start, end, excludeBookingId)) {
                    slotAvailable = false;
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

