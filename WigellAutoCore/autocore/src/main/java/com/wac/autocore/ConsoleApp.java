package com.wac.autocore;

import com.wac.autocore.exception.AppException;
import com.wac.autocore.config.DatabasePath;
import com.wac.autocore.data.Db;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.PaymentType;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.repository.UserRepository;
import com.wac.autocore.security.Identity;
import com.wac.autocore.security.IdentityHolder;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.GuardedGarageSystem;
import com.wac.autocore.service.LoginService;
import com.wac.autocore.ui.ConsolePrinter;
import com.wac.autocore.ui.i18n.I18n;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleApp {

    private static final Scanner scanner = new Scanner(System.in);

    // Set in main: the file may be given as an argument, and nothing may touch the
    // application's own database before that is known.
    private static GarageSystem garageSystem;

    public static void main(String[] args) {

        configureLogging();

        Db db = new Db(DatabasePath.resolve(args));

        // This is a second way into the database, so it has to stand behind the same guard as the
        // window. Without a sign-in it went straight to GarageSystem and could read and write
        // everything, which is the way round the access control that the order says must not exist.
        IdentityHolder identity = new IdentityHolder();

        if (!signIn(db, identity)) {
            System.out.println("Ingen inloggning, programmet avslutas.");
            System.exit(1);
        }

        garageSystem = new GuardedGarageSystem(new SqliteRepositoryFactory(db), identity, new ConsolePrinter());

        boolean running = true;

        printHeader();

        while (running) {

            printMenu();

            int choice = readInt("Select option: ");

            switch (choice) {

                case 1:
                    garageSystem.showCustomers();
                    break;

                case 2:
                    createCustomer();
                    break;

                case 3:
                    garageSystem.showVehicles();
                    break;

                case 4:
                    createVehicle();
                    break;

                case 5:
                    garageSystem.showBookings();
                    break;

                case 6:
                    createBooking();
                    break;

                case 7:
                    garageSystem.showServiceItems();
                    break;

                case 8:
                    garageSystem.showMechanics();
                    break;

                case 9:
                    garageSystem.showWorkOrders();
                    break;

                case 10:
                    createWorkOrder();
                    break;

                case 11:
                    startWorkOrder();
                    break;

                case 12:
                    completeWorkOrder();
                    break;

                case 13:
                    garageSystem.showInvoices();
                    break;

                case 14:
                    createInvoice();
                    break;

                case 15:
                    garageSystem.showPayments();
                    break;

                case 16:
                    processPayment();
                    break;

                case 0:
                    running = false;
                    break;

                default:
                    System.out.println("Unknown menu option.");
                    break;
            }
        }

        System.out.println();
        System.out.println("Shutting down Wigell AutoCore...");
        System.out.println("Goodbye!");

        scanner.close();
    }

    /**
     * The same sign-in the window uses: a name and a password become an identity in the holder the
     * guard reads. Three tries, then no. The password is read without echo when the terminal can.
     */
    private static boolean signIn(Db db, IdentityHolder identity) {

        LoginService login = new LoginService(new UserRepository(db));
        Console terminal = System.console();

        for (int attempt = 1; attempt <= 3; attempt++) {

            System.out.print("Användare: ");
            String username = readLine();
            if (username == null) {
                return false;
            }

            String password;
            if (terminal != null) {
                char[] typed = terminal.readPassword("Lösenord: ");
                System.out.println();
                password = typed == null ? "" : new String(typed);
            } else {
                System.out.print("Lösenord: ");
                password = readLine();
                if (password == null) {
                    return false;
                }
            }

            Optional<Identity> signedIn = login.signIn(username.trim(), password);

            if (signedIn.isPresent()) {
                identity.set(signedIn.get());
                return true;
            }
            System.out.println("Fel användare eller lösenord.");
        }

        return false;
    }

    /** The next line, or null when the input has ended. A closed stdin is not a crash. */
    private static String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    /** Loads the standalone logging setup, since Spring is not here to read logback-spring.xml. */
    private static void configureLogging() {

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();

        try {
            configurator.doConfigure(ConsoleApp.class.getResource("/logback-standalone.xml"));

        } catch (Exception cannotRead) {
            System.out.println("Kunde inte läsa logginställningen: " + cannotRead.getMessage());
        }
    }

    private static void printHeader() {
        System.out.println("===========================================");
        System.out.println("            WIGELL AUTOCORE");
        System.out.println("         A Wigell Group Company");
        System.out.println("===========================================");
        System.out.println();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("=============== MAIN MENU =================");
        System.out.println(" 1. Show customers");
        System.out.println(" 2. Create customer");
        System.out.println(" 3. Show vehicles");
        System.out.println(" 4. Create vehicle");
        System.out.println(" 5. Show bookings");
        System.out.println(" 6. Create booking");
        System.out.println(" 7. Show services");
        System.out.println(" 8. Show mechanics");
        System.out.println(" 9. Show work orders");
        System.out.println("10. Create work order");
        System.out.println("11. Start work order");
        System.out.println("12. Complete work order");
        System.out.println("13. Show invoices");
        System.out.println("14. Create invoice");
        System.out.println("15. Show payments");
        System.out.println("16. Process payment");
        System.out.println(" 0. Exit");
        System.out.println("===========================================");
    }

    /**
     * A refusal is said the way the window says it: the sentence the key names, not the key.
     * "Customer was not saved: required" was the internal word for a missing value; the reader is
     * owed the same English the window shows. The line in the log was written where the error was
     * raised, so the console has nothing to log of its own.
     */
    private static void say(String what, AppException rejected) {
        String key = rejected.getMessageKey();
        if (key != null) {
            System.out.println(what + ": " + I18n.get(key));
            return;
        }
        // No key: the message is already a sentence, and it is the only thing to say.
        System.out.println(what + ": " + rejected.getMessage());
    }

    private static void createCustomer() {

        System.out.println();
        System.out.println("=== CREATE CUSTOMER ===");

        System.out.print("Name: ");
        String name = scanner.nextLine();

        System.out.print("Phone: ");
        String phone = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        try {
            Customer created = garageSystem.createCustomer(name, phone, email);

            if (created != null) {
                System.out.println("Customer created successfully.");
            }
        } catch (AppException rejected) {
            say("Customer was not saved", rejected);
        }
    }

    private static void createVehicle() {

        System.out.println();
        System.out.println("=== CREATE VEHICLE ===");

        garageSystem.showCustomers();

        int customerId = readInt("Customer ID: ");

        System.out.print("Registration number: ");
        String registrationNumber = scanner.nextLine();

        System.out.print("Brand: ");
        String brand = scanner.nextLine();

        System.out.print("Model: ");
        String model = scanner.nextLine();

        int year = readInt("Year: ");

        try {
            Vehicle created = garageSystem.createVehicle(
                    registrationNumber,
                    brand,
                    model,
                    year,
                    customerId
            );

            if (created != null) {
                System.out.println("Vehicle created successfully.");
            }
        } catch (AppException rejected) {
            say("Vehicle was not saved", rejected);
        }
    }

    private static void createBooking() {

        System.out.println();
        System.out.println("=== CREATE BOOKING ===");

        garageSystem.showVehicles();

        int vehicleId = readInt("Vehicle ID: ");

        LocalDate date = readDate("Date (YYYY-MM-DD): ");

        System.out.print("Description: ");
        String description = scanner.nextLine();

        garageSystem.createBooking(
                vehicleId,
                date,
                description
        );
    }

    private static void createWorkOrder() {

        System.out.println();
        System.out.println("=== CREATE WORK ORDER ===");

        garageSystem.showBookings();
        int bookingId = readInt("Booking ID: ");

        garageSystem.showMechanics();
        int mechanicId = readInt("Mechanic ID: ");

        // The work order gets the booking's services.
        try {
            garageSystem.createWorkOrder(bookingId, mechanicId);
        } catch (AppException rejected) {
            say("Work order was not created", rejected);
        }
    }

    private static void startWorkOrder() {

        System.out.println();
        System.out.println("=== START WORK ORDER ===");

        garageSystem.showWorkOrders();

        int workOrderId = readInt("Work order ID: ");

        try {
            garageSystem.startWorkOrder(workOrderId);
        } catch (AppException rejected) {
            say("Work order was not started", rejected);
        }
    }

    private static void completeWorkOrder() {

        System.out.println();
        System.out.println("=== COMPLETE WORK ORDER ===");

        garageSystem.showWorkOrders();

        int workOrderId = readInt("Work order ID: ");

        try {
            garageSystem.completeWorkOrder(workOrderId);
        } catch (AppException rejected) {
            say("Work order was not completed", rejected);
        }
    }

    private static void createInvoice() {

        System.out.println();
        System.out.println("=== CREATE INVOICE ===");

        garageSystem.showWorkOrders();

        int workOrderId = readInt("Work order ID: ");

        System.out.print("Discount code (leave empty for none): ");
        String discountCode = scanner.nextLine();

        Invoice created = garageSystem.createInvoice(
                workOrderId,
                discountCode
        );

        if (created != null) {
            System.out.println("Invoice created successfully.");
        }
    }

    private static void processPayment() {

        System.out.println();
        System.out.println("=== PROCESS PAYMENT ===");

        garageSystem.showInvoices();

        int invoiceId = readInt("Invoice ID: ");

        System.out.println();
        System.out.println("Available payment types:");
        for (PaymentType type : PaymentType.values()) {
            System.out.println(type.name());
        }

        System.out.print("Payment type: ");
        PaymentType paymentType = PaymentType.fromText(scanner.nextLine());

        Payment created = garageSystem.processPayment(
                invoiceId,
                paymentType
        );

        if (created != null) {
            System.out.println("Payment completed successfully.");
        }
    }

    private static int readInt(String message) {

        while (true) {

            System.out.print(message);

            String input = scanner.nextLine();

            try {
                return Integer.parseInt(input);

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static LocalDate readDate(String message) {

        while (true) {

            System.out.print(message);

            String input = scanner.nextLine();

            try {
                return LocalDate.parse(input);

            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use format YYYY-MM-DD.");
            }
        }
    }
}