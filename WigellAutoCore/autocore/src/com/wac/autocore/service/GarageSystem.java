package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class GarageSystem {

    public List<Customer> getCustomers() {
        return customerService.getAll();
    }

    public List<Vehicle> getVehicles() {
        return vehicleService.getAll();
    }

    public List<Booking> getBookings() {
        return bookingService.getAll();
    }

    public List<ServiceItem> getServiceItems() {
        return Collections.unmodifiableList(Database.getServiceItems());
    }

    public List<Mechanic> getMechanics() {
        return Collections.unmodifiableList(Database.getMechanics());
    }

    public List<WorkOrder> getWorkOrders() {
        return workOrderService.getAll();
    }

    public List<Invoice> getInvoices() {
        return billingService.getAll();
    }

    public List<Payment> getPayments() {
        return paymentService.getAll();
    }

    private final com.wac.autocore.ui.ConsolePrinter printer = new com.wac.autocore.ui.ConsolePrinter();
    private final CustomerService customerService = new CustomerService();
    private final VehicleService vehicleService = new VehicleService(customerService);
    private final WorkOrderService workOrderService = new WorkOrderService();
    private final BookingService bookingService = new BookingService();
    private final BillingService billingService = new BillingService();
    private final PaymentService paymentService = new PaymentService();

    public void showCustomers() {
        printer.printCustomers(Database.getCustomers());
    }

    public void showVehicles() {
        printer.printVehicles(Database.getVehicles());
    }

    public void showBookings() {
        printer.printBookings(Database.getBookings());
    }

    public void showServiceItems() {
        printer.printServiceItems(Database.getServiceItems());
    }

    public void showMechanics() {
        printer.printMechanics(Database.getMechanics());
    }

    public void showWorkOrders() {
        printer.printWorkOrders(Database.getWorkOrders());
    }

    public void showInvoices() {
        printer.printInvoices(Database.getInvoices());
    }

    public void showPayments() {
        printer.printPayments(Database.getPayments());
    }

    public Customer createCustomer(String name, String phone, String email) {
        return customerService.createCustomer(name, phone, email);
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {
        return vehicleService.createVehicle(registrationNumber, brand, model, year, customerId);
    }

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description) {
        return bookingService.createBooking(vehicleId, date, description);
    }

    public WorkOrder createWorkOrder(int bookingId,
                                     int mechanicId,
                                     int... serviceItemIds) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    public void startWorkOrder(int workOrderId) {
        workOrderService.startWorkOrder(workOrderId);
    }

    public void completeWorkOrder(int workOrderId) {
        workOrderService.completeWorkOrder(workOrderId);
    }

    public Invoice createInvoice(int workOrderId, String discountCode) {
        return billingService.createInvoice(workOrderId, discountCode);
    }

    public Payment processPayment(int invoiceId, String paymentType) {
        return paymentService.processPayment(invoiceId, paymentType);
    }
}