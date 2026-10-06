package com.wac.autocore.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.wac.autocore.model.LoadLevel;
import com.wac.autocore.model.TimeSlot;
import com.wac.autocore.model.DayLoad;
import com.wac.autocore.model.MonthDayStatus;

/** Mekanikernas bokade timmar. */
public class MechanicSchedule {

    public static final int START_HOUR = 7;
    public static final int END_HOUR = 16;
    public static final int WORK_HOURS_PER_DAY = END_HOUR - START_HOUR; // 9 timmar (7-16)





    // Singleton instance för delat tillstånd i UI
    private static final MechanicSchedule INSTANCE = new MechanicSchedule();

    public static MechanicSchedule getInstance() {
        return INSTANCE;
    }

    // Nyckel: "mechanicId:YYYY-MM-DD:hour"
    private final Map<String, TimeSlot> slots = new HashMap<String, TimeSlot>();
    private boolean seeded = false;
    private boolean databaseSyncEnabled = true;

    public MechanicSchedule() {
        initDefaultSeedData();
    }

    private String slotKey(int mechanicId, LocalDate date, int hour) {
        return mechanicId + ":" + date.toString() + ":" + hour;
    }

/** Läser in arbetsordrarna från databasen och mappar dem till timluckor. */
    public synchronized void syncFromDatabase() {
        if (!databaseSyncEnabled) return;
        String sql = "SELECT wo.id AS wo_id, wo.mechanic_id, wo.status AS wo_status, "
                + "b.id AS booking_id, b.date, b.description, "
                + "v.registration_number, c.name AS customer_name "
                + "FROM work_orders wo "
                + "JOIN bookings b ON wo.booking_id = b.id "
                + "LEFT JOIN vehicles v ON b.vehicle_id = v.id "
                + "LEFT JOIN customers c ON v.customer_id = c.id "
                + "WHERE wo.status IN ('CREATED', 'IN_PROGRESS')\n";

        try (java.sql.Connection conn = com.wac.autocore.data.Db.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int woId = rs.getInt("wo_id");
                int mechId = rs.getInt("mechanic_id");
                int bId = rs.getInt("booking_id");
                String dateStr = rs.getString("date");
                if (dateStr == null || dateStr.trim().isEmpty()) continue;
                LocalDate date = LocalDate.parse(dateStr);
                String desc = rs.getString("description");
                String reg = rs.getString("registration_number");
                String cust = rs.getString("customer_name");

                boolean alreadyBooked = false;
                for (TimeSlot ts : slots.values()) {
                    if (ts.getWorkOrderId() == woId) {
                        alreadyBooked = true;
                        break;
                    } else if (ts.getBookingId() == bId && ts.getWorkOrderId() == 0) {
                        ts.setWorkOrderId(woId);
                        alreadyBooked = true;
                        break;
                    }
                }

                if (!alreadyBooked) {
                    for (int hour = START_HOUR + 1; hour < END_HOUR; hour++) {
                        String key = slotKey(mechId, date, hour);
                        TimeSlot existing = slots.get(key);
                        if (existing == null || !existing.isBooked()) {
                            bookSlotInternal(mechId, date, hour, bId, woId, cust, reg, desc);
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Schemat kan användas innan databasen hunnit bli klar.
        }
    }

/** Nästa datum efter afterDate då mekanikern har minst en bokad timme. */
    public synchronized LocalDate getNextBookingDate(int mechanicId, LocalDate afterDate) {
        syncFromDatabase();
        LocalDate nextDate = null;
        for (TimeSlot slot : slots.values()) {
            if (slot.getMechanicId() == mechanicId && slot.isBooked()) {
                LocalDate d = slot.getDate();
                if (d != null && d.isAfter(afterDate)) {
                    if (nextDate == null || d.isBefore(nextDate)) {
                        nextDate = d;
                    }
                }
            }
        }
        return nextDate;
    }

    /** Lägger in exempelbokningar i schemat första gången vyn används. */
    public synchronized void initDefaultSeedData() {
        if (seeded) return;
        seeded = true;

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

// Exempel: mekaniker 1 har 08-10 och 13-14 i dag, alltså 3 timmar bokade.
        bookSlotInternal(1, today, 8, 1, 1, "Anna Andersson", "ABC123", "seed.booking.oil_change_filter.description");
        bookSlotInternal(1, today, 9, 1, 1, "Anna Andersson", "ABC123", "seed.schedule.oil_change_continued.description");
        bookSlotInternal(1, today, 13, 2, 2, "Erik Eriksson", "DEF456", "seed.booking.front_brake_inspection.description");

        // Mekaniker 1: Imorgon 08-12 och 13-16 -> 7h bokade (Röd / Full)
        for (int h = 8; h <= 11; h++) {
            bookSlotInternal(1, tomorrow, h, 3, 5, "Maria Svensson", "GHI789", "seed.booking.full_brake_overhaul.description");
        }
        for (int h = 13; h <= 15; h++) {
            bookSlotInternal(1, tomorrow, h, 3, 5, "Maria Svensson", "GHI789", "seed.booking.full_brake_overhaul.description");
        }

        // Mekaniker 1: Dagen efter imorgon -> 1h bokad (Grön)
        bookSlotInternal(1, today.plusDays(2), 10, 1, 1, "Anna Andersson", "ABC123", "seed.schedule.oil_follow_up.description");

        // Mekaniker 2 (Sara Nilsson): Idag 09-12 och 14-16 -> 5h bokade (Orange)
        bookSlotInternal(2, today, 9, 4, 3, "Olof Palme", "XYZ999", "seed.booking.brake_calipers_pads.description");
        bookSlotInternal(2, today, 10, 4, 3, "Olof Palme", "XYZ999", "seed.booking.brake_calipers_pads.description");
        bookSlotInternal(2, today, 11, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.brake_system_bleeding.description");
        bookSlotInternal(2, today, 14, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.brake_hose_inspection.description");
        bookSlotInternal(2, today, 15, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.final_brake_inspection.description");

        // Mekaniker 2: Igår 08-09 (Grön)
        bookSlotInternal(2, today.minusDays(1), 8, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.quick_brake_check.description");

        // Mekaniker 3 (Mikael Berg): Idag 07-08 -> 1h (Grön)
        bookSlotInternal(3, today, 7, 5, 4, "Sven Melander", "AAA001", "seed.booking.obd2_fault_codes.description");
        // Mekaniker 3: Imorgon 10-15 -> 5h (Orange)
        for (int h = 10; h <= 14; h++) {
            bookSlotInternal(3, tomorrow, h, 6, 6, "Gustav Vasa", "BBB002", "seed.booking.electronic_fault_diagnosis.description");
        }
    }

    private void bookSlotInternal(int mechanicId, LocalDate date, int hour, int bookingId, int workOrderId,
                                  String customer, String vehicleReg, String desc) {
        TimeSlot slot = new TimeSlot(mechanicId, date, hour);
        slot.setBooked(true);
        slot.setBookingId(bookingId);
        slot.setWorkOrderId(workOrderId);
        slot.setCustomerName(customer);
        slot.setVehicleReg(vehicleReg);
        slot.setDescription(desc);
        slots.put(slotKey(mechanicId, date, hour), slot);
    }

/** Alla tidsslottar för en mekaniker en dag, 07:00 till 16:00. */
    public synchronized List<TimeSlot> getSlotsForDay(int mechanicId, LocalDate date) {
        syncFromDatabase();
        List<TimeSlot> result = new ArrayList<TimeSlot>();
        for (int hour = START_HOUR; hour < END_HOUR; hour++) {
            String key = slotKey(mechanicId, date, hour);
            TimeSlot slot = slots.get(key);
            if (slot == null) {
                slot = new TimeSlot(mechanicId, date, hour);
            }
            result.add(slot);
        }
        return result;
    }

/** Bokar en timme för en mekaniker. */
    public synchronized boolean bookSlot(int mechanicId, LocalDate date, int hour, int bookingId, int workOrderId,
                                         String customer, String vehicleReg, String desc) {
        if (hour < START_HOUR || hour >= END_HOUR) {
            return false;
        }
        String key = slotKey(mechanicId, date, hour);
        TimeSlot existing = slots.get(key);
        if (existing != null && existing.isBooked()) {
            return false; // Redan bokad
        }

        TimeSlot slot = new TimeSlot(mechanicId, date, hour);
        slot.setBooked(true);
        slot.setBookingId(bookingId);
        slot.setWorkOrderId(workOrderId);
        slot.setCustomerName(customer);
        slot.setVehicleReg(vehicleReg);
        slot.setDescription(desc);
        slots.put(key, slot);
        return true;
    }

/** Avbokar alla timmar som hör till en bokning. */
    public synchronized boolean cancelSlotForBooking(int bookingId) {
        if (bookingId <= 0) return false;
        boolean removed = false;
        java.util.Iterator<java.util.Map.Entry<String, TimeSlot>> it = slots.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<String, TimeSlot> entry = it.next();
            if (entry.getValue().getBookingId() == bookingId) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

/** Belastningen en dag, 0 till 9 timmar. */
    public synchronized DayLoad getDayLoad(int mechanicId, LocalDate date) {
        List<TimeSlot> daySlots = getSlotsForDay(mechanicId, date);
        int booked = 0;
        for (TimeSlot s : daySlots) {
            if (s.isBooked()) {
                booked++;
            }
        }

        LoadLevel level;
        if (booked <= 2) {
            level = LoadLevel.FREE;      // 0-2h (Grön)
        } else if (booked <= 4) {
            level = LoadLevel.MODERATE;  // 3-4h (Gul)
        } else if (booked <= 6) {
            level = LoadLevel.BUSY;      // 5-6h (Orange)
        } else {
            level = LoadLevel.FULL;      // 7+h (Röd)
        }

        return new DayLoad(date, mechanicId, booked, WORK_HOURS_PER_DAY, level, daySlots);
    }

/** Belastningen per dag en hel vecka. */
    public synchronized List<DayLoad> getWeekLoads(int mechanicId, LocalDate weekStartDate) {
        LocalDate monday = weekStartDate.with(DayOfWeek.MONDAY);
        List<DayLoad> result = new ArrayList<DayLoad>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            result.add(getDayLoad(mechanicId, day));
        }
        return result;
    }

/** Månadens dagar med tillgänglighet. */
    public synchronized List<MonthDayStatus> getMonthDays(int mechanicId, YearMonth yearMonth, boolean mechanicAvailableFlag) {
        List<MonthDayStatus> result = new ArrayList<MonthDayStatus>();
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate startGrid = firstOfMonth.with(DayOfWeek.MONDAY);
        LocalDate lastOfMonth = yearMonth.atDay(daysInMonth);
        LocalDate endGrid = lastOfMonth.with(DayOfWeek.SUNDAY);

        LocalDate cur = startGrid;
        while (!cur.isAfter(endGrid)) {
            boolean inCurrentMonth = cur.getMonth() == yearMonth.getMonth();
            boolean isWeekend = (cur.getDayOfWeek() == DayOfWeek.SATURDAY || cur.getDayOfWeek() == DayOfWeek.SUNDAY);

            int booked = 0;
            LoadLevel level = LoadLevel.FREE;
            if (inCurrentMonth && !isWeekend) {
                for (TimeSlot s : getSlotsForDay(mechanicId, cur)) {
                    if (s.isBooked()) booked++;
                }
                if (booked <= 2) {
                    level = LoadLevel.FREE;
                } else if (booked <= 4) {
                    level = LoadLevel.MODERATE;
                } else if (booked <= 6) {
                    level = LoadLevel.BUSY;
                } else {
                    level = LoadLevel.FULL;
                }
            }
            result.add(new MonthDayStatus(cur, inCurrentMonth, isWeekend, mechanicAvailableFlag, booked, level));
            cur = cur.plusDays(1);
        }

        return result;
    }

/** Rensar en borttagen mekanikers timmar. */
    public synchronized void removeSlotsForMechanic(int mechanicId) {
        java.util.Iterator<Map.Entry<String, TimeSlot>> it = slots.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().getMechanicId() == mechanicId) {
                it.remove();
            }
        }
    }

}
