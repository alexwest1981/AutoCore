package com.wac.autocore.model;

import java.time.LocalDate;
import java.util.List;

public class DayLoad {
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

    public LoadLevel getLevel() {
        return level;
    }

    public List<TimeSlot> getSlots() {
        return slots;
    }
}
