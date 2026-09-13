package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;

import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar fordonsadministration i AutoCore.
 *
 * Ansvarar för:
 * - Validering av fordonets ägare (att kund existerar)
 * - Skapande och ID-generering för nya fordon
 * - Uppslag av fordon baserat på ID
 * - Tillhandahållande av oföränderlig vy över fordonsregistret
 */
public class VehicleService {

    private final CustomerService customerService;

    public VehicleService() {
        this(new CustomerService());
    }

    public VehicleService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public List<Vehicle> getAll() {
        return Collections.unmodifiableList(Database.getVehicles());
    }

    public Vehicle findById(int id) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == id) {
                return vehicle;
            }
        }
        return null;
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

        int id = Database.getVehicles().size() + 1;

        Vehicle vehicle = new Vehicle(
                id,
                registrationNumber,
                brand,
                model,
                year,
                customerId
        );

        Database.getVehicles().add(vehicle);

        System.out.println("Vehicle created successfully.");
        System.out.println(vehicle);

        return vehicle;
    }
}
