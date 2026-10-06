package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkOrder {

    private int id;
    private int bookingId;
    private int mechanicId;
    private List<Integer> serviceItemIds;
    private final List<Integer> completedServiceItems = new ArrayList<Integer>();
    private final Map<Integer, Double> completedServicePrices = new LinkedHashMap<Integer, Double>();
    private String status;
    private int vehicleId;
    private String description;

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

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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


    public List<Integer> getCompletedServiceItems() {
        return completedServiceItems;
    }

    public void setCompletedServiceItems(List<Integer> completed) {
        this.completedServiceItems.clear();
        if (completed != null) {
            this.completedServiceItems.addAll(completed);
        }
    }

    /** Returnerar priset som gällde när tjänsten utfördes. */
    public Double getCompletedServicePrice(int serviceItemId) {
        return completedServicePrices.get(Integer.valueOf(serviceItemId));
    }

    public Map<Integer, Double> getCompletedServicePrices() {
        return completedServicePrices;
    }

    public void setCompletedServicePrices(Map<Integer, Double> prices) {
        completedServicePrices.clear();
        if (prices != null) {
            completedServicePrices.putAll(prices);
        }
    }

    public void markServiceAsCompleted(int serviceItemId) {
        if (!this.serviceItemIds.contains(serviceItemId)) {
            throw new IllegalArgumentException("Tjänsten med ID " + serviceItemId + " tillhör inte denna arbetsorder.");
        }
        if (!this.completedServiceItems.contains(serviceItemId)) {
            this.completedServiceItems.add(serviceItemId);
        }
    }

    /** Markerar tjänsten som utförd och sparar priset som gällde då. */
    public void markServiceAsCompleted(int serviceItemId, double frozenPrice) {
        markServiceAsCompleted(serviceItemId);
        completedServicePrices.put(Integer.valueOf(serviceItemId), Double.valueOf(frozenPrice));
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