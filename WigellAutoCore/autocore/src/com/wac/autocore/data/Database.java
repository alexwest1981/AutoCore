package com.wac.autocore.data;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;

import java.time.LocalDate;
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

    static {
        loadSampleData();
    }

    private static void loadSampleData() {

        customers.add(new Customer(
                1,
                "Anna Andersson",
                "070-1111111",
                "anna.andersson@email.se"
        ));

        customers.add(new Customer(
                2,
                "Erik Eriksson",
                "070-2222222",
                "erik.eriksson@email.se"
        ));

        customers.add(new Customer(
                3,
                "Maria Svensson",
                "070-3333333",
                "maria.svensson@email.se"
        ));

        customers.add(new Customer(
                4,
                "Olof Palme",
                "070-4444444",
                "olof.palme@email.se"
        ));

        customers.add(new Customer(
                5,
                "Sven Melander",
                "070-5555555",
                "sven.melander@email.se"
        ));

        customers.add(new Customer(
                6,
                "Gustav Vasa",
                "070-6666666",
                "gustav.vasa@email.se"
        ));

        customers.get(1).setVip(true);

        vehicles.add(new Vehicle(
                1,
                "ABC123",
                "Volvo",
                "V70",
                2012,
                1
        ));

        vehicles.add(new Vehicle(
                2,
                "DEF456",
                "Volkswagen",
                "Passat",
                2018,
                2
        ));

        vehicles.add(new Vehicle(
                3,
                "GHI789",
                "Toyota",
                "Corolla",
                2020,
                3
        ));

        vehicles.add(new Vehicle(
                4,
                "XYZ999",
                "BMW",
                "320d",
                2021,
                4
        ));

        vehicles.add(new Vehicle(
                5,
                "AAA001",
                "Audi",
                "A4",
                2019,
                5
        ));

        vehicles.add(new Vehicle(
                6,
                "BBB002",
                "Mercedes",
                "C220",
                2022,
                6
        ));

        serviceItems.add(new ServiceItem(
                1,
                "Oil change",
                "Engine oil and oil filter replacement",
                1295.0,
                45
        ));

        serviceItems.add(new ServiceItem(
                2,
                "Brake service",
                "Inspection and replacement of front brake pads",
                2495.0,
                90
        ));

        serviceItems.add(new ServiceItem(
                3,
                "Diagnostics",
                "Electronic fault code diagnostics",
                995.0,
                60
        ));

        serviceItems.add(new ServiceItem(
                4,
                "Annual service",
                "Standard annual vehicle service",
                3495.0,
                120
        ));

        mechanics.add(new Mechanic(
                1,
                "Johan Karlsson",
                "070-5551111",
                "General service"
        ));

        mechanics.add(new Mechanic(
                2,
                "Sara Nilsson",
                "070-5552222",
                "Brakes"
        ));

        mechanics.add(new Mechanic(
                3,
                "Mikael Berg",
                "070-5553333",
                "Diagnostics"
        ));

        LocalDate today = LocalDate.now();

        bookings.add(new Booking(
                1,
                1,
                today,
                "Oljeservice & filterbyte"
        ));

        bookings.add(new Booking(
                2,
                2,
                today,
                "Bromskontroll fram"
        ));

        bookings.add(new Booking(
                3,
                3,
                today.plusDays(1),
                "Helrenovering bromsar"
        ));

        bookings.add(new Booking(
                4,
                4,
                today,
                "Bromsok och belägg"
        ));

        bookings.add(new Booking(
                5,
                5,
                today,
                "Felkodsläsning OBD2"
        ));

        bookings.add(new Booking(
                6,
                6,
                today.plusDays(1),
                "Elektronikfelsökning"
        ));

        // Initialisera arbetsordrar kopplade till bokningarna och mekanikerna
        WorkOrder wo1 = new WorkOrder(1, 1, 1);
        wo1.addServiceItem(1);
        wo1.setStatus("IN_PROGRESS");
        workOrders.add(wo1);

        WorkOrder wo2 = new WorkOrder(2, 2, 1);
        wo2.addServiceItem(2);
        wo2.setStatus("CREATED");
        workOrders.add(wo2);

        WorkOrder wo3 = new WorkOrder(3, 4, 2);
        wo3.addServiceItem(2);
        wo3.setStatus("IN_PROGRESS");
        workOrders.add(wo3);

        WorkOrder wo4 = new WorkOrder(4, 5, 3);
        wo4.addServiceItem(3);
        wo4.setStatus("IN_PROGRESS");
        workOrders.add(wo4);

        WorkOrder wo5 = new WorkOrder(5, 3, 1);
        wo5.addServiceItem(2);
        wo5.addServiceItem(4);
        wo5.setStatus("CREATED");
        workOrders.add(wo5);

        WorkOrder wo6 = new WorkOrder(6, 6, 3);
        wo6.addServiceItem(3);
        wo6.setStatus("CREATED");
        workOrders.add(wo6);
    }

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