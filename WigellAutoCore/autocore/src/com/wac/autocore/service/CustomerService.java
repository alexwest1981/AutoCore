package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;

import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar kundadministration i AutoCore.
 *
 * Ansvarar för:
 * - Skapande och ID-generering för nya kunder
 * - Uppslag av kund baserat på ID
 * - Tillhandahållande av oföränderlig vy över kundregistret
 */
public class CustomerService {

    public List<Customer> getAll() {
        return Collections.unmodifiableList(Database.getCustomers());
    }

    public Customer findById(int id) {
        for (Customer customer : Database.getCustomers()) {
            if (customer.getId() == id) {
                return customer;
            }
        }
        return null;
    }

    public Customer createCustomer(String name, String phone, String email) {
        int id = Database.getCustomers().size() + 1;

        Customer customer = new Customer(id, name, phone, email);
        Database.getCustomers().add(customer);

        System.out.println("Customer created successfully.");
        System.out.println(customer);

        return customer;
    }
}
