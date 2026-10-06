package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;

public class ServicePackage {
    private int id;
    private String name;
    private String description;
    private List<ServiceItem> serviceItems = new ArrayList<ServiceItem>();

    public ServicePackage(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ServiceItem> getServiceItems() {
        return serviceItems;
    }

    public void setServiceItems(List<ServiceItem> serviceItems) {
        this.serviceItems = new ArrayList<ServiceItem>(serviceItems);
    }

    public void addServiceItem(ServiceItem item) {
        serviceItems.add(item);
    }

    public double getTotalPrice() {
        double total = 0;
        for (ServiceItem item : serviceItems) {
            total += item.getPrice();
        }
        return total;
    }

    public int getTotalEstimatedMinutes() {
        int total = 0;
        for (ServiceItem item : serviceItems) {
            total += item.getEstimatedMinutes();
        }
        return total;
    }

    @Override
    public String toString() {
        return name;
    }
}
