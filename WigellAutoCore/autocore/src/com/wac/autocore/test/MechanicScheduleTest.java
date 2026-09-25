package com.wac.autocore.test;

import com.wac.autocore.service.MechanicSchedule;
import com.wac.autocore.service.MechanicSchedule.DayLoad;
import com.wac.autocore.service.MechanicSchedule.LoadLevel;
import com.wac.autocore.service.MechanicSchedule.MonthDayStatus;
import com.wac.autocore.service.MechanicSchedule.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Enhetstester för MechanicSchedule:
 * - Schemaläggning och tidsslottar (07:00-16:00)
 * - Beläggningsberäkning och färgskala (grön -> gul -> orange -> röd)
 * - Veckoöversikt
 * - Månadstillgänglighet
 * - Dubbelbokningsskydd
 */
public class MechanicScheduleTest {

    public void testDaySlotsCountAndHours() {
        MechanicSchedule schedule = new MechanicSchedule();
        schedule.resetForTest();

        LocalDate today = LocalDate.of(2026, 9, 22);
        List<TimeSlot> slots = schedule.getSlotsForDay(1, today);

        TestRunner.assertEquals(9, slots.size(), "Dagsvy ska innehålla 9 timmar (7-16)");
        TestRunner.assertEquals(7, slots.get(0).getHour(), "Första timmen ska vara 07");
        TestRunner.assertEquals(15, slots.get(slots.size() - 1).getHour(), "Sista timmen ska vara 15 (15:00-16:00)");
        TestRunner.assertEquals("07:00 - 08:00", slots.get(0).getTimeRange(), "Tidsintervall-formatering");
    }

    public void testBookingAndDoubleBookingPrevention() {
        MechanicSchedule schedule = new MechanicSchedule();
        schedule.resetForTest();

        LocalDate date = LocalDate.of(2026, 9, 23);
        boolean bookedFirst = schedule.bookSlot(1, date, 9, 101, "Anna Andersson", "ABC123", "Service");
        TestRunner.assertTrue(bookedFirst, "Bokning av ledig tid ska lyckas");

        boolean bookedAgain = schedule.bookSlot(1, date, 9, 102, "Bengt Berg", "XYZ789", "Annan service");
        TestRunner.assertFalse(bookedAgain, "Dubbelbokning på samma mekaniker och timme ska nekas");

        // Kontrollera att slottens data är korrekt
        List<TimeSlot> slots = schedule.getSlotsForDay(1, date);
        TimeSlot slot9 = null;
        for (TimeSlot s : slots) {
            if (s.getHour() == 9) slot9 = s;
        }
        TestRunner.assertNotNull(slot9, "Slott 9 ska finnas");
        TestRunner.assertTrue(slot9.isBooked(), "Slott 9 ska vara markerad som bokad");
        TestRunner.assertEquals("Anna Andersson", slot9.getCustomerName(), "Kundnamn ska stämma");
        TestRunner.assertEquals("ABC123", slot9.getVehicleReg(), "Regnr ska stämma");
    }

