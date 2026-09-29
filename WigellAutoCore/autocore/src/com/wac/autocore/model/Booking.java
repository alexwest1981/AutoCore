package com.wac.autocore.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
    private final List<ServiceItem> serviceItems = new ArrayList<ServiceItem>();
    private final List<Integer> serviceItemIds = new ArrayList<Integer>();

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
        this.status = "BOOKED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        setServiceItemId(serviceItemId);
    }

    public Booking(int id, int vehicleId, LocalDate date, String description,
                   LocalTime startTime, LocalTime endTime, int mechanicId, int serviceItemId) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        setServiceItemId(serviceItemId);
    }

    public Booking(int vehicleId, LocalDate date, String description,
                   LocalTime startTime, LocalTime endTime, int mechanicId, List<ServiceItem> serviceItems) {
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        setServiceItems(serviceItems);
    }

    public Booking(int id, int vehicleId, LocalDate date, String description,
                   LocalTime startTime, LocalTime endTime, int mechanicId, List<ServiceItem> serviceItems) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        setServiceItems(serviceItems);
    }

    public Booking(int vehicleId, LocalDate date, String description,
                   LocalTime startTime, LocalTime endTime, int mechanicId, ServiceItem serviceItem) {
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        if (serviceItem != null) {
            addServiceItem(serviceItem);
        }
    }

    public Booking(int id, int vehicleId, LocalDate date, String description,
                   LocalTime startTime, LocalTime endTime, int mechanicId, ServiceItem serviceItem) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
        this.startTime = startTime;
        this.endTime = endTime;
        this.mechanicId = mechanicId;
        if (serviceItem != null) {
            addServiceItem(serviceItem);
        }
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

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    public boolean isWorkStarted() {
        return "IN_PROGRESS".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean canModifyServices() {
        return !isWorkStarted();
    }

    public List<ServiceItem> getServiceItems() {
        return new ArrayList<ServiceItem>(serviceItems);
    }

    public void setLoadedServiceItems(List<ServiceItem> items) {
        this.serviceItems.clear();
        this.serviceItemIds.clear();
        if (items != null) {
            for (ServiceItem item : items) {
                if (item != null) {
                    this.serviceItems.add(item);
                    this.serviceItemIds.add(item.getId());
                }
            }
        }
        if (!this.serviceItemIds.isEmpty()) {
            this.serviceItemId = this.serviceItemIds.get(0);
        } else {
            this.serviceItemId = 0;
        }
    }

    public boolean setServiceItems(List<ServiceItem> items) {
        if (!canModifyServices()) {
            return false;
        }
        setLoadedServiceItems(items);
        return true;
    }

    public boolean addServiceItem(ServiceItem item) {
        if (!canModifyServices()) {
            return false;
        }
        if (item == null) return false;
        this.serviceItems.add(item);
        if (!this.serviceItemIds.contains(item.getId())) {
            this.serviceItemIds.add(item.getId());
        }
        if (this.serviceItemId <= 0) {
            this.serviceItemId = item.getId();
        }
        return true;
    }

    public boolean removeServiceItem(ServiceItem item) {
        if (!canModifyServices()) {
            return false;
        }
        if (item == null) return false;
        boolean removed = this.serviceItems.remove(item);
        this.serviceItemIds.remove(Integer.valueOf(item.getId()));
        if (this.serviceItemId == item.getId()) {
            this.serviceItemId = this.serviceItemIds.isEmpty() ? 0 : this.serviceItemIds.get(0);
        }
        return removed;
    }

    public boolean removeServiceItemById(int serviceId) {
        if (!canModifyServices()) {
            return false;
        }
        boolean removedId = this.serviceItemIds.remove(Integer.valueOf(serviceId));
        ServiceItem toRemove = null;
        for (ServiceItem s : this.serviceItems) {
            if (s.getId() == serviceId) {
                toRemove = s;
                break;
            }
        }
        if (toRemove != null) {
            this.serviceItems.remove(toRemove);
        }
        if (this.serviceItemId == serviceId) {
            this.serviceItemId = this.serviceItemIds.isEmpty() ? 0 : this.serviceItemIds.get(0);
        }
        return removedId || toRemove != null;
    }

    public List<Integer> getServiceItemIds() {
        if (!serviceItemIds.isEmpty()) {
            return new ArrayList<Integer>(serviceItemIds);
        }
        List<Integer> ids = new ArrayList<Integer>();
        if (serviceItemId > 0) {
            ids.add(serviceItemId);
        }
        return ids;
    }

    public void setServiceItemIds(List<Integer> ids) {
        this.serviceItemIds.clear();
        if (ids != null) {
            this.serviceItemIds.addAll(ids);
        }
        if (!this.serviceItemIds.isEmpty()) {
            this.serviceItemId = this.serviceItemIds.get(0);
        } else {
            this.serviceItemId = 0;
        }
    }

    public int getServiceItemId() {
        if (serviceItemId > 0) {
            return serviceItemId;
        }
        if (!serviceItemIds.isEmpty()) {
            return serviceItemIds.get(0);
        }
        if (!serviceItems.isEmpty()) {
            return serviceItems.get(0).getId();
        }
        return 0;
    }

    public void setServiceItemId(int serviceItemId) {
        this.serviceItemId = serviceItemId;
        if (serviceItemId > 0 && !this.serviceItemIds.contains(serviceItemId)) {
            this.serviceItemIds.add(serviceItemId);
        }
    }

    public int getTotalEstimatedMinutes() {
        int total = 0;
        for (ServiceItem s : serviceItems) {
            if (s != null) {
                total += s.getEstimatedMinutes();
            }
        }
        return total;
    }

    public double getTotalEstimatedCost() {
        double total = 0.0;
        for (ServiceItem s : serviceItems) {
            if (s != null) {
                total += s.getPrice();
            }
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(" - Vehicle ID: ").append(vehicleId)
          .append(" | Date: ").append(date)
          .append(" | Description: ").append(description)
          .append(" | Status: ").append(status);
        if (!serviceItems.isEmpty()) {
            sb.append(" | Services: ");
            for (int i = 0; i < serviceItems.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(serviceItems.get(i).getName());
            }
        } else if (!serviceItemIds.isEmpty()) {
            sb.append(" | Service IDs: ").append(serviceItemIds);
        } else if (serviceItemId > 0) {
            sb.append(" | Service ID: ").append(serviceItemId);
        }
        return sb.toString();
    }
}