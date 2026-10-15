package com.wac.autocore.test;

import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.NotFoundException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * A12: an invalid customer number is a rule violation with a message worth reading, and a
 * database that stops answering is another kind of error. Neither a failed read nor a failed
 * write may end as a quiet null. The proof goes through the service layer only, and runs against
 * a copy of the database, so the real one is never touched.
 *
 * Run from the repository root: the database path is the first argument, or data/autocore.db.
 */
public final class ErrorBoundaryProof {

    private static final int MISSING_CUSTOMER = 999999;

    private ErrorBoundaryProof() {}

    public static void main(String[] args) throws Exception {
        File original = new File(args.length > 0 ? args[0] : "data/autocore.db");
        File working = File.createTempFile("error-boundary-", ".db");
        working.deleteOnExit();
        Files.copy(original.toPath(), working.toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("A12 · ett ogiltigt kundnummer är inte ett tekniskt fel");
        System.out.println("register: " + working.getAbsolutePath() + " (kopia av " + original + ")");
        System.out.println();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(working.getAbsolutePath()));

        boolean refused = report(garage, MISSING_CUSTOMER);
        System.out.println();

        int existing = firstCustomerId(garage);
        boolean accepted = accepted(garage, existing);
        System.out.println();

        stopAnswering(working);
        System.out.println("3. samma anrop, men registret svarar inte längre");
        String[] failure = failure(garage, MISSING_CUSTOMER);
        boolean technical = "DataAccessException".equals(failure[0]);
        System.out.println("   " + failure[0] + ": " + failure[1]);
        System.out.println();

        System.out.println("4. en skrivning som inte går igenom");
        String[] write = failureToWrite(garage);
        boolean signalled = "DataAccessException".equals(write[0]);
        System.out.println("   " + write[0] + ": " + write[1]);
        System.out.println();

        System.out.println("regelbrottet är ett eget fel med en text att läsa: " + refused);
        System.out.println("ett nummer som finns går igenom kontrollen:        " + accepted);
        System.out.println("databasen som inte svarar ger ett annat fel:        " + technical);
        System.out.println("en skrivning som misslyckas ger ett tydligt fel:   " + signalled);
        System.exit(refused && accepted && technical && signalled ? 0 : 1);
    }

    /** A customer number that is not in the register. */
    private static boolean report(GarageSystem garage, int customerId) {
        System.out.println("1. kundnummer som inte finns: " + customerId);
        String[] failure = failure(garage, customerId);
        System.out.println("   " + failure[0] + ": " + failure[1]);
        return "NotFoundException".equals(failure[0]) && failure[1] != null && !failure[1].isEmpty();
    }

    /** The same call with a number that is in the register, so the check is not just a wall. */
    private static boolean accepted(GarageSystem garage, int customerId) {
        System.out.println("2. samma anrop med ett nummer som finns: " + customerId);
        try {
            garage.createVehicle("ZZZ999", "Volvo", "V70", 2012, customerId);
            return true;
        } catch (Exception e) {
            System.out.println("   " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private static String[] failure(GarageSystem garage, int customerId) {
        try {
            garage.createVehicle("ZZZ998", "Volvo", "V70", 2012, customerId);
            return new String[]{"Ingen", "anropet gick igenom"};
        } catch (Exception e) {
            return new String[]{e.getClass().getSimpleName(), e.getMessage()};
        }
    }

    private static int firstCustomerId(GarageSystem garage) {
        List<Customer> customers = garage.getCustomers();
        if (customers.isEmpty()) {
            throw new IllegalStateException("Registret har ingen kund att pröva med.");
        }
        return customers.get(0).getId();
    }

    /** A write that cannot go through: a null here would hide a failure as a normal answer. */
    private static String[] failureToWrite(GarageSystem garage) {
        try {
            garage.createCustomer("Proof Kund", "070-1234567", "proof@example.com");
            return new String[]{"Ingen", "anropet gick igenom"};
        } catch (Exception e) {
            return new String[]{e.getClass().getSimpleName(), e.getMessage()};
        }
    }

    /** The database stops answering: the table the lookup needs is no longer there. */
    private static void stopAnswering(File database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE customers RENAME TO customers_gone");
        }
    }
}