    public void testWorkloadColorProgression() {
        MechanicSchedule schedule = new MechanicSchedule();
        schedule.resetForTest();

        LocalDate date = LocalDate.of(2026, 9, 24);
        int mechId = 1;

        // 0 bokade timmar -> FREE (Grön)
        DayLoad load0 = schedule.getDayLoad(mechId, date);
        TestRunner.assertEquals(LoadLevel.FREE, load0.getLevel(), "0 bokade ska vara FREE (Grön)");
        TestRunner.assertEquals(0, load0.getBookedHours(), "0 bokade timmar");

        // 2 bokade timmar -> FREE (Grön)
        schedule.bookSlot(mechId, date, 8, 1, "C1", "R1", "D1");
        schedule.bookSlot(mechId, date, 9, 2, "C2", "R2", "D2");
        DayLoad load2 = schedule.getDayLoad(mechId, date);
        TestRunner.assertEquals(LoadLevel.FREE, load2.getLevel(), "2 bokade ska vara FREE (Grön)");

        // 3-4 bokade timmar -> MODERATE (Gul)
        schedule.bookSlot(mechId, date, 10, 3, "C3", "R3", "D3");
        DayLoad load3 = schedule.getDayLoad(mechId, date);
        TestRunner.assertEquals(LoadLevel.MODERATE, load3.getLevel(), "3 bokade ska vara MODERATE (Gul)");

        schedule.bookSlot(mechId, date, 11, 4, "C4", "R4", "D4");
        DayLoad load4 = schedule.getDayLoad(mechId, date);
        TestRunner.assertEquals(LoadLevel.MODERATE, load4.getLevel(), "4 bokade ska vara MODERATE (Gul)");

        // 5-6 bokade timmar -> BUSY (Orange)
        schedule.bookSlot(mechId, date, 12, 5, "C5", "R5", "D5");
        DayLoad load5 = schedule.getDayLoad(mechId, date);
        TestRunner.assertEquals(LoadLevel.BUSY, load5.getLevel(), "5 bokade ska vara BUSY (Orange)");

        // 7+ bokade timmar -> FULL (Röd)
        schedule.bookSlot(mechId, date, 13, 6, "C6", "R6", "D6");
        schedule.bookSlot(mechId, date, 14, 7, "C7", "R7", "D7");
        DayLoad load7 = schedule.getDayLoad(mechId, date);
        TestRunner.assertEquals(LoadLevel.FULL, load7.getLevel(), "7 bokade ska vara FULL (Röd)");
    }

    public void testWeekLoads() {
        MechanicSchedule schedule = new MechanicSchedule();
        schedule.resetForTest();

        LocalDate wednesday = LocalDate.of(2026, 9, 23); // En onsdag
        List<DayLoad> week = schedule.getWeekLoads(1, wednesday);

        TestRunner.assertEquals(7, week.size(), "En vecka ska ha 7 dagar");
        TestRunner.assertEquals(DayOfWeek.MONDAY, week.get(0).getDate().getDayOfWeek(), "Första dagen ska vara måndag");
        TestRunner.assertEquals(DayOfWeek.SUNDAY, week.get(6).getDate().getDayOfWeek(), "Sista dagen ska vara söndag");
    }

    public void testMonthAvailability() {
        MechanicSchedule schedule = new MechanicSchedule();
        schedule.resetForTest();

        YearMonth ym = YearMonth.of(2026, 9);
        List<MonthDayStatus> days = schedule.getMonthDays(1, ym, true);

        TestRunner.assertTrue(days.size() >= 28, "Månadsgitter ska ha minst 28 dagar");

        // Vardag utan bokningar ska vara tillgänglig
        LocalDate weekdayInMonth = LocalDate.of(2026, 9, 15); // Tisdag
        MonthDayStatus status = null;
        for (MonthDayStatus s : days) {
            if (s.getDate().equals(weekdayInMonth)) status = s;
        }
        TestRunner.assertNotNull(status, "Dag 15 ska hittas");
        TestRunner.assertTrue(status.isAvailableForBooking(), "Vardag utan bokningar och med tillgänglig mekaniker ska vara öppen");

        // Helgdag ska inte vara tillgänglig för bokning
        LocalDate weekendDay = LocalDate.of(2026, 9, 19); // Lördag
        MonthDayStatus weekendStatus = null;
        for (MonthDayStatus s : days) {
            if (s.getDate().equals(weekendDay)) weekendStatus = s;
        }
        TestRunner.assertNotNull(weekendStatus, "Lördag ska hittas");
        TestRunner.assertFalse(weekendStatus.isAvailableForBooking(), "Helgdagar ska inte vara bokningsbara");

        // Om mekanikern är flagged som otillgänglig
        List<MonthDayStatus> unavailableDays = schedule.getMonthDays(1, ym, false);
        MonthDayStatus unavailStatus = null;
        for (MonthDayStatus s : unavailableDays) {
            if (s.getDate().equals(weekdayInMonth)) unavailStatus = s;
        }
        TestRunner.assertNotNull(unavailStatus, "Dag 15 ska hittas i otillgänglig lista");
        TestRunner.assertFalse(unavailStatus.isAvailableForBooking(), "Mekaniker som är ej tillgänglig ska inte ha öppna dagar");
    }

