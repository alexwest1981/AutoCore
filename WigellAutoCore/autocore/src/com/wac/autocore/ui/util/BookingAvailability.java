package com.wac.autocore.ui.util;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.MechanicSchedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Hjälpklass för att kontrollera tillgänglighet och upptagna timmar för mekaniker.
 */
public final class BookingAvailability {

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
}
