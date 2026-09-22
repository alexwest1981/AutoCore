package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.WorkOrder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tjänst som hanterar mekanikers schemaläggning och tidsbokningar (07:00 - 16:00).
 * Beräknar arbetsbelastning (grön -> gul -> orange -> röd) och tillgänglighet för
 * dag-, vecko- och månadsvyer.
 */
public class MechanicSchedule {

    public static final int START_HOUR = 7;
    public static final int END_HOUR = 16;
    public static final int WORK_HOURS_PER_DAY = END_HOUR - START_HOUR; // 9 timmar (7-16)

    public enum LoadLevel {
        FREE("free", "#10b981", "Grön"),        // 0-2 timmar
        MODERATE("moderate", "#eab308", "Gul"),  // 3-4 timmar
        BUSY("busy", "#f97316", "Orange"),       // 5-6 timmar
        FULL("full", "#ef4444", "Röd");          // 7+ timmar

        private final String code;
        private final String colorHex;
        private final String swedishName;

        LoadLevel(String code, String colorHex, String swedishName) {
            this.code = code;
            this.colorHex = colorHex;
            this.swedishName = swedishName;
        }

        public String getCode() {
            return code;
        }

        public String getColorHex() {
            return colorHex;
        }

        public String getSwedishName() {
            return swedishName;
        }
    }

    public static class TimeSlot {
        private final int mechanicId;
        private final LocalDate date;
        private final int hour; // 7 till 15
        private int bookingId;
        private int workOrderId;
        private String customerName;
        private String vehicleReg;
        private String description;
        private boolean booked;

        public TimeSlot(int mechanicId, LocalDate date, int hour) {
            this.mechanicId = mechanicId;
            this.date = date;
            this.hour = hour;
            this.booked = false;
        }

        public int getMechanicId() {
            return mechanicId;
        }

        public LocalDate getDate() {
            return date;
        }

        public int getHour() {
            return hour;
        }

        public String getTimeRange() {
            return String.format("%02d:00 - %02d:00", hour, hour + 1);
        }

        public boolean isBooked() {
            return booked;
        }

        public void setBooked(boolean booked) {
            this.booked = booked;
        }

        public int getBookingId() {
            return bookingId;
        }

        public void setBookingId(int bookingId) {
            this.bookingId = bookingId;
        }

        public int getWorkOrderId() {
            return workOrderId;
        }

