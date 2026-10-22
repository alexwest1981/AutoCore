package com.wac.autocore.proof;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * A12: an invalid customer number is a rule violation with a message worth reading, and a
 * database that stops answering is another kind of error. Neither a failed read nor a failed
 * write may end as a quiet null. The proof goes through the service layer only, and builds its
 * own database, so the real one is never touched and the proof runs on any machine.
 *
 * No arguments: the database is the proof's own, built from the schema files on the classpath.
 */
public final class ErrorBoundaryProof {

    private static final int MISSING_CUSTOMER = 999999;

    /**
     * Numbers that cannot be a customer: one that is simply not in the register, and the ones the
     * model uses to mean "none at all". Every one of them is a rule violation, never a technical
     * error, and none of them may leave a row behind.
     */
    private static final int[] NO_CUSTOMER = {MISSING_CUSTOMER, 0, -1, Integer.MAX_VALUE};

    private ErrorBoundaryProof() {}

    public static void main(String[] args) throws Exception {
        File working = File.createTempFile("error-boundary-", ".db");
        working.deleteOnExit();
        // Nothing to copy: the program builds the database, so a fresh checkout runs this the same
        // way as a machine that has used the program for a while.
        working.delete();

        System.out.println("A12 · ett ogiltigt kundnummer är inte ett tekniskt fel");
        System.out.println("register: " + working.getAbsolutePath() + " (byggt av provet)");
        System.out.println();

        GarageSystem garage = new GarageSystem(new SqliteRepositoryFactory(new Db(working.getAbsolutePath())));

        boolean refused = report(garage);
        System.out.println();

        int existing = firstCustomerId(garage);
        boolean accepted = accepted(garage, existing);
        System.out.println();

        stopAnswering(working);
        System.out.println("3. samma anrop, men registret svarar inte längre");
        String[] failure = failure(garage, "ZZZ998", MISSING_CUSTOMER);
        boolean technical = "DataAccessException".equals(failure[0]);
        System.out.println("   " + failure[0] + ": " + failure[1]);
        System.out.println();

        System.out.println("4. en skrivning som inte går igenom");
        String[] write = failureToWrite(garage);
        boolean signalled = "DataAccessException".equals(write[0]) && readable(write[1]);
        System.out.println("   " + write[0] + ": " + write[1]);
        System.out.println();

        System.out.println("regelbrottet är ett eget fel med en text att läsa: " + refused);
        System.out.println("ett nummer som finns går igenom kontrollen:        " + accepted);
        System.out.println("databasen som inte svarar ger ett annat fel:        " + technical);
        System.out.println("en skrivning som misslyckas ger ett tydligt fel:   " + signalled);
        System.exit(refused && accepted && technical && signalled ? 0 : 1);
    }

    /**
     * Numbers that cannot be a customer. All of them have to be refused the same way, and the
     * ones the model reads as "none at all" are the interesting ones: a zero that slips through
     * is written as a reference to nobody instead of being turned down.
     */
    private static boolean report(GarageSystem garage) {
        System.out.println("1. kundnummer som inte kan vara en kund");
        boolean all = true;
        for (int customerId : NO_CUSTOMER) {
            String[] failure = failure(garage, "ZZZ" + Math.abs(customerId), customerId);
            System.out.println("   " + customerId + " -> " + failure[0] + ": " + failure[1]);
            all = all && "NotFoundException".equals(failure[0]) && readable(failure[1]);
        }
        return all;
    }

    /**
     * A message that says something. After A3 a rule violation carries a key and A5 lets the ui
     * translate it, so what is required here is a text to read or to translate — not words. The
     * check is the same on both sides of that change, which is why it lives here.
     */
    private static boolean readable(String message) {
        return message != null && !message.isEmpty();
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

    private static String[] failure(GarageSystem garage, String registrationNumber, int customerId) {
        try {
            garage.createVehicle(registrationNumber, "Volvo", "V70", 2012, customerId);
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
