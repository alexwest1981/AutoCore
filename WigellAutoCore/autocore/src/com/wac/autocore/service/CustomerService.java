package com.wac.autocore.service;

import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/** Kunder: läsning, uppslag och validering. */
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

/** Sparar ändringen genom samma regel som skapandet. */
    public void updateCustomer(Customer customer) throws SQLException {
        if (customer == null) {
            return;
        }
        refuseUnlessStorable(customer.getName(), customer.getPhone(), customer.getEmail());
        customerRepository.save(customer);
    }

/** Regeln ligger här och inte i formuläret, så varje väg till en kundradering kontrolleras. */
    private static void refuseUnlessStorable(String name, String phone, String email) {
        String problem = Customer.validationProblem(name, phone, email);
        if (problem != null) {
            throw new IllegalArgumentException("Customer data rejected: " + problem);
        }
    }
}
