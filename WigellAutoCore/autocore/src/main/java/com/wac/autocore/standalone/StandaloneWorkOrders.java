package com.wac.autocore.standalone;

import java.time.LocalDate;
import java.time.LocalTime;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.GarageSystem;

/**
 * Two of the garage's operations from a plain Java program, without Spring.
 */
public final class StandaloneWorkOrders {

    public static void main(String[] args) {

        String databasePath = args.length > 0 ? args[0] : Db.DEFAULT_PATH;

        Db db = new Db(databasePath);
        db.ensureReady();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(db));

        // Nobody logs in here, so the program says who is running it and the core reads the same
        // holder the window fills at login.
        IdentityHolder identity = new IdentityHolder();
        identity.set(new Identity("fristaende", Role.STAFF));

        Mechanic mechanic = firstAvailableMechanic(garage);
        ServiceItem service = garage.getServiceItems().get(0);

        Customer customer = customerNamed(garage, "Fristående Kund");
        Vehicle vehicle = vehicleOf(garage, customer, "STD 001");

        // Far enough ahead that the seeded bookings cannot hold the mechanic at that time. Each run
        // takes the next free hour on the same day, so the program can run again on the same file.
        LocalDate day = LocalDate.now().plusDays(30);
        LocalTime start = LocalTime.of(8, 0).plusHours(hoursBooked(garage, vehicle.getId(), day));

        Booking booking = garage.createBooking(vehicle.getId(), day,
                "Fristående körning", start, mechanic.getId(), service.getId());

        WorkOrder order = garage.createWorkOrder(booking.getId(), mechanic.getId());

        if (order == null) {
            System.out.println("Ingen arbetsorder kunde skapas för bokning " + booking.getId() + ".");
            return;
        }

        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());
        garage.completeWorkOrder(order.getId());

        WorkOrder finished = garage.findWorkOrder(order.getId());

        System.out.println("Skapade arbetsorder " + order.getId() + " för bokning " + booking.getId() + ".");
        System.out.println("Slutförde samma order, status är nu " + finished.getStatus() + ".");
        System.out.println("STANDALONE_OK");
    }

    private static Mechanic firstAvailableMechanic(GarageSystem garage) {

        for (Mechanic mechanic : garage.getMechanics()) {

            if (mechanic.isAvailable()) {
                return mechanic;
            }
        }

        throw new IllegalStateException("Alla mekaniker är upptagna, kör mot en ny databasfil.");
    }

    /** The program runs more than once against the same file, so it reuses what it made last time. */
    private static Customer customerNamed(GarageSystem garage, String name) {

        for (Customer customer : garage.getCustomers()) {

            if (name.equals(customer.getName())) {
                return customer;
            }
        }

        return garage.createCustomer(name, "070-0000000", "fristaende@example.se");
    }

    private static Vehicle vehicleOf(GarageSystem garage, Customer customer, String registrationNumber) {

        for (Vehicle vehicle : garage.getVehicles()) {

            if (registrationNumber.equals(vehicle.getRegistrationNumber())
                    && vehicle.getCustomerId() == customer.getId()) {
                return vehicle;
            }
        }

        return garage.createVehicle(registrationNumber, "Volvo", "V70", 2016, customer.getId());
    }

    private static int hoursBooked(GarageSystem garage, int vehicleId, LocalDate day) {

        int hours = 0;

        for (Booking booking : garage.getBookings()) {
            if (booking.getVehicleId() == vehicleId && day.equals(booking.getDate())) {
                hours++;
            }
        }

        return hours;
    }
}
