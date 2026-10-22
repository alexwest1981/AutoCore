package com.wac.autocore.standalone;

import java.time.LocalDate;
import java.time.LocalTime;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.wac.autocore.data.Db;
import com.wac.autocore.exception.AppException;
import com.wac.autocore.exception.RuleViolationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.OpeningHours;
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
import org.slf4j.LoggerFactory;

/**
 * Two of the garage's operations from a plain Java program, without Spring.
 */
public final class StandaloneWorkOrders {

    /** Far enough ahead that the seeded bookings cannot hold the mechanic at that time. */
    private static final int DAYS_AHEAD = 30;

    /** How many open days the program is willing to walk through before it gives up. */
    private static final int OPEN_DAYS_TO_TRY = 30;

    public static void main(String[] args) {

        configureLogging();

        try {
            run(args);

        } catch (AppException refused) {
            // The line in the log was written where the refusal was raised, with its cause. Here
            // only the words for the reader are left.
            String key = refused.getMessageKey();
            System.out.println("NEKAD: " + (key == null ? refused.getMessage() : I18n.get(key)));
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

        // Far enough ahead that the seeded bookings cannot hold the mechanic at that time.
        Booking booking = bookOnFirstOpenDay(garage, vehicle.getId(), mechanic, service,
                LocalDate.now().plusDays(DAYS_AHEAD));

        if (booking == null) {
            System.out.println("Ingen ledig dag hittades för en bokning.");
            return;
        }

        System.out.println("Bokade " + booking.getDate() + " klockan " + booking.getStartTime() + ".");

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

    /**
     * The first booking from {@code firstTry} onwards that lands on a day the workshop has open, or
     * null when {@link #OPEN_DAYS_TO_TRY} open days are all full. The day is chosen from
     * {@link OpeningHours} before the service is asked, so a closed day is never handed over and
     * never answered with a refusal. Each run takes the next free hour on the day, so the program
     * can run again on the same file.
     */
    static Booking bookOnFirstOpenDay(GarageSystem garage, int vehicleId, Mechanic mechanic,
                                      ServiceItem service, LocalDate firstTry) {

        LocalDate day = firstOpenDayOnOrAfter(firstTry);

        for (int tried = 0; tried < OPEN_DAYS_TO_TRY; tried++) {

            LocalTime start = LocalTime.of(8, 0).plusHours(hoursBooked(garage, vehicleId, day));

            try {
                return garage.createBooking(vehicleId, day, "Fristående körning", start,
                        mechanic.getId(), service.getId());
            } catch (RuleViolationException full) {
                // The day is open but has no hour left for the job. The next open day gets the try.
                day = firstOpenDayOnOrAfter(day.plusDays(1));
            }
        }

        return null;
    }

    /** The day itself when it is a weekday, otherwise the Monday that follows it. */
    static LocalDate firstOpenDayOnOrAfter(LocalDate day) {

        LocalDate candidate = day;

        while (!OpeningHours.isOpenOn(candidate)) {
            candidate = candidate.plusDays(1);
        }

        return candidate;
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

    /** Loads the standalone logging setup, since Spring is not here to read logback-spring.xml. */
    private static void configureLogging() {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();

        try {
            configurator.doConfigure(StandaloneWorkOrders.class.getResource("/logback-standalone.xml"));

        } catch (Exception cannotRead) {
            System.out.println("Kunde inte läsa logginställningen: " + cannotRead.getMessage());
        }
    }
}
