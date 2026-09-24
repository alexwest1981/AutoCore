package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final ServiceItemRepository serviceItemRepository = new ServiceItemRepository();

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
            System.out.println("Could not read booking: " + e.getMessage());
            return null;
        }
    }

    public Booking createBooking(int vehicleId, LocalDate date, String description) throws SQLException {
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            System.out.println("Vehicle with ID " + vehicleId + " does not exist.");
            return null;
        }


        Booking booking = new Booking(vehicleId, date, description);
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

    public Booking createBooking(int vehicleId, LocalDate date, String description, LocalTime startTime, int mechanicId, int serviceItemId) throws SQLException {
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            System.out.println("Vehicle with ID " + vehicleId + " does not exist.");
            return null;
        }
        ServiceItem serviceItem = serviceItemRepository.findAll().stream()
                .filter(item -> item.getId() == serviceItemId)
                .findFirst()
                .orElse(null);
        if (serviceItem == null) {
            System.out.println("Service item with ID " + serviceItemId + " does not exist ");
            return null;
        }
        //Beräkna endTime automatiskt utifrån startTime och estimatedMinutes
        LocalTime endTime = startTime.plusMinutes(serviceItem.getEstimatedMinutes());

        Booking booking = new Booking(vehicleId, date, description, startTime, endTime, mechanicId, serviceItemId);

        bookingRepository.save(booking);

        System.out.println("Booking created successfully.");
        System.out.println(booking);

        return booking;
    }

    private Vehicle findVehicle(int id) {
        try {
            return vehicleRepository.findById(id);
        } catch (SQLException e) {
            System.out.println("Could not read vehicle: " + e.getMessage());
            return null;
        }
    }
}
