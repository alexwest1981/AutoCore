package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkOrder {

    /** Standard, reclamation or internal work. The code lives here and nowhere else. */
    public static final String STANDARD = "STANDARD";
    public static final String RECLAMATION = "RECLAMATION";
    public static final String INTERNAL = "INTERNAL";

    /** The types you can pick. The order is the one the picker shows, and the first is standard. */
    public static final List<String> TYPES = Arrays.asList(STANDARD, RECLAMATION, INTERNAL);

    private int id;
    private int bookingId;
    private int mechanicId;
    private List<Integer> serviceItemIds;
    private final List<Integer> completedServiceItems = new ArrayList<Integer>();
    private final Map<Integer, Double> completedServicePrices = new LinkedHashMap<Integer, Double>();
    /** The package each service came from, carried over from the booking. */
    private final Map<Integer, String> servicePackages = new LinkedHashMap<Integer, String>();
    private String status;
    private String plannedDate;
    private String customerInstructions;
    private String otherComments;
    private String type;

    // The work order the reclamation concerns. 0 means the order is not a reclamation.
    private int originalWorkOrderId;
    private int vehicleId;
    private String description;

    public WorkOrder(int id, int bookingId, int mechanicId) {
        this.id = id;
        this.bookingId = bookingId;
        this.mechanicId = mechanicId;
        this.serviceItemIds = new ArrayList<Integer>();
        this.status = "CREATED";
        this.type = STANDARD;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOriginalWorkOrderId() {
        return originalWorkOrderId;
    }

    public void setOriginalWorkOrderId(int originalWorkOrderId) {
        this.originalWorkOrderId = originalWorkOrderId;
    }

    /** The planned date on a draft, as text the same way as the booking's date. */
    public String getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(String plannedDate) {
        this.plannedDate = plannedDate;
    }

    public String getCustomerInstructions() {
        return customerInstructions;
    }

    public void setCustomerInstructions(String customerInstructions) {
        this.customerInstructions = customerInstructions;
    }

    public String getOtherComments() {
        return otherComments;
    }

    public void setOtherComments(String otherComments) {
        this.otherComments = otherComments;
    }

    /** A reclamation is not charged to the customer, but it must still show on the invoice. */
    public boolean isReclamation() {
        return RECLAMATION.equals(type);
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

    /** Returns the price that applied when the service was performed. */
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

    /** The package name for one service, or an empty string when it came without a package. */
    public String getServicePackageName(int serviceItemId) {
        String name = servicePackages.get(Integer.valueOf(serviceItemId));
        return name == null ? "" : name;
    }

    public void setServicePackage(int serviceItemId, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            servicePackages.remove(Integer.valueOf(serviceItemId));
            return;
        }
        servicePackages.put(Integer.valueOf(serviceItemId), packageName);
    }

    public void setServicePackages(Map<Integer, String> packages) {
        servicePackages.clear();
        if (packages != null) {
            servicePackages.putAll(packages);
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

    /** Marks the service as performed and stores the price that applied then. */
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
                " | Status: " + status +
                " | Type: " + type +
                (originalWorkOrderId > 0 ? " | Reclamation of: " + originalWorkOrderId : "");
    }
}