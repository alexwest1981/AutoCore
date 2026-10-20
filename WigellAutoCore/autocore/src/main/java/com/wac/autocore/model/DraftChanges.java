package com.wac.autocore.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


/* Everything a draft needs when it is saved, gathered in one object so that two
 * values of the same type cannot change places in the call. */
public final class DraftChanges {

    private final int workOrderId;
    private final int vehicleId;
    private final String description;
    private final int mechanicId;
    private final List<Integer> serviceItemIds;
    private final LocalDate plannedDate;
    private final LocalTime plannedTime;
    private final String customerInstructions;
    private final String otherComments;

    private DraftChanges(Builder builder) {
        this.workOrderId = builder.workOrderId;
        this.vehicleId = builder.vehicleId;
        this.description = builder.description;
        this.mechanicId = builder.mechanicId;
        this.serviceItemIds = builder.serviceItemIds;
        this.plannedDate = builder.plannedDate;
        this.plannedTime = builder.plannedTime;
        this.customerInstructions = builder.customerInstructions;
        this.otherComments = builder.otherComments;
    }

    public int getWorkOrderId() {
        return workOrderId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public String getDescription() {
        return description;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public List<Integer> getServiceItemIds() {
        return serviceItemIds;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public LocalTime getPlannedTime() {
        return plannedTime;
    }

    public String getCustomerInstructions() {
        return customerInstructions;
    }

    public String getOtherComments() {
        return otherComments;
    }

    public static Builder forWorkOrder(int workOrderId) {
        return new Builder(workOrderId);
    }

    /** Every value has a name here, which is what the nine parameters never gave. */
    public static final class Builder {

        private final int workOrderId;
        private int vehicleId;
        private String description;
        private int mechanicId;
        private List<Integer> serviceItemIds;
        private LocalDate plannedDate;
        private LocalTime plannedTime;
        private String customerInstructions;
        private String otherComments;

        private Builder(int workOrderId) {
            this.workOrderId = workOrderId;
        }

        public Builder vehicle(int vehicleId) {
            this.vehicleId = vehicleId; return this;
        }

        public Builder description(String description) {
            this.description = description; return this;
        }

        public Builder mechanic(int mechanicId) {
            this.mechanicId = mechanicId; return this;
        }

        public Builder services(List<Integer> serviceItemIds) {
            this.serviceItemIds = serviceItemIds; return this;
        }

        public Builder plannedAt(LocalDate date, LocalTime time) {
            this.plannedDate = date; this.plannedTime = time; return this;
        }

        public Builder customerInstructions(String customerInstructions) {
            this.customerInstructions = customerInstructions; return this;
        }

        public Builder otherComments(String otherComments) {
            this.otherComments = otherComments; return this;
        }

        public DraftChanges build() {
            return new DraftChanges(this);
        }
    }
}