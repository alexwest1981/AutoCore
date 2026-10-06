package com.wac.autocore.model;

import java.time.LocalDate;

public class TimeSlot {
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
