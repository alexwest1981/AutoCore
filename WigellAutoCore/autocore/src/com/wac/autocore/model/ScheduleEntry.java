package com.wac.autocore.model;

import java.time.LocalDate;

/** A work order the way the schedule needs it: who works, when, and for whom. */
public class ScheduleEntry {

    private final int workOrderId;
    private final int mechanicId;
    private final int bookingId;
    private final LocalDate date;
    private final String startTime;
    private final String description;
    private final String registrationNumber;
    private final String customerName;

    public ScheduleEntry(int workOrderId, int mechanicId, int bookingId, LocalDate date,
                         String startTime, String description,
                         String registrationNumber, String customerName) {
        this.workOrderId = workOrderId;
        this.mechanicId = mechanicId;
        this.bookingId = bookingId;
        this.date = date;
        this.startTime = startTime;
        this.description = description;
        this.registrationNumber = registrationNumber;
        this.customerName = customerName;
    }

    public int getWorkOrderId() {
        return workOrderId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getDescription() {
        return description;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getCustomerName() {
        return customerName;
    }
}
