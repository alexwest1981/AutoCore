package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.util.List;

// The caller decides where the lists go, so the service never points at a window.
public interface Printer {

    default void printCustomers(List<Customer> customers) { }

    default void printVehicles(List<Vehicle> vehicles) { }

    default void printBookings(List<Booking> bookings) { }

    default void printServiceItems(List<ServiceItem> serviceItems) { }

    default void printMechanics(List<Mechanic> mechanics) { }

    default void printWorkOrders(List<WorkOrder> workOrders) { }

    default void printInvoices(List<Invoice> invoices) { }

    default void printPayments(List<Payment> payments) { }
}