        public void setWorkOrderId(int workOrderId) {
            this.workOrderId = workOrderId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getVehicleReg() {
            return vehicleReg;
        }

        public void setVehicleReg(String vehicleReg) {
            this.vehicleReg = vehicleReg;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class DayLoad {
        private final LocalDate date;
        private final int mechanicId;
        private final int bookedHours;
        private final int totalHours;
        private final LoadLevel level;
        private final List<TimeSlot> slots;

        public DayLoad(LocalDate date, int mechanicId, int bookedHours, int totalHours, LoadLevel level, List<TimeSlot> slots) {
            this.date = date;
            this.mechanicId = mechanicId;
            this.bookedHours = bookedHours;
            this.totalHours = totalHours;
            this.level = level;
            this.slots = slots;
        }

        public LocalDate getDate() {
            return date;
        }

        public int getMechanicId() {
            return mechanicId;
        }

        public int getBookedHours() {
            return bookedHours;
        }

        public int getTotalHours() {
            return totalHours;
        }

        public double getLoadPercentage() {
            return totalHours == 0 ? 0 : (double) bookedHours / totalHours;
        }

        public LoadLevel getLevel() {
            return level;
        }

        public List<TimeSlot> getSlots() {
            return slots;
        }
    }

    public static class MonthDayStatus {
        private final LocalDate date;
        private final boolean inCurrentMonth;
        private final boolean isWeekend;
        private final boolean isMechanicAvailable;
        private final boolean isFullyBooked;
        private final int bookedHours;
        private final LoadLevel level;

        public MonthDayStatus(LocalDate date, boolean inCurrentMonth, boolean isWeekend,
                              boolean isMechanicAvailable, boolean isFullyBooked, int bookedHours, LoadLevel level) {
            this.date = date;
            this.inCurrentMonth = inCurrentMonth;
            this.isWeekend = isWeekend;
            this.isMechanicAvailable = isMechanicAvailable;
            this.isFullyBooked = isFullyBooked;
            this.bookedHours = bookedHours;
            this.level = level != null ? level : LoadLevel.FREE;
        }

        public MonthDayStatus(LocalDate date, boolean inCurrentMonth, boolean isWeekend,
                              boolean isMechanicAvailable, boolean isFullyBooked, int bookedHours) {
            this(date, inCurrentMonth, isWeekend, isMechanicAvailable, isFullyBooked, bookedHours, LoadLevel.FREE);
        }

        public LocalDate getDate() {
            return date;
        }

        public LoadLevel getLevel() {
            return level;
        }

        public boolean isInCurrentMonth() {
            return inCurrentMonth;
        }

        public boolean isWeekend() {
            return isWeekend;
        }

        public boolean isAvailableForBooking() {
            return inCurrentMonth && !isWeekend && isMechanicAvailable && !isFullyBooked;
        }

        public boolean isFullyBooked() {
            return isFullyBooked;
        }

        public boolean isMechanicAvailable() {
            return isMechanicAvailable;
        }

        public int getBookedHours() {
            return bookedHours;
        }
    }

    // Singleton instance för delat tillstånd i UI
    private static final MechanicSchedule INSTANCE = new MechanicSchedule();

    public static MechanicSchedule getInstance() {
        return INSTANCE;
    }

    // Nyckel: "mechanicId:YYYY-MM-DD:hour"
    private final Map<String, TimeSlot> slots = new HashMap<String, TimeSlot>();
    private boolean seeded = false;

    public MechanicSchedule() {
        initDefaultSeedData();
    }

    private String slotKey(int mechanicId, LocalDate date, int hour) {
        return mechanicId + ":" + date.toString() + ":" + hour;
    }

    /**
     * Initialiserar realistisk testdata för innevarande vecka och dag.
     */
    public synchronized void initDefaultSeedData() {
        if (seeded) return;
        seeded = true;

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Mekaniker 1 (Johan Karlsson): Idag 08-10 (Service) och 13-14 (Inspektion) -> 3h bokade (Gul)
        bookSlotInternal(1, today, 8, 1, 1, "Anna Andersson", "ABC123", "Oljeservice & filterbyte");
        bookSlotInternal(1, today, 9, 1, 1, "Anna Andersson", "ABC123", "Fortsättning oljeservice");
        bookSlotInternal(1, today, 13, 2, 2, "Erik Eriksson", "DEF456", "Bromskontroll fram");

        // Mekaniker 1: Imorgon 08-12 och 13-16 -> 7h bokade (Röd / Full)
        for (int h = 8; h <= 11; h++) {
            bookSlotInternal(1, tomorrow, h, 3, 5, "Maria Svensson", "GHI789", "Helrenovering bromsar");
        }
        for (int h = 13; h <= 15; h++) {
            bookSlotInternal(1, tomorrow, h, 3, 5, "Maria Svensson", "GHI789", "Helrenovering bromsar");
        }

        // Mekaniker 1: Dagen efter imorgon -> 1h bokad (Grön)
        bookSlotInternal(1, today.plusDays(2), 10, 1, 1, "Anna Andersson", "ABC123", "Efterkontroll olja");

        // Mekaniker 2 (Sara Nilsson): Idag 09-12 och 14-16 -> 5h bokade (Orange)
        bookSlotInternal(2, today, 9, 4, 3, "Olof Palme", "XYZ999", "Bromsok och belägg");
        bookSlotInternal(2, today, 10, 4, 3, "Olof Palme", "XYZ999", "Bromsok och belägg");
        bookSlotInternal(2, today, 11, 4, 3, "Olof Palme", "XYZ999", "Luftning bromssystem");
        bookSlotInternal(2, today, 14, 4, 3, "Olof Palme", "XYZ999", "Kontroll bromsslangar");
        bookSlotInternal(2, today, 15, 4, 3, "Olof Palme", "XYZ999", "Slutbesiktning bromsar");

        // Mekaniker 2: Igår 08-09 (Grön)
        bookSlotInternal(2, today.minusDays(1), 8, 4, 3, "Olof Palme", "XYZ999", "Snabbkoll bromsar");

        // Mekaniker 3 (Mikael Berg): Idag 07-08 -> 1h (Grön)
        bookSlotInternal(3, today, 7, 5, 4, "Sven Melander", "AAA001", "Felkodsläsning OBD2");
        // Mekaniker 3: Imorgon 10-15 -> 5h (Orange)
        for (int h = 10; h <= 14; h++) {
            bookSlotInternal(3, tomorrow, h, 6, 6, "Gustav Vasa", "BBB002", "Elektronikfelsökning");
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

    /**
     * Hämtar alla tidsslottar för en specifik mekaniker och dag (07:00 till 16:00).
     */
    public synchronized List<TimeSlot> getSlotsForDay(int mechanicId, LocalDate date) {
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

    /**
     * Boka en specifik timme för en mekaniker med valfritt workOrderId.
     */
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

    /**
     * Boka en specifik timme för en mekaniker.
     */
    public synchronized boolean bookSlot(int mechanicId, LocalDate date, int hour, int bookingId,
                                         String customer, String vehicleReg, String desc) {
        return bookSlot(mechanicId, date, hour, bookingId, 0, customer, vehicleReg, desc);
    }

    /**
     * Avboka en tidsslott.
     */
    public synchronized boolean cancelSlot(int mechanicId, LocalDate date, int hour) {
        String key = slotKey(mechanicId, date, hour);
        TimeSlot slot = slots.remove(key);
        return slot != null;
    }

    /**
     * Beräknar belastningsgrad för en dag (0-9 timmar).
     */
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

    /**
     * Hämtar dagsbelastning för en hel vecka (Måndag till Söndag).
     */
    public synchronized List<DayLoad> getWeekLoads(int mechanicId, LocalDate weekStartDate) {
        LocalDate monday = weekStartDate.with(DayOfWeek.MONDAY);
        List<DayLoad> result = new ArrayList<DayLoad>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            result.add(getDayLoad(mechanicId, day));
        }
        return result;
    }

    /**
     * Hämtar månadsdagar och deras tillgänglighetsstatus för en hel månad.
     */
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
            boolean isFullyBooked = booked >= WORK_HOURS_PER_DAY;

            result.add(new MonthDayStatus(cur, inCurrentMonth, isWeekend, mechanicAvailableFlag, isFullyBooked, booked, level));
            cur = cur.plusDays(1);
        }

        return result;
    }

    /**
     * Återställer alla tidsbokningar (för enhetstester).
     */
    public synchronized void resetForTest() {
        slots.clear();
        seeded = false;
    }
}
