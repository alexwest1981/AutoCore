package com.wac.autocore.service;

import com.wac.autocore.exception.AccessDeniedException;
import com.wac.autocore.exception.NotAuthenticatedException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.security.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class GuardedGarageSystem extends GarageSystem {

    private final IdentityHolder identityHolder;

    public GuardedGarageSystem(RepositoryFactory repositories, IdentityHolder identityHolder) {
        super(repositories);
        this.identityHolder = identityHolder;
    }

    private void require(Operation operation) {

        // Two refusals, two answers. Nobody signed in is a different thing from being signed in
        // without the right, and the order asks the system to tell them apart.
        java.util.Optional<Identity> current = identityHolder.current();
        if (current.isEmpty()) {
            throw new NotAuthenticatedException("Nobody is signed in, and " + operation + " needs one.");
        }

        Role role = current.get().role();
        if (!RoleMatrix.allows(role, operation)) {
            throw new AccessDeniedException("Role " + role + " may not " + operation + ".");
        }
    }

    @Override
    public Customer createCustomer(String name, String phone, String email) {

        require(Operation.CREATE_CUSTOMER);

        return super.createCustomer(name, phone, email);
    }

    @Override
    public void updateCustomer(Customer customer) {

        require(Operation.UPDATE_CUSTOMER);

        super.updateCustomer(customer);
    }

    @Override
    public void deleteCustomer(int customerId) {

        require(Operation.DELETE_CUSTOMER);

        super.deleteCustomer(customerId);
    }

    @Override
    public Vehicle createVehicle(String registrationNumber, String brand, String model, int year, int customerId) {

        require(Operation.CREATE_VEHICLE);

        return super.createVehicle(registrationNumber, brand, model, year, customerId);
    }

    @Override
    public void updateVehicle(Vehicle vehicle) {

        require(Operation.UPDATE_VEHICLE);

        super.updateVehicle(vehicle);
    }

    @Override
    public void deleteVehicle(int vehicleId) {

        require(Operation.DELETE_VEHICLE);

        super.deleteVehicle(vehicleId);
    }

    @Override
    public Booking createBooking(int vehicleId, LocalDate date, String description) {

        require(Operation.CREATE_BOOKING);

        return super.createBooking(vehicleId, date, description);
    }

    @Override
    public Booking createBooking(int vehicleId, LocalDate date, String description, LocalTime startTime, int mechanicId, int serviceItemId) {

        require(Operation.CREATE_BOOKING);

        return super.createBooking(vehicleId, date, description, startTime, mechanicId, serviceItemId);
    }

    @Override
    public Booking saveBooking(Booking booking) {

        require(Operation.UPDATE_BOOKING);

        return super.saveBooking(booking);
    }

    @Override
    public void cancelBooking(int bookingId) {

        require(Operation.CANCEL_BOOKING);

        super.cancelBooking(bookingId);
    }

    @Override
    public void deleteBooking(int bookingId) {

        require(Operation.DELETE_BOOKING);

        super.deleteBooking(bookingId);
    }

    @Override
    public Booking createDropInBooking(int vehicleId, List<ServiceItem> services, List<Mechanic> mechanics) {

        require(Operation.CREATE_BOOKING);

        return super.createDropInBooking(vehicleId, services, mechanics);
    }

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createWorkOrder(bookingId, mechanicId);
    }

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, List<Integer> serviceItemIds, String type) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createWorkOrder(bookingId, mechanicId, serviceItemIds, type);
    }

    @Override
    public WorkOrder createReclamation(int originalWorkOrderId, String description) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createReclamation(originalWorkOrderId, description);
    }

    @Override
    public WorkOrder createDraft(int vehicleId, String description) {

        require(Operation.CREATE_WORK_ORDER);

        return super.createDraft(vehicleId, description);
    }

    @Override
    public WorkOrder updateDraft(DraftChanges changes) {

        require(Operation.UPDATE_WORK_ORDER);

        return super.updateDraft(changes);
    }

    @Override
    public boolean reserveSlot(int mechanicId, LocalDate date, int hour, int bookingId, String customer, String vehicleReg, String description) {

        require(Operation.UPDATE_WORK_ORDER);

        return super.reserveSlot(mechanicId, date, hour, bookingId, customer, vehicleReg, description);
    }

    @Override
    public void releaseSlot(int bookingId) {

        require(Operation.UPDATE_WORK_ORDER);

        super.releaseSlot(bookingId);
    }

    @Override
    public void startWorkOrder(int workOrderId) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        super.startWorkOrder(workOrderId);
    }

    @Override
    public boolean confirmWorkOrder(int workOrderId) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        return super.confirmWorkOrder(workOrderId);
    }

    @Override
    public void completeWorkOrder(int workOrderId) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        super.completeWorkOrder(workOrderId);
    }

    @Override
    public boolean cancelWorkOrder(int workOrderId) {

        require(Operation.CANCEL_WORK_ORDER);

        return super.cancelWorkOrder(workOrderId);
    }

    @Override
    public boolean markServicesAsCompleted(int workOrderId, int[] serviceItemIds) {

        require(Operation.CHANGE_WORK_ORDER_STATUS);

        return super.markServicesAsCompleted(workOrderId, serviceItemIds);
    }

    @Override
    public Invoice createInvoice(int workOrderId, String discountCode) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoice(workOrderId, discountCode);
    }

    @Override
    public Invoice createInvoice(int workOrderId, String discountCode, String extraName, int extraAmount) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoice(workOrderId, discountCode, extraName, extraAmount);
    }

    @Override
    public Invoice createInvoiceForBooking(int bookingId, String discountCode) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoiceForBooking(bookingId, discountCode);
    }

    @Override
    public Invoice createInvoiceForBooking(int bookingId, String discountCode, String extraName, int extraAmount) {

        require(Operation.CREATE_INVOICE);

        return super.createInvoiceForBooking(bookingId, discountCode, extraName, extraAmount);
    }

    @Override
    public Payment processPayment(int invoiceId, String paymentType) {

        require(Operation.TAKE_PAYMENT);

        return super.processPayment(invoiceId, paymentType);
    }

    @Override
    public Mechanic createMechanic(String name, String phone, String specialization) {

        require(Operation.CREATE_MECHANIC);

        return super.createMechanic(name, phone, specialization);
    }

    @Override
    public void updateMechanic(Mechanic mechanic) {

        require(Operation.UPDATE_MECHANIC);

        super.updateMechanic(mechanic);
    }

    @Override
    public void deleteMechanic(int mechanicId) {

        require(Operation.DELETE_MECHANIC);

        super.deleteMechanic(mechanicId);
    }

    @Override
    public ServiceItem createServiceItem(String name, String description, int price, int estimatedMinutes) {

        require(Operation.CREATE_SERVICE_ITEM);

        return super.createServiceItem(name, description, price, estimatedMinutes);
    }

    @Override
    public ServiceItem createServiceItem(String name, String description, int price, int estimatedMinutes, String specialization) {

        require(Operation.CREATE_SERVICE_ITEM);

        return super.createServiceItem(name, description, price, estimatedMinutes, specialization);
    }

    @Override
    public void updateServiceItem(ServiceItem item) {

        require(Operation.UPDATE_SERVICE_ITEM);

        super.updateServiceItem(item);
    }

    @Override
    public void deleteServiceItem(int serviceItemId) {

        require(Operation.DELETE_SERVICE_ITEM);

        super.deleteServiceItem(serviceItemId);
    }

    @Override
    public ServicePackage createServicePackage(String name, String description, List<ServiceItem> serviceItems) {

        require(Operation.CREATE_SERVICE_PACKAGE);

        return super.createServicePackage(name, description, serviceItems);
    }

    @Override
    public void updateServicePackage(ServicePackage servicePackage) {

        require(Operation.UPDATE_SERVICE_PACKAGE);

        super.updateServicePackage(servicePackage);
    }

    @Override
    public void deleteServicePackage(int id) {

        require(Operation.DELETE_SERVICE_PACKAGE);

        super.deleteServicePackage(id);
    }

    @Override
    public void saveSetting(String key, String value) {

        require(Operation.CHANGE_SETTINGS);

        super.saveSetting(key, value);
    }
}
