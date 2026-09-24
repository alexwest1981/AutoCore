package com.wac.autocore.service;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class GarageSystem {

    public GarageSystem() {
        Db.ensureReady();
    }

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
        try {
            return serviceItemRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read service items: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Mechanic> getMechanics() {
        try {
            return mechanicRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read mechanics: " + e.getMessage());
            return Collections.emptyList();
        }
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
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();
    private final CustomerRepository customerRepository = new CustomerRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();
    private final BookingRepository bookingRepository = new BookingRepository();

    public void showCustomers() {
        printer.printCustomers(customerService.getAll());
    }

    public void showVehicles() {
        printer.printVehicles(vehicleService.getAll());
    }

    public void showBookings() {
        printer.printBookings(bookingService.getAll());
    }

    public void showServiceItems() {
        printer.printServiceItems(getServiceItems());
    }

    public void showMechanics() {
        printer.printMechanics(getMechanics());
    }

    public void showWorkOrders() {
        printer.printWorkOrders(workOrderService.getAll());
    }

    public void showInvoices() {
        printer.printInvoices(billingService.getAll());
    }

    public void showPayments() {
        printer.printPayments(paymentService.getAll());
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

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description, LocalTime startTime, int mechanicId, int serviceItemId) throws SQLException {
        return bookingService.createBooking(vehicleId, date, description,
                startTime, mechanicId, serviceItemId);
    }

    public WorkOrder createWorkOrder(int bookingId,
                                     int mechanicId,
                                     int... serviceItemIds) {
        return workOrderService.createWorkOrder(bookingId, mechanicId, serviceItemIds);
    }

    public int getEstimatedDuration(int... serviceItemsIds) {
        return workOrderService.getTotalEstimatedMinutes(serviceItemsIds);
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

    public void updateMechanic(Mechanic mechanic) throws SQLException {
        mechanicRepository.save(mechanic);
    }

    public boolean canDeleteMechanic(int mechanicId) {
        List<WorkOrder> orders = getWorkOrders();
        for (WorkOrder wo : orders) {
            if (wo.getMechanicId() == mechanicId && !"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                return false;
            }
        }
        return true;
    }

    public void deleteMechanic(int mechanicId) throws SQLException {
        mechanicRepository.delete(mechanicId);
        MechanicSchedule.getInstance().removeSlotsForMechanic(mechanicId);
    }

    public Mechanic createMechanic(String name, String phone, String specialization) throws SQLException {
        Mechanic mechanic = new Mechanic(0, name, phone, specialization);
        mechanicRepository.save(mechanic);
        return mechanic;
    }

    public void updateCustomer(Customer customer) throws SQLException {
        customerRepository.save(customer);
    }

    public boolean canDeleteCustomer(int customerId) {
        for (Vehicle v : getVehicles()) {
            if (v.getCustomerId() == customerId) {
                if (!canDeleteVehicle(v.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    public void deleteCustomer(int customerId) throws SQLException {
        for (Vehicle v : getVehicles()) {
            if (v.getCustomerId() == customerId) {
                vehicleRepository.delete(v.getId());
            }
        }
        customerRepository.delete(customerId);
    }

    public void updateVehicle(Vehicle vehicle) throws SQLException {
        vehicleRepository.save(vehicle);
    }

    public boolean canDeleteVehicle(int vehicleId) {
        for (Booking b : getBookings()) {
            if (b.getVehicleId() == vehicleId && !"CANCELLED".equalsIgnoreCase(b.getStatus())) {
                return false;
            }
        }
        for (WorkOrder wo : getWorkOrders()) {
            if (!"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                for (Booking b : getBookings()) {
                    if (b.getId() == wo.getBookingId() && b.getVehicleId() == vehicleId) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void deleteVehicle(int vehicleId) throws SQLException {
        vehicleRepository.delete(vehicleId);
    }

    public void updateBooking(Booking booking) throws SQLException {
        bookingRepository.save(booking);
        MechanicSchedule.getInstance().syncFromDatabase();
    }

    public boolean canCancelOrDeleteBooking(int bookingId) {
        for (WorkOrder wo : getWorkOrders()) {
            if (wo.getBookingId() == bookingId && !"COMPLETED".equalsIgnoreCase(wo.getStatus())) {
                return false;
            }
        }
        return true;
    }

    public void cancelBooking(int bookingId) throws SQLException {
        Booking b = bookingService.findById(bookingId);
        if (b != null) {
            b.setStatus("CANCELLED");
            bookingRepository.save(b);
            MechanicSchedule.getInstance().syncFromDatabase();
        }
    }

    public void deleteBooking(int bookingId) throws SQLException {
        bookingRepository.delete(bookingId);
        MechanicSchedule.getInstance().syncFromDatabase();
    }

    public ServiceItem createServiceItem(String name, String description, double price, int estimatedMinutes) throws SQLException {
        ServiceItem item = new ServiceItem(0, name, description, price, estimatedMinutes);
        serviceItemRepository.save(item);
        return item;
    }

    public void updateServiceItem(ServiceItem item) throws SQLException {
        serviceItemRepository.save(item);
    }

    public boolean canDeleteServiceItem(int serviceItemId) {
        for (WorkOrder wo : getWorkOrders()) {
            if (!"COMPLETED".equalsIgnoreCase(wo.getStatus()) && wo.getServiceItemIds() != null) {
                for (int id : wo.getServiceItemIds()) {
                    if (id == serviceItemId) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void deleteServiceItem(int serviceItemId) throws SQLException {
        serviceItemRepository.delete(serviceItemId);
    }
}
