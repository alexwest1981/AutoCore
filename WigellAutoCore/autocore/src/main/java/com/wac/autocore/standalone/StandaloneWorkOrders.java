package com.wac.autocore.standalone;

import java.time.LocalDate;
import java.time.LocalTime;

import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.exception.RuleViolationException;
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
import com.wac.autocore.service.GuardedGarageSystem;
import com.wac.autocore.ui.i18n.I18n;

/**
 * Two of the garage's operations from a plain Java program, without Spring.
 */
public final class StandaloneWorkOrders {

    public static void main(String[] args) {

        try {
            run(args);

        } catch (AppException refused) {
            System.out.println("NEKAD: " + I18n.get(refused.getMessageKey()));
        }
    }

    /** The ordinary run: a database, a booking, and a work order carried through to done. */
    private static void run(String[] args) {

        String databasePath = args.length > 0 ? args[0] : Db.DEFAULT_PATH;

        Db db = new Db(databasePath);
        db.ensureReady();

        // Nobody logs in here, so the program fills the same holder the window fills at login. The
        // second argument decides whether it does, so a run without identity can be shown too.
        IdentityHolder identity = new IdentityHolder();

        if (args.length < 2 || !"utan".equals(args[1])) {
            identity.set(new Identity("fristaende", Role.STAFF));
        }

        GarageSystem garage = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity);

        Mechanic mechanic = firstAvailableMechanic(garage);
        ServiceItem service = garage.getServiceItems().get(0);

        Customer customer = customerNamed(garage, "Fristående Kund");
        Vehicle vehicle = vehicleOf(garage, customer, "STD 001");

        // Far enough ahead that the seeded bookings cannot hold the mechanic at that time. The
        // workshop is closed on some of those days, and then the booking is refused, so the program
        // walks on to the first day that takes it. Each run takes the next free hour on that day, so
        // it can run again on the same file.
        Booking booking = null;
        for (int daysAhead = 30; daysAhead < 60 && booking == null; daysAhead++) {

            LocalDate day = LocalDate.now().plusDays(daysAhead);
            LocalTime start = LocalTime.of(8, 0).plusHours(hoursBooked(garage, vehicle.getId(), day));

            try {
                booking = garage.createBooking(vehicle.getId(), day,
                        "Fristående körning", start, mechanic.getId(), service.getId());
            } catch (RuleViolationException closedOrFull) {
                // A closed day, or a day with no free hour left. The next one gets the same try.
            }
        }

        if (booking == null) {
            System.out.println("Ingen ledig dag hittades för en bokning.");
            return;
        }

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
