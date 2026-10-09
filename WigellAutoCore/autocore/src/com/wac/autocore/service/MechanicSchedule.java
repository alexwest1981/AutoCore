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

/** The mechanics' booked hours. */
public class MechanicSchedule {

    public static final int START_HOUR = 7;
    public static final int END_HOUR = 16;
    public static final int WORK_HOURS_PER_DAY = END_HOUR - START_HOUR; // 9 hours (7-16)

    // Singleton instance for the shared state in the UI
    private static final MechanicSchedule INSTANCE = new MechanicSchedule();

    public static MechanicSchedule getInstance() {
        return INSTANCE;
    }

    // Key: "mechanicId:YYYY-MM-DD:hour"
    private final Map<String, TimeSlot> slots = new HashMap<String, TimeSlot>();
    private boolean seeded = false;

    public MechanicSchedule() {
        initDefaultSeedData();
    }

    private String slotKey(int mechanicId, LocalDate date, int hour) {
        return mechanicId + ":" + date.toString() + ":" + hour;
    }

    /** Reads the work orders from the database and maps them onto hour slots. */
    public synchronized void syncFromDatabase() {
        String sql = "SELECT wo.id AS wo_id, wo.mechanic_id, wo.status AS wo_status, "
                + "b.id AS booking_id, b.date, b.start_time, b.description, "
                + "v.registration_number, c.name AS customer_name "
                + "FROM work_orders wo "
                + "JOIN bookings b ON wo.booking_id = b.id "
                + "LEFT JOIN vehicles v ON b.vehicle_id = v.id "
                + "LEFT JOIN customers c ON v.customer_id = c.id "
                + "WHERE wo.status IN ('CREATED', 'CONFIRMED', 'IN_PROGRESS')\n";

        java.util.Set<Integer> activeWorkOrderIds = new java.util.HashSet<Integer>();

        try (java.sql.Connection conn = com.wac.autocore.data.Db.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int woId = rs.getInt("wo_id");
                activeWorkOrderIds.add(Integer.valueOf(woId));
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
                    // The booking's own time first. A drop-in is booked on the hour the customer
                    // arrived and should show there, not in the first slot that happens to be free.
                    if (bookAtStoredTime(mechId, date, rs.getString("start_time"), bId, woId, cust, reg, desc) < 0) {
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
            }

            // Slots whose work order is no longer active leave the schedule.
            java.util.Iterator<java.util.Map.Entry<String, TimeSlot>> staleSlots = slots.entrySet().iterator();
            while (staleSlots.hasNext()) {
                TimeSlot slot = staleSlots.next().getValue();
                if (slot.getWorkOrderId() > 0 && !activeWorkOrderIds.contains(Integer.valueOf(slot.getWorkOrderId()))) {
                    staleSlots.remove();
                }
            }
        } catch (Exception ignored) {
            // The schedule can be used before the database has become ready.
        }
    }

    /** The next date after afterDate on which the mechanic has at least one booked hour. */
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

