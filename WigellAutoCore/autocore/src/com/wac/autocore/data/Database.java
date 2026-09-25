package com.wac.autocore.data;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final List<Customer> customers = new ArrayList<Customer>();
    private static final List<Vehicle> vehicles = new ArrayList<Vehicle>();
    private static final List<Booking> bookings = new ArrayList<Booking>();
    private static final List<ServiceItem> serviceItems = new ArrayList<ServiceItem>();
    private static final List<Mechanic> mechanics = new ArrayList<Mechanic>();
    private static final List<WorkOrder> workOrders = new ArrayList<WorkOrder>();
    private static final List<Invoice> invoices = new ArrayList<Invoice>();
    private static final List<Payment> payments = new ArrayList<Payment>();

    public static List<Customer> getCustomers() {
        return customers;
    }

    public static List<Vehicle> getVehicles() {
        return vehicles;
    }

    public static List<Booking> getBookings() {
        return bookings;
    }

    public static List<ServiceItem> getServiceItems() {
        return serviceItems;
    }

    public static List<Mechanic> getMechanics() {
        return mechanics;
    }

    public static List<WorkOrder> getWorkOrders() {
        return workOrders;
    }

    public static List<Invoice> getInvoices() {
        return invoices;
    }

    public static List<Payment> getPayments() {
        return payments;
    }
}