package com.wac.autocore.test;

import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.BookingAvailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * BOKNINGSBARA TIDER: ett jobb måste rymmas inom öppettiderna, både när dagen väljs och när
 * starttiden väljs. Utan det går det att boka in 315 minuters arbete klockan 16:00 på en verkstad
 * som stänger 17:00, och tiden räcker inte.
 */
public class BookingAvailabilityTest {

    public void testJobThatDoesNotFinishBeforeClosingIsRejected() {
        GarageSystem garage = new GarageSystem();
        LocalDate day = nextWeekday();

        TestRunner.assertFalse(BookingAvailability.isSlotAvailable(garage, null, day, LocalTime.of(16, 0), 315, 0),
                "315 minuter som börjar 16:00 slutar 21:15 — det är ingen ledig tid på en verkstad som stänger 17:00");
        TestRunner.assertTrue(BookingAvailability.isSlotAvailable(garage, null, day, LocalTime.of(11, 0), 315, 0),
                "315 minuter som börjar 11:00 slutar 16:15 och ryms före stängning");
    }

    public void testDayWithoutRoomForTheWholeJobIsNotOffered() {
        GarageSystem garage = new GarageSystem();
        LocalDate day = nextWeekday();

        TestRunner.assertTrue(BookingAvailability.hasAvailableSlotOnDate(garage, null, null, day, 315, 0),
                "En arbetsdag 07:00-17:00 rymmer ett jobb på 315 minuter");
        TestRunner.assertFalse(BookingAvailability.hasAvailableSlotOnDate(garage, null, null, day, 601, 0),
                "Ett jobb längre än hela arbetsdagen ska inte erbjudas alls");
    }

    /** Nästa vardag, så provet inte faller på att helgen är stängd. */
    private LocalDate nextWeekday() {
        LocalDate day = LocalDate.now().plusDays(1);
        while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.plusDays(1);
        }
        return day;
    }
}
