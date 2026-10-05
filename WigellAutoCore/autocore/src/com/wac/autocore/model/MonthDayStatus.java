package com.wac.autocore.model;

import java.time.LocalDate;

public class MonthDayStatus {
    private final LocalDate date;
    private final boolean inCurrentMonth;
    private final boolean isWeekend;
    private final boolean isMechanicAvailable;
    private final int bookedHours;
    private final LoadLevel level;

    public MonthDayStatus(LocalDate date, boolean inCurrentMonth, boolean isWeekend,
                  boolean isMechanicAvailable, int bookedHours, LoadLevel level) {
        this.date = date;
        this.inCurrentMonth = inCurrentMonth;
        this.isWeekend = isWeekend;
        this.isMechanicAvailable = isMechanicAvailable;
        this.bookedHours = bookedHours;
        this.level = level != null ? level : LoadLevel.FREE;
    }

    public MonthDayStatus(LocalDate date, boolean inCurrentMonth, boolean isWeekend,
                          boolean isMechanicAvailable, int bookedHours) {
        this(date, inCurrentMonth, isWeekend, isMechanicAvailable, bookedHours, LoadLevel.FREE);
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


    public boolean isMechanicAvailable() {
        return isMechanicAvailable;
    }

    public int getBookedHours() {
        return bookedHours;
    }
}