    /** Puts the sample bookings into the schedule the first time the view is used. */
    public synchronized void initDefaultSeedData() {
        if (seeded) return;
        seeded = true;

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Example: mechanic 1 has 08-10 and 13-14 today, so 3 hours booked.
        bookSlotInternal(1, today, 8, 1, 1, "Anna Andersson", "ABC123", "seed.booking.oil_change_filter.description");
        bookSlotInternal(1, today, 9, 1, 1, "Anna Andersson", "ABC123", "seed.schedule.oil_change_continued.description");
        bookSlotInternal(1, today, 13, 2, 2, "Erik Eriksson", "DEF456", "seed.booking.front_brake_inspection.description");

        // Mechanic 1: tomorrow 08-12 and 13-16 -> 7h booked (red / full)
        for (int h = 8; h <= 11; h++) {
            bookSlotInternal(1, tomorrow, h, 3, 5, "Maria Svensson", "GHI789", "seed.booking.full_brake_overhaul.description");
        }
        for (int h = 13; h <= 15; h++) {
            bookSlotInternal(1, tomorrow, h, 3, 5, "Maria Svensson", "GHI789", "seed.booking.full_brake_overhaul.description");
        }

        // Mechanic 1: the day after tomorrow -> 1h booked (green)
        bookSlotInternal(1, today.plusDays(2), 10, 1, 1, "Anna Andersson", "ABC123", "seed.schedule.oil_follow_up.description");

        // Mechanic 2 (Sara Nilsson): today 09-12 and 14-16 -> 5h booked (orange)
        bookSlotInternal(2, today, 9, 4, 3, "Olof Palme", "XYZ999", "seed.booking.brake_calipers_pads.description");
        bookSlotInternal(2, today, 10, 4, 3, "Olof Palme", "XYZ999", "seed.booking.brake_calipers_pads.description");
        bookSlotInternal(2, today, 11, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.brake_system_bleeding.description");
        bookSlotInternal(2, today, 14, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.brake_hose_inspection.description");
        bookSlotInternal(2, today, 15, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.final_brake_inspection.description");

        // Mechanic 2: yesterday 08-09 (green)
        bookSlotInternal(2, today.minusDays(1), 8, 4, 3, "Olof Palme", "XYZ999", "seed.schedule.quick_brake_check.description");

        // Mechanic 3 (Mikael Berg): today 07-08 -> 1h (green)
        bookSlotInternal(3, today, 7, 5, 4, "Sven Melander", "AAA001", "seed.booking.obd2_fault_codes.description");
        // Mechanic 3: tomorrow 10-15 -> 5h (orange)
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

    /** Books the booking's own hour when it exists and is free, otherwise -1 so the caller has to
     *  go looking for a free slot. If the hour is taken by someone else, nobody else's job is moved
     *  out of the way for a drop-in; the job gets a free hour instead. */
    private int bookAtStoredTime(int mechanicId, LocalDate date, String startTime, int bookingId,
                                 int workOrderId, String customer, String vehicleReg, String desc) {
        if (startTime == null || startTime.trim().isEmpty()) {
            return -1;
        }
        int hour;
        try {
            hour = java.time.LocalTime.parse(startTime.trim()).getHour();
        } catch (java.time.format.DateTimeParseException e) {
            return -1;
        }
        if (hour < START_HOUR || hour >= END_HOUR) {
            return -1;
        }
        TimeSlot existing = slots.get(slotKey(mechanicId, date, hour));
        if (existing != null && existing.isBooked()) {
            return -1;
        }
        bookSlotInternal(mechanicId, date, hour, bookingId, workOrderId, customer, vehicleReg, desc);
        return hour;
    }

    /** Every time slot for a mechanic on one day, 07:00 to 16:00. */
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

    /** Books one hour for a mechanic. */
    public synchronized boolean bookSlot(int mechanicId, LocalDate date, int hour, int bookingId, int workOrderId,
                                         String customer, String vehicleReg, String desc) {
        if (hour < START_HOUR || hour >= END_HOUR) {
            return false;
        }
        String key = slotKey(mechanicId, date, hour);
        TimeSlot existing = slots.get(key);
        if (existing != null && existing.isBooked()) {
            return false; // already booked
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

    /** Cancels every hour that belongs to a booking. */
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

    /** The load on one day, 0 to 9 hours. */
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
            level = LoadLevel.FREE;      // 0-2h (green)
        } else if (booked <= 4) {
            level = LoadLevel.MODERATE;  // 3-4h (yellow)
        } else if (booked <= 6) {
            level = LoadLevel.BUSY;      // 5-6h (orange)
        } else {
            level = LoadLevel.FULL;      // 7+h (red)
        }

        return new DayLoad(date, mechanicId, booked, WORK_HOURS_PER_DAY, level, daySlots);
    }

    /** The load per day for a whole week. */
    public synchronized List<DayLoad> getWeekLoads(int mechanicId, LocalDate weekStartDate) {
        LocalDate monday = weekStartDate.with(DayOfWeek.MONDAY);
        List<DayLoad> result = new ArrayList<DayLoad>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            result.add(getDayLoad(mechanicId, day));
        }
        return result;
    }

    /** The month's days with availability. */
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

    /** Clears the hours of a removed mechanic. */
    public synchronized void removeSlotsForMechanic(int mechanicId) {
        java.util.Iterator<Map.Entry<String, TimeSlot>> it = slots.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().getMechanicId() == mechanicId) {
                it.remove();
            }
        }
    }

}
