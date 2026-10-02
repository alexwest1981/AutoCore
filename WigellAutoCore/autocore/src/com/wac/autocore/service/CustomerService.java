package com.wac.autocore.service;

import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar kundadministration i AutoCore.
 *
 * Ansvarar för:
 * - Skapande av nya kunder
 * - Uppslag av kund baserat på ID
 * - Tillhandahållande av vy över kundregistret
 */
public class CustomerService {

    private final CustomerRepository customerRepository = new CustomerRepository();

    public List<Customer> getAll() {
        try {
            return customerRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read customers: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Customer findById(int id) {
        try {
            return customerRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read customer " + id + ": " + e.getMessage());
            return null;
        }
    }

    public Customer createCustomer(String name, String phone, String email) {
        refuseUnlessStorable(name, phone, email);

        Customer customer = new Customer(0, name, phone, email);

        try {
            customerRepository.save(customer);
        } catch (SQLException e) {
            System.out.println("Could not save customer: " + e.getMessage());
            return null;
        }

        System.out.println("Customer created successfully.");
        System.out.println(customer);

        return customer;
    }

    /**
     * Sparar en ändrad kund. Ändringen går genom samma regel som skapandet, annars kunde en ogiltig
     * rad skrivas in via redigera-dialogen i stället för via skapa-dialogen.
     */
    public void updateCustomer(Customer customer) throws SQLException {
        if (customer == null) {
            return;
        }
        refuseUnlessStorable(customer.getName(), customer.getPhone(), customer.getEmail());
        customerRepository.save(customer);
    }

    /**
     * The rule is enforced here, not in the form, so every writer of a customer row is covered.
     * The message names the broken rule; the form turns it into text in the active language.
     */
    private static void refuseUnlessStorable(String name, String phone, String email) {
        String problem = Customer.validationProblem(name, phone, email);
        if (problem != null) {
            throw new IllegalArgumentException("Customer data rejected: " + problem);
        }
    }
}
