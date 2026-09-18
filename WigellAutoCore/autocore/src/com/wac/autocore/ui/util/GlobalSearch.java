package com.wac.autocore.ui.util;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ren söklogik för att söka igenom hela AutoCore-systemet över alla domänentiteter.
 * Frikopplad från JavaFX så att all sök- och matchningslogik kan enhetstestas.
 */
public final class GlobalSearch {

    private GlobalSearch() {}

    public static class SearchResults {
        private final String query;
        private final List<Customer> customers;
        private final List<Vehicle> vehicles;
        private final List<WorkOrder> workOrders;
        private final List<Booking> bookings;
        private final List<Mechanic> mechanics;
        private final List<Invoice> invoices;
        private final List<ServiceItem> services;

        public SearchResults(String query,
                             List<Customer> customers,
                             List<Vehicle> vehicles,
                             List<WorkOrder> workOrders,
                             List<Booking> bookings,
                             List<Mechanic> mechanics,
                             List<Invoice> invoices,
                             List<ServiceItem> services) {
            this.query = query == null ? "" : query.trim();
            this.customers = Collections.unmodifiableList(customers);
            this.vehicles = Collections.unmodifiableList(vehicles);
            this.workOrders = Collections.unmodifiableList(workOrders);
            this.bookings = Collections.unmodifiableList(bookings);
            this.mechanics = Collections.unmodifiableList(mechanics);
            this.invoices = Collections.unmodifiableList(invoices);
            this.services = Collections.unmodifiableList(services);
        }

        public String getQuery() {
            return query;
        }

        public List<Customer> getCustomers() {
            return customers;
        }

        public List<Vehicle> getVehicles() {
            return vehicles;
        }

        public List<WorkOrder> getWorkOrders() {
            return workOrders;
        }

        public List<Booking> getBookings() {
            return bookings;
        }

        public List<Mechanic> getMechanics() {
            return mechanics;
        }

        public List<Invoice> getInvoices() {
            return invoices;
        }

        public List<ServiceItem> getServices() {
            return services;
        }

        public int getTotalMatches() {
            return customers.size() + vehicles.size() + workOrders.size()
                    + bookings.size() + mechanics.size() + invoices.size()
                    + services.size();
        }

        public int getSectionsWithMatchesCount() {
            int count = 0;
            if (!customers.isEmpty()) count++;
            if (!vehicles.isEmpty()) count++;
            if (!workOrders.isEmpty()) count++;
            if (!bookings.isEmpty()) count++;
            if (!mechanics.isEmpty()) count++;
            if (!invoices.isEmpty()) count++;
            if (!services.isEmpty()) count++;
            return count;
        }

        public boolean isEmpty() {
            return getTotalMatches() == 0;
        }
    }

