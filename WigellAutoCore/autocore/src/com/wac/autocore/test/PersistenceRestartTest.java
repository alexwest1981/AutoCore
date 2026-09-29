package com.wac.autocore.test;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.ServiceItem;
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

    public void testBookingWithMultipleServicesSurvivesRestart() throws SQLException {
        List<Vehicle> vehicles = garage.getVehicles();
        TestRunner.assertTrue(!vehicles.isEmpty(), "sample data should contain vehicles");
        int vehicleId = vehicles.get(0).getId();

        List<ServiceItem> services = garage.getServiceItems();
        TestRunner.assertTrue(services.size() >= 2, "sample data should contain at least two services");
        ServiceItem s1 = services.get(0);
        ServiceItem s2 = services.get(1);

        Booking booking = new Booking(vehicleId, LocalDate.now().plusDays(5), "Multi-service restart check");
        booking.addServiceItem(s1);
        booking.addServiceItem(s2);

        BookingRepository repo = new BookingRepository();
        repo.save(booking);
        int bookingId = booking.getId();
        TestRunner.assertTrue(bookingId > 0, "booking should receive a generated id from database");

        try {
            // Kontrollera att raderna i tabellen booking_service_items faktiskt finns (BEVIS för SCRUM-148)
            try (java.sql.Connection conn = com.wac.autocore.data.Db.getConnection();
                 java.sql.PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM booking_service_items WHERE booking_id = ?")) {
                ps.setInt(1, bookingId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    TestRunner.assertTrue(rs.next(), "query should return a count");
                    TestRunner.assertEquals(2, rs.getInt(1), "booking_service_items table should contain 2 rows for the booking");
                }
            }

            // Simulera omstart genom att läsa via ny repository-instans
            BookingRepository restartRepo = new BookingRepository();
            Booking reloaded = restartRepo.findById(bookingId);

            TestRunner.assertNotNull(reloaded, "booking should survive restart");
            TestRunner.assertEquals(2, reloaded.getServiceItems().size(), "booking should contain 2 service items after restart");
            TestRunner.assertEquals(s1.getId(), reloaded.getServiceItems().get(0).getId(), "first service id should match");
            TestRunner.assertEquals(s2.getId(), reloaded.getServiceItems().get(1).getId(), "second service id should match");
            TestRunner.assertEquals(s1.getEstimatedMinutes() + s2.getEstimatedMinutes(), reloaded.getTotalEstimatedMinutes(), "total estimated minutes should match sum of services");
            TestRunner.assertEquals(s1.getPrice() + s2.getPrice(), reloaded.getTotalEstimatedCost(), "total cost should match sum of services");
        } finally {
            repo.delete(bookingId);
        }
    }
}
