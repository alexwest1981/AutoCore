package com.wac.autocore.service;

import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;

import java.util.List;

/** Customers: reading, lookup and validation. */
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAll() {
        try {
            return customerRepository.findAll();
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read customers", e);
        }
    }

    public Customer findById(int id) {
        try {
            return customerRepository.findById(id);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not read customer " + id, e);
        }
    }

    public Customer createCustomer(String name, String phone, String email) {
        refuseUnlessStorable(name, phone, email);

        Customer customer = new Customer(0, name, phone, email);

        try {
            customerRepository.save(customer);
        } catch (DataAccessException e) {
            throw new DataAccessException("Could not save customer", e);
        }

        System.out.println("Customer created successfully.");
        System.out.println(customer);

        return customer;
    }

    /** Saves the change through the same rule as the creation. */
    public void updateCustomer(Customer customer) {
        if (customer == null) {
            return;
        }
        refuseUnlessStorable(customer.getName(), customer.getPhone(), customer.getEmail());
        customerRepository.save(customer);
    }

    /** The rule sits here and not in the form, so every path to a customer edit is checked. */
    private static void refuseUnlessStorable(String name, String phone, String email) {
        String problem = Customer.validationProblem(name, phone, email);
        if (problem != null) {
            throw new ValidationException(problem);
        }
    }
}
