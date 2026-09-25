package com.wac.autocore.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Booking {

    private int id;
    private int vehicleId;
    private LocalDate date;
    private String description;
    private String status;
    private LocalTime startTime;
    private LocalTime endTime;
    private int mechanicId;
    private int serviceItemId;

    public Booking(int vehicleId, LocalDate date, String description) {
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
    }
    public Booking(int id, int vehicleId, LocalDate date, String description) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
    }
    public Booking(int vehicleId, LocalDate date, String description,
                   LocalTime startTime, LocalTime endTime, int mechanicId, int serviceItemId) {
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "CREATED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        this.serviceItemId = serviceItemId;
    }

    public int getId() {
        return id;
    }
@SuppressWarnings("do not use")
    public void setId(int id) {
        this.id = id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getMechanicId() { return mechanicId; }

    public void setMechanicId(int mechanicId) { this.mechanicId = mechanicId; }

    public int getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(int serviceItemId) { this.serviceItemId = serviceItemId; }

    @Override
    public String toString() {
        return id + " - Vehicle ID: " + vehicleId +
                " | Date: " + date +
                " | Description: " + description +
                " | Status: " + status;
    }
}