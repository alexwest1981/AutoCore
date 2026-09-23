package com.wac.autocore.service;

import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar fordonsadministration i AutoCore.
 *
 * Ansvarar för:
 * - Validering av fordonets ägare (att kund existerar)
 * - Skapande av nya fordon
 * - Uppslag av fordon baserat på ID
 * - Tillhandahållande av vy över fordonsregistret
 */
public class VehicleService {

    private final CustomerService customerService;
    private final VehicleRepository vehicleRepository = new VehicleRepository();

    public VehicleService() {
        this(new CustomerService());
    }

    public VehicleService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public List<Vehicle> getAll() {
        try {
            return vehicleRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read vehicles: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Vehicle findById(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read vehicle " + id + ": " + e.getMessage());
            return null;
        }
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {

        Customer customer = customerService.findById(customerId);

        if (customer == null) {
            System.out.println("Customer with ID " + customerId + " does not exist.");
            return null;
        }

        Vehicle vehicle = new Vehicle(0, registrationNumber, brand, model, year, customerId);

        try {
            vehicleRepository.save(vehicle);
        } catch (SQLException e) {
            System.out.println("Could not save vehicle: " + e.getMessage());
            return null;
        }

        System.out.println("Vehicle created successfully.");
        System.out.println(vehicle);

        return vehicle;
    }
}