    public void testGetNextBookingDate() {
        MechanicSchedule schedule = new MechanicSchedule();
        schedule.resetForTest();

        LocalDate d1 = LocalDate.of(2026, 9, 23);
        LocalDate d2 = LocalDate.of(2026, 9, 25);

        // Ingen bokning ännu
        TestRunner.assertTrue(schedule.getNextBookingDate(1, d1) == null, "Ingen framtida bokning ska ge null");

        // Boka 25:e
        schedule.bookSlot(1, d2, 10, 999, "Test Person", "TST123", "Service");

        LocalDate next = schedule.getNextBookingDate(1, d1);
        TestRunner.assertNotNull(next, "Ska hitta nästa bokade dag");
        TestRunner.assertEquals(d2, next, "Nästa bokade dag ska vara 2026-09-25");
    }

    public void testBookingSlotAvailabilityCheck() {
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        LocalDate testDate = LocalDate.of(2026, 10, 15);
        int mechId = 1;

        boolean booked = schedule.bookSlot(mechId, testDate, 10, 888, "Test Kund", "TST999", "Oljebyte");
        TestRunner.assertTrue(booked, "Bokning av kl 10 ska lyckas");

        com.wac.autocore.model.Mechanic mech = new com.wac.autocore.model.Mechanic(mechId, "Test Mekaniker", "070-0000000", "Allmän");
        boolean is10Booked = com.wac.autocore.ui.BookingDialogs.isHourBooked(null, mech, testDate, 10, 0);
        TestRunner.assertTrue(is10Booked, "Kl 10 ska detekteras som upptagen");

        boolean is11Booked = com.wac.autocore.ui.BookingDialogs.isHourBooked(null, mech, testDate, 11, 0);
        TestRunner.assertFalse(is11Booked, "Kl 11 ska detekteras som ledig");

        boolean is10SelfExcluded = com.wac.autocore.ui.BookingDialogs.isHourBooked(null, mech, testDate, 10, 888);
        TestRunner.assertFalse(is10SelfExcluded, "Egen bokning ska inte blockera tiden vid redigering");

        schedule.cancelSlot(mechId, testDate, 10);
    }

    public void testCancelSlotForBooking() {
        MechanicSchedule schedule = MechanicSchedule.getInstance();
        LocalDate testDate = LocalDate.now().plusDays(25);
        int mechId = 1;
        int testBookingId = 777;

        boolean booked = schedule.bookSlot(mechId, testDate, 14, testBookingId, "Kund Namn", "XYZ123", "Service");
        TestRunner.assertTrue(booked, "Slot ska bokas för bokningsId 777");

        com.wac.autocore.model.Mechanic mech = new com.wac.autocore.model.Mechanic(mechId, "Test Mekaniker", "070-0000000", "Allmän");
        TestRunner.assertTrue(com.wac.autocore.ui.BookingDialogs.isHourBooked(null, mech, testDate, 14, 0),
                "Kl 14 ska vara upptagen före avbokning");

        boolean removed = schedule.cancelSlotForBooking(testBookingId);
        TestRunner.assertTrue(removed, "cancelSlotForBooking ska returnera true när slot togs bort");

        TestRunner.assertFalse(com.wac.autocore.ui.BookingDialogs.isHourBooked(null, mech, testDate, 14, 0),
                "Kl 14 ska vara ledig efter att bokningen avbokats");
    }
}
