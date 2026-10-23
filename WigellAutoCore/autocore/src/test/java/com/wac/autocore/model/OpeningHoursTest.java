package com.wac.autocore.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Which hours of a day have gone by.
 *
 * The Kanban day view used to offer every unbooked hour of today with a plus, also the ones that
 * had already passed. After closing the day looked free while a drop-in answered that no time was
 * left, which is two answers to the same question.
 *
 * The rule here is the one a drop-in already follows: a job may start now, never earlier. These
 * tests pin it to the clock the caller passes in, so the answer is the same at nine in the morning
 * as at midnight.
 */
class OpeningHoursTest {

    private static final LocalDate A_WEDNESDAY = LocalDate.of(2026, 10, 21);

    @Test
    void theHoursBeforeTheCurrentOneHavePassed() {
        assertTrue(OpeningHours.hasPassed(A_WEDNESDAY, 7, A_WEDNESDAY, LocalTime.of(9, 30)));
        assertTrue(OpeningHours.hasPassed(A_WEDNESDAY, 8, A_WEDNESDAY, LocalTime.of(9, 30)));
    }

    @Test
    void theCurrentHourIsStillBookable() {
        assertFalse(OpeningHours.hasPassed(A_WEDNESDAY, 9, A_WEDNESDAY, LocalTime.of(9, 30)));
        assertFalse(OpeningHours.hasPassed(A_WEDNESDAY, 16, A_WEDNESDAY, LocalTime.of(9, 30)));
    }

    @Test
    void afterClosingTheWholeDayHasPassed() {
        LocalTime afterClosing = LocalTime.of(20, 15);
        for (int hour = OpeningHours.firstHour(); hour < OpeningHours.closingHour(); hour++) {
            assertTrue(OpeningHours.hasPassed(A_WEDNESDAY, hour, A_WEDNESDAY, afterClosing),
                    hour + ":00 ska vara passerad efter stängning");
        }
    }

    @Test
    void anotherDayIsNotJudgedByTodaysClock() {
        LocalDate tomorrow = A_WEDNESDAY.plusDays(1);
        LocalDate yesterday = A_WEDNESDAY.minusDays(1);

        assertFalse(OpeningHours.hasPassed(tomorrow, 7, A_WEDNESDAY, LocalTime.of(23, 0)));
        assertTrue(OpeningHours.hasPassed(yesterday, 16, A_WEDNESDAY, LocalTime.of(1, 0)));
    }

    /**
     * The point of the card: the day view and a drop-in must answer the same about which hours are
     * left. A drop-in may start from {@code max(now, opening)} and not earlier, so every hour in
     * the day is bookable exactly when it is not in the past.
     */
    @Test
    void theDayViewAndADropInAgreeAboutTheHoursLeft() {
        for (LocalTime now : new LocalTime[] {
                LocalTime.of(5, 0), LocalTime.of(7, 0), LocalTime.of(9, 30),
                LocalTime.of(16, 59), LocalTime.of(20, 15)}) {

            int dropInMayStart = Math.max(now.getHour(), OpeningHours.firstHour());
            for (int hour = OpeningHours.firstHour(); hour < OpeningHours.closingHour(); hour++) {
                assertEquals(hour < dropInMayStart,
                        OpeningHours.hasPassed(A_WEDNESDAY, hour, A_WEDNESDAY, now),
                        "klockan " + now + ", timme " + hour);
            }
        }
    }
}
