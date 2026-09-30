package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;

public class WorkOrder {

    private int id;
    private int bookingId;
    private int mechanicId;
    private List<Integer> serviceItemIds;
    private String status;
    private final List<Integer> completedServiceItems = new ArrayList<>();

    public WorkOrder(int id, int bookingId, int mechanicId) {
        this.id = id;
        this.bookingId = bookingId;
        this.mechanicId = mechanicId;
        this.serviceItemIds = new ArrayList<Integer>();
        this.status = "CREATED";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    public List<Integer> getServiceItemIds() {
        return serviceItemIds;
    }

    public void setServiceItemIds(List<Integer> serviceItemIds) {
        this.serviceItemIds = serviceItemIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addServiceItem(int serviceItemId) {
        serviceItemIds.add(serviceItemId);
    }

    public void removeServiceItem(int serviceItemId) {
        serviceItemIds.remove(Integer.valueOf(serviceItemId));
    }

    public List<Integer> getCompletedServiceItems() {return completedServiceItems; }

    public void markServiceAsCompleted(int serviceItemId) {
        if (!this.serviceItemIds.contains(serviceItemId)) {
            throw new IllegalArgumentException("Service item " + serviceItemId + " does not belong to this work order.");
        }
        if (!this.completedServiceItems.contains(serviceItemId)) {
            this.completedServiceItems.add(serviceItemId);
        }
    }

    public void addServiceItemId(int serviceItemId) {
        if (!this.serviceItemIds.contains(serviceItemId)) {
            this.serviceItemIds.add(serviceItemId);
        }
    }

    @Override
    public String toString() {
        return id +
                " - Booking ID: " + bookingId +
                " | Mechanic ID: " + mechanicId +
                " | Services: " + serviceItemIds +
                " | Status: " + status;
    }
}