package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;

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

    public List<Booking> getAll() {
        return Collections.unmodifiableList(Database.getBookings());
    }

    public Booking findById(int id) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == id) {
                return booking;
            }
        }
        return null;
    }

    public Booking createBooking(int vehicleId, LocalDate date, String description) {
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            System.out.println("Vehicle with ID " + vehicleId + " does not exist.");
            return null;
        }

        int id = Database.getBookings().size() + 1;
        Booking booking = new Booking(id, vehicleId, date, description);

        Database.getBookings().add(booking);

        System.out.println("Booking created successfully.");
        System.out.println(booking);

        return booking;
    }

    private Vehicle findVehicle(int id) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == id) {
                return vehicle;
            }
        }
        return null;
    }
}
