package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.service.GarageSystem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class PersistenceRestartTest {

    private final GarageSystem garage = new GarageSystem();

    public void testCustomerSurvivesNewRepository() throws SQLException {
        Customer created = garage.createCustomer("Persistence Test Customer", "070-1234567", "persistence@test.se");
        TestRunner.assertNotNull(created, "the customer should be created");
        TestRunner.assertTrue(created.getId() > 0, "the database should assign the customer an id");

        CustomerRepository newConnection = new CustomerRepository();
        Customer readBack = newConnection.findById(created.getId());

        TestRunner.assertNotNull(readBack, "the customer should be readable through a new connection");
        TestRunner.assertEquals("Persistence Test Customer", readBack.getName(), "the name should still be stored");
        TestRunner.assertEquals("070-1234567", readBack.getPhone(), "the phone number should still be stored");
        TestRunner.assertEquals("persistence@test.se", readBack.getEmail(), "the email address should still be stored");

        newConnection.delete(created.getId());
    }

    public void testBookingSurvivesNewRepository() throws SQLException {
        List<Vehicle> vehicles = garage.getVehicles();
        TestRunner.assertTrue(!vehicles.isEmpty(), "the sample data should contain at least one vehicle");
        int vehicleId = vehicles.get(0).getId();

        LocalDate date = LocalDate.now();
        Booking created = garage.createBooking(vehicleId, date, "Persistence check");
        TestRunner.assertNotNull(created, "the booking should be created");
        TestRunner.assertTrue(created.getId() > 0, "the database should assign the booking an id");

        BookingRepository newConnection = new BookingRepository();
        Booking readBack = newConnection.findById(created.getId());

        TestRunner.assertNotNull(readBack, "the booking should be readable through a new connection");
        TestRunner.assertEquals(vehicleId, readBack.getVehicleId(), "the vehicle should still be stored");
        TestRunner.assertEquals(date, readBack.getDate(), "the date should still be stored");
        TestRunner.assertEquals("Persistence check", readBack.getDescription(), "the description should still be stored");

        newConnection.delete(created.getId());
    }

    public void testSeedDoesNotDuplicateOnSecondRead() {
        int firstCount = garage.getCustomers().size();

        GarageSystem secondStart = new GarageSystem();
        int secondCount = secondStart.getCustomers().size();

        TestRunner.assertTrue(firstCount > 0, "the sample data should exist in the database");
        TestRunner.assertEquals(firstCount, secondCount, "a new start should not insert the sample data again");
    }
}
