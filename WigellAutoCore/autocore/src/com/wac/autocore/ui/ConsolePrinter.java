package com.wac.autocore.ui;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.util.List;

/** All utskrift för textversionen. Samlad här så servicelagret slipper skriva till konsolen. */
public class ConsolePrinter {

    public void printCustomers(List<Customer> customers) {
        printHeader("CUSTOMERS");
        if (customers == null || customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        for (Customer customer : customers) {
            System.out.println(customer);
        }
    }

    public void printVehicles(List<Vehicle> vehicles) {
        printHeader("VEHICLES");
        if (vehicles == null || vehicles.isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }
        for (Vehicle vehicle : vehicles) {
            System.out.println(vehicle);
        }
    }

    public void printBookings(List<Booking> bookings) {
        printHeader("BOOKINGS");
        if (bookings == null || bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        for (Booking booking : bookings) {
            System.out.println(booking);
        }
    }

    public void printServiceItems(List<ServiceItem> serviceItems) {
        printHeader("SERVICES");
        if (serviceItems == null || serviceItems.isEmpty()) {
            System.out.println("No services found.");
            return;
        }
        for (ServiceItem item : serviceItems) {
            System.out.println(item);
        }
    }

    public void printMechanics(List<Mechanic> mechanics) {
        printHeader("MECHANICS");
        if (mechanics == null || mechanics.isEmpty()) {
            System.out.println("No mechanics found.");
            return;
        }
        for (Mechanic mechanic : mechanics) {
            System.out.println(mechanic);
        }
    }

    public void printWorkOrders(List<WorkOrder> workOrders) {
        printHeader("WORK ORDERS");
        if (workOrders == null || workOrders.isEmpty()) {
            System.out.println("No work orders found.");
            return;
        }
        for (WorkOrder order : workOrders) {
            System.out.println(order);
            if (order.getServiceItemIds() != null && !order.getServiceItemIds().isEmpty()) {
                System.out.println("   Services to perform (" + order.getServiceItemIds().size() + "):");
                for (Integer sid : order.getServiceItemIds()) {
                    boolean done = order.getCompletedServiceItems() != null && order.getCompletedServiceItems().contains(sid);
                    Double price = order.getCompletedServicePrice(sid.intValue());
                    String priceStr = price != null ? " (" + price + " kr)" : "";
                    String st = done ? "[Completed]" : "[To perform]";
                    System.out.println("     • Service #" + sid + priceStr + " " + st);
                }
            }
        }
    }

    public void printInvoices(List<Invoice> invoices) {
        printHeader("INVOICES");
        if (invoices == null || invoices.isEmpty()) {
            System.out.println("No invoices found.");
            return;
        }
        for (Invoice invoice : invoices) {
            System.out.println(invoice);
        }
    }

    public void printPayments(List<Payment> payments) {
        printHeader("PAYMENTS");
        if (payments == null || payments.isEmpty()) {
            System.out.println("No payments found.");
            return;
        }
        for (Payment payment : payments) {
            System.out.println(payment);
        }
    }


    private void printHeader(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