    /**
     * Utför en granulär sökning över alla entiteter i GarageSystem.
     */
    public static SearchResults search(GarageSystem garage, String query) {
        if (garage == null || query == null || query.trim().isEmpty()) {
            return new SearchResults(query,
                    Collections.<Customer>emptyList(), Collections.<Vehicle>emptyList(),
                    Collections.<WorkOrder>emptyList(), Collections.<Booking>emptyList(),
                    Collections.<Mechanic>emptyList(), Collections.<Invoice>emptyList(),
                    Collections.<ServiceItem>emptyList());
        }

        final String q = query.trim().toLowerCase();

        // 1. Kunder
        List<Customer> matchingCustomers = new ArrayList<Customer>();
        for (Customer c : garage.getCustomers()) {
            if (c == null) continue;
            if (containsIgnoreCase(c.getName(), q)
                    || containsIgnoreCase(c.getPhone(), q)
                    || containsIgnoreCase(c.getEmail(), q)
                    || String.valueOf(c.getId()).equals(q)) {
                matchingCustomers.add(c);
            }
        }

        // 2. Fordon
        List<Vehicle> matchingVehicles = new ArrayList<Vehicle>();
        for (Vehicle v : garage.getVehicles()) {
            if (v == null) continue;
            String ownerName = EntityLookup.customerName(garage, v.getCustomerId());
            if (containsIgnoreCase(v.getRegistrationNumber(), q)
                    || containsIgnoreCase(v.getBrand(), q)
                    || containsIgnoreCase(v.getModel(), q)
                    || String.valueOf(v.getYear()).contains(q)
                    || containsIgnoreCase(ownerName, q)
                    || String.valueOf(v.getId()).equals(q)) {
                matchingVehicles.add(v);
            }
        }

        // 3. Arbetsordrar
        List<WorkOrder> matchingOrders = new ArrayList<WorkOrder>();
        for (WorkOrder wo : garage.getWorkOrders()) {
            if (wo == null) continue;
            String custName = EntityLookup.workOrderCustomerName(garage, wo);
            String vehReg = EntityLookup.workOrderVehicleReg(garage, wo);
            String mechName = EntityLookup.mechanicName(garage, wo.getMechanicId());
            String sNames = EntityLookup.serviceNames(garage, wo.getServiceItemIds());
            if (String.valueOf(wo.getId()).equals(q)
                    || ("order #" + wo.getId()).toLowerCase().contains(q)
                    || containsIgnoreCase(vehReg, q)
                    || containsIgnoreCase(custName, q)
                    || containsIgnoreCase(mechName, q)
                    || containsIgnoreCase(wo.getStatus(), q)
                    || containsIgnoreCase(sNames, q)) {
                matchingOrders.add(wo);
            }
        }

        // 4. Bokningar
        List<Booking> matchingBookings = new ArrayList<Booking>();
        for (Booking b : garage.getBookings()) {
            if (b == null) continue;
            String custName = EntityLookup.bookingCustomerName(garage, b.getId());
            String vehReg = EntityLookup.vehicleReg(garage, b.getVehicleId());
            if (String.valueOf(b.getId()).equals(q)
                    || ("booking #" + b.getId()).toLowerCase().contains(q)
                    || containsIgnoreCase(custName, q)
                    || containsIgnoreCase(vehReg, q)
                    || containsIgnoreCase(b.getStatus(), q)
                    || (b.getDate() != null && b.getDate().toString().contains(q))
                    || containsIgnoreCase(b.getDescription(), q)) {
                matchingBookings.add(b);
            }
        }

        // 5. Mekaniker
        List<Mechanic> matchingMechanics = new ArrayList<Mechanic>();
        for (Mechanic m : garage.getMechanics()) {
            if (m == null) continue;
            if (String.valueOf(m.getId()).equals(q)
                    || containsIgnoreCase(m.getName(), q)
                    || containsIgnoreCase(m.getPhone(), q)
                    || containsIgnoreCase(m.getSpecialization(), q)) {
                matchingMechanics.add(m);
            }
        }

        // 6. Fakturor
        List<Invoice> matchingInvoices = new ArrayList<Invoice>();
        for (Invoice inv : garage.getInvoices()) {
            if (inv == null) continue;
            String custName = EntityLookup.invoiceCustomerName(garage, inv);
            if (String.valueOf(inv.getId()).equals(q)
                    || ("invoice #" + inv.getId()).toLowerCase().contains(q)
                    || containsIgnoreCase(custName, q)
                    || String.valueOf(inv.getWorkOrderId()).equals(q)) {
                matchingInvoices.add(inv);
            }
        }

        // 7. Tjänster
        List<ServiceItem> matchingServices = new ArrayList<ServiceItem>();
        for (ServiceItem si : garage.getServiceItems()) {
            if (si == null) continue;
            if (String.valueOf(si.getId()).equals(q)
                    || containsIgnoreCase(si.getName(), q)
                    || containsIgnoreCase(si.getDescription(), q)) {
                matchingServices.add(si);
            }
        }

        return new SearchResults(query,
                matchingCustomers,
                matchingVehicles,
                matchingOrders,
                matchingBookings,
                matchingMechanics,
                matchingInvoices,
                matchingServices);
    }

    private static boolean containsIgnoreCase(String source, String queryLower) {
        if (source == null || queryLower == null) {
            return false;
        }
        return source.toLowerCase().contains(queryLower);
    }
}
