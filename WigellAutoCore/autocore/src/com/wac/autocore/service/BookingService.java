package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Service som hanterar tidsbokningar för service och reparation i verkstaden.
 *
 * Ansvarar för:
 * - Validering av fordon
 * - Skapande och registrering av nya bokningar
 * - Uppslagning av bokningar baserat på ID
 */
public class BookingService {

    private final BookingRepository bookingRepository = new BookingRepository();
    private final VehicleRepository vehicleRepository = new VehicleRepository();

    public List<Booking> getAll() {
        try {
            return bookingRepository.findAll();
        } catch (SQLException e) {
            System.out.println("Could not read bookings: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Booking findById(int id) {
        try {
            return bookingRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read booking " + id + ": " + e.getMessage());
            return null;
        }
    }

    public Booking createBooking(int vehicleId, LocalDate date, String description) {
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            System.out.println("Vehicle with ID " + vehicleId + " does not exist.");
            return null;
        }

        Booking booking = new Booking(0, vehicleId, date, description);

        try {
            bookingRepository.save(booking);
        } catch (SQLException e) {
            System.out.println("Could not save booking: " + e.getMessage());
            return null;
        }

        System.out.println("Booking created successfully.");
        System.out.println(booking);

        return booking;
    }

    private Vehicle findVehicle(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read vehicle " + id + ": " + e.getMessage());
            return null;
        }
    }
}
