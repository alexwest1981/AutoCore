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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.wac.autocore.seed.SeedText;

/** Söker och grupperar träffar från systemets olika delar. */
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

        /* Registren byggs EN gång per sökning. garage.getBookings() läser från databasen varje
           anrop (cirka 5 ms), och uppslagen i EntityLookup går igenom listan på nytt för varje rad.
           För ett "a", som träffar allt, blev det hundratals databasanrop och två sekunder fryst
           gränssnitt per tangenttryckning i sökfältet. En genomgång per lista räcker. */
        final List<Customer> allCustomers = garage.getCustomers();
        final List<Vehicle> allVehicles = garage.getVehicles();
        final List<WorkOrder> allWorkOrders = garage.getWorkOrders();
        final List<Booking> allBookings = garage.getBookings();
        final List<Mechanic> allMechanics = garage.getMechanics();

        final Map<Integer, String> customerNames = new HashMap<Integer, String>();
        for (Customer c : allCustomers) {
            if (c != null) {
                customerNames.put(Integer.valueOf(c.getId()), c.getName());
            }
        }

        final Map<Integer, String> vehicleRegs = new HashMap<Integer, String>();
        final Map<Integer, Integer> vehicleOwners = new HashMap<Integer, Integer>();
        for (Vehicle v : allVehicles) {
            if (v != null) {
                vehicleRegs.put(Integer.valueOf(v.getId()), v.getRegistrationNumber());
                vehicleOwners.put(Integer.valueOf(v.getId()), Integer.valueOf(v.getCustomerId()));
            }
        }

        final Map<Integer, Integer> bookingVehicles = new HashMap<Integer, Integer>();
        for (Booking b : allBookings) {
            if (b != null) {
                bookingVehicles.put(Integer.valueOf(b.getId()), Integer.valueOf(b.getVehicleId()));
            }
        }

        final Map<Integer, Integer> orderBookings = new HashMap<Integer, Integer>();
        for (WorkOrder wo : allWorkOrders) {
            if (wo != null) {
                orderBookings.put(Integer.valueOf(wo.getId()), Integer.valueOf(wo.getBookingId()));
            }
        }

        final Map<Integer, String> mechanicNames = new HashMap<Integer, String>();
        for (Mechanic m : allMechanics) {
            if (m != null) {
                mechanicNames.put(Integer.valueOf(m.getId()), m.getName());
            }
        }

        // 1. Kunder
        List<Customer> matchingCustomers = new ArrayList<Customer>();
        for (Customer c : allCustomers) {
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
        for (Vehicle v : allVehicles) {
            if (v == null) continue;
            String ownerName = customerName(customerNames, v.getCustomerId());
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
        for (WorkOrder wo : allWorkOrders) {
            if (wo == null) continue;
            String custName = bookingCustomer(bookingVehicles, vehicleOwners, customerNames, wo.getBookingId());
            String vehReg = bookingReg(bookingVehicles, vehicleRegs, wo.getBookingId());
            String mechName = mechanicName(mechanicNames, wo.getMechanicId());
            String sNames = serviceNames(garage, wo.getServiceItemIds());
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
        for (Booking b : allBookings) {
            if (b == null) continue;
            String custName = bookingCustomer(bookingVehicles, vehicleOwners, customerNames, b.getId());
            String vehReg = vehicleReg(vehicleRegs, b.getVehicleId());
            if (String.valueOf(b.getId()).equals(q)
                    || ("booking #" + b.getId()).toLowerCase().contains(q)
                    || containsIgnoreCase(custName, q)
                    || containsIgnoreCase(vehReg, q)
                    || containsIgnoreCase(b.getStatus(), q)
                    || (b.getDate() != null && b.getDate().toString().contains(q))
                    || containsIgnoreCase(SeedText.resolve(b.getDescription()), q)) {
                matchingBookings.add(b);
            }
        }

        // 5. Mekaniker
        List<Mechanic> matchingMechanics = new ArrayList<Mechanic>();
        for (Mechanic m : allMechanics) {
            if (m == null) continue;
            if (String.valueOf(m.getId()).equals(q)
                    || containsIgnoreCase(m.getName(), q)
                    || containsIgnoreCase(m.getPhone(), q)
                    || containsIgnoreCase(SeedText.resolve(m.getSpecialization()), q)) {
                matchingMechanics.add(m);
            }
        }

        // 6. Fakturor
        List<Invoice> matchingInvoices = new ArrayList<Invoice>();
        for (Invoice inv : garage.getInvoices()) {
            if (inv == null) continue;
            String custName = invoiceCustomerName(orderBookings, bookingVehicles, vehicleOwners, customerNames, inv);
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
                    || containsIgnoreCase(SeedText.resolve(si.getName()), q)
                    || containsIgnoreCase(SeedText.resolve(si.getDescription()), q)) {
                matchingServices.add(si);
            }
        }

        sortPrefixMatches(matchingCustomers, c -> c.getName() + " " + c.getEmail(), q);
        sortPrefixMatches(matchingVehicles, v -> v.getBrand() + " " + v.getModel() + " " + v.getRegistrationNumber(), q);
        sortPrefixMatches(matchingOrders, wo -> bookingCustomer(bookingVehicles, vehicleOwners, customerNames, wo.getBookingId())
                + " " + bookingReg(bookingVehicles, vehicleRegs, wo.getBookingId()), q);
        sortPrefixMatches(matchingBookings, b -> SeedText.resolve(b.getDescription())
                + " " + bookingReg(bookingVehicles, vehicleRegs, b.getId()), q);
        sortPrefixMatches(matchingMechanics, m -> m.getName() + " " + SeedText.resolve(m.getSpecialization()), q);
        sortPrefixMatches(matchingInvoices, inv -> "Invoice #" + inv.getId() + " "
                + invoiceCustomerName(orderBookings, bookingVehicles, vehicleOwners, customerNames, inv), q);
        sortPrefixMatches(matchingServices, s -> SeedText.resolve(s.getName()), q);

        return new SearchResults(query,
                matchingCustomers,
                matchingVehicles,
                matchingOrders,
                matchingBookings,
                matchingMechanics,
                matchingInvoices,
                matchingServices);
    }

    /** Kunden bakom ett fordon. Samma text som EntityLookup.customerName. */
    private static String customerName(Map<Integer, String> names, int id) {
        String name = names.get(Integer.valueOf(id));
        return name != null ? name : "Customer #" + id;
    }

    /** Registreringsnumret för ett fordon. Samma text som EntityLookup.vehicleReg. */
    private static String vehicleReg(Map<Integer, String> regs, int id) {
        String reg = regs.get(Integer.valueOf(id));
        return reg != null ? reg : "Vehicle #" + id;
    }

    /** Mekanikerns namn. Samma text som EntityLookup.mechanicName. */
    private static String mechanicName(Map<Integer, String> names, int id) {
        String name = names.get(Integer.valueOf(id));
        return name != null ? name : "Mechanic #" + id;
    }

    /** Registreringsnumret för en boknings fordon, som EntityLookup.bookingVehicleReg. */
    private static String bookingReg(Map<Integer, Integer> bookingVehicles,
                                     Map<Integer, String> regs, int bookingId) {
        Integer vehicleId = bookingVehicles.get(Integer.valueOf(bookingId));
        return vehicleId != null ? vehicleReg(regs, vehicleId.intValue()) : "Booking #" + bookingId;
    }

    /** Kunden bakom en bokning, via fordonet. Samma kedja som EntityLookup.bookingCustomerName. */
    private static String bookingCustomer(Map<Integer, Integer> bookingVehicles,
                                          Map<Integer, Integer> vehicleOwners,
                                          Map<Integer, String> names, int bookingId) {
        Integer vehicleId = bookingVehicles.get(Integer.valueOf(bookingId));
        if (vehicleId == null) return "-";
        Integer customerId = vehicleOwners.get(vehicleId);
        if (customerId == null) return "-";
        return customerName(names, customerId.intValue());
    }

    /** Kunden bakom en faktura: fakturan hänger på en arbetsorder, som hänger på en bokning. */
    private static String invoiceCustomerName(Map<Integer, Integer> orderBookings,
                                              Map<Integer, Integer> bookingVehicles,
                                              Map<Integer, Integer> vehicleOwners,
                                              Map<Integer, String> names, Invoice inv) {
        if (inv == null) return "-";
        Integer bookingId = orderBookings.get(Integer.valueOf(inv.getWorkOrderId()));
        if (bookingId == null) return "-";
        return bookingCustomer(bookingVehicles, vehicleOwners, names, bookingId.intValue());
    }

    /**
     * Tjänstenamnen på en arbetsorder. Tjänstekatalogen är liten, så den slås upp direkt i stället
     * för att byggas till ett register. Samma text som EntityLookup.serviceNames.
     */
    private static String serviceNames(GarageSystem garage, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        List<ServiceItem> services = garage.getServiceItems();
        StringBuilder sb = new StringBuilder();
        for (Integer sid : ids) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String found = null;
            for (ServiceItem s : services) {
                if (s.getId() == sid.intValue()) {
                    found = SeedText.resolve(s.getName());
                    break;
                }
            }
            sb.append(found != null ? found : "Service #" + sid);
        }
        return sb.toString();
    }

    private static <T> void sortPrefixMatches(List<T> list, final java.util.function.Function<T, String> textExtractor, final String queryLower) {
        if (list == null || list.size() <= 1 || queryLower == null || queryLower.isEmpty()) return;

        /* Nyckeln räknas ut en gång per rad. Förut drog jämförelsen fram texten på nytt i varje
           jämförelse, och den texten byggs av uppslag som läser databasen - för ett "a" (som
           träffar allt) blev det tusentals genomsökningar per tangenttryckning. */
        final Map<T, Boolean> startarMed = new java.util.IdentityHashMap<T, Boolean>();
        for (T item : list) {
            startarMed.put(item, startsWithWordIgnoreCase(textExtractor.apply(item), queryLower));
        }

        Collections.sort(list, new java.util.Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                boolean p1 = Boolean.TRUE.equals(startarMed.get(o1));
                boolean p2 = Boolean.TRUE.equals(startarMed.get(o2));
                if (p1 && !p2) return -1;
                if (!p1 && p2) return 1;
                return 0;
            }
        });
    }

    public static boolean startsWithWordIgnoreCase(String source, String queryLower) {
        if (source == null || queryLower == null || queryLower.isEmpty()) {
            return false;
        }
        String s = source.trim().toLowerCase();
        if (s.startsWith(queryLower)) {
            return true;
        }
        for (String word : s.split("[\\s\\-_/.]+")) {
            if (word.startsWith(queryLower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String source, String queryLower) {
        if (source == null || queryLower == null) {
            return false;
        }
        return source.toLowerCase().contains(queryLower);
    }
}
