package com.wac.autocore.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** The workshop's opening hours. The only place they are written down. */
public final class OpeningHours {

    /** The workshop opens. */
    public static final LocalTime OPENS = LocalTime.of(7, 0);

    /** The workshop closes. A job has to be finished by then. */
    public static final LocalTime CLOSES = LocalTime.of(17, 0);

    private OpeningHours() {}

    /** The first hour a job can start. */
    public static int firstHour() {
        return OPENS.getHour();
    }

    /** The hour the day ends, the first one that cannot be booked. */
    public static int closingHour() {
        return CLOSES.getHour();
    }

    /** How many minutes the working day has. */
    public static int minutesPerDay() {
        return (int) Duration.between(OPENS, CLOSES).toMinutes();
    }

    /** True if a job starting at that time is finished by closing. */
    public static boolean endsBeforeClosing(LocalTime start, int minutes) {
        return !start.plusMinutes(minutes).isAfter(CLOSES);
    }

    /** True on weekdays. The workshop is closed on Saturdays and Sundays. */
    public static boolean isOpenOn(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /**
     * True when the hour on that day has already begun and gone by. The hour we are in is still
     * bookable, which is the same rule a drop-in follows: it may start now, never earlier. A day
     * that has passed is passed whole; a day to come has none.
     */
    public static boolean hasPassed(LocalDate day, int hour, LocalDate today, LocalTime now) {
        if (day == null || today == null || now == null) {
            return false;
        }
        if (day.isBefore(today)) {
            return true;
        }
        if (day.isAfter(today)) {
            return false;
        }
        return hour < now.getHour();
    }

    /** The same question, asked about the clock in this machine. */
    public static boolean hasPassed(LocalDate day, int hour) {
        return hasPassed(day, hour, LocalDate.now(), LocalTime.now());
    }
}