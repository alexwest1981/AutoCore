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
        if (mechanic == null || date == null) {
            return false;
        }

        LocalTime slotStart = LocalTime.of(hour, 0);
        LocalTime slotEnd = slotStart.plusHours(1);

        // 1. Kontrollera mot schemat (MechanicSchedule)
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        List<MechanicSchedule.TimeSlot> slots = schedule.getSlotsForDay(mechanic.getId(), date);
        for (MechanicSchedule.TimeSlot s : slots) {
            if (s.getHour() == hour && s.isBooked()) {
                if (excludeBookingId > 0 && s.getBookingId() == excludeBookingId) {
                    continue;
                }
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
                if (b.getMechanicId() == mechanic.getId() && date.equals(b.getDate())) {
                    if (b.getStartTime() != null) {
                        LocalTime bStart = b.getStartTime();
                        LocalTime bEnd = b.getEndTime() != null ? b.getEndTime() : bStart.plusHours(1);
                        if (bEnd.isBefore(bStart) || bEnd.equals(bStart)) {
                            bEnd = bStart.plusHours(1);
                        }
                        if (slotStart.isBefore(bEnd) && bStart.isBefore(slotEnd)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
