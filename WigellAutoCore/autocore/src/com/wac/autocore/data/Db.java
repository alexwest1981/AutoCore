package com.wac.autocore.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {

    private static final String DATABASE_PATH = "data/autocore.db";

    private static boolean ready = false;

    public static Connection getConnection() throws SQLException {
        File dataDirectory = new File("data");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
    }

    public static void ensureReady() {
        if (ready) {
            return;
        }
        ready = true;
        initTables();
        SeedData.seedIfEmpty();
    }

    public static void initTables() {
        String[] createStatements = {
            "CREATE TABLE IF NOT EXISTS customers ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT, "
                + "email TEXT, "
                + "vip INTEGER DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS vehicles ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "registration_number TEXT NOT NULL, "
                + "brand TEXT, "
                + "model TEXT, "
                + "year INTEGER, "
                + "customer_id INTEGER)",

            "CREATE TABLE IF NOT EXISTS mechanics ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT, "
                + "specialization TEXT, "
                + "available INTEGER DEFAULT 1)",

            "CREATE TABLE IF NOT EXISTS service_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "description TEXT, "
                + "price REAL, "
                + "estimated_minutes INTEGER, "
                + "specialization TEXT)",

            "CREATE TABLE IF NOT EXISTS bookings ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "vehicle_id INTEGER, "
                + "start_time TEXT, "
                + "end_time TEXT, "
                + "mechanic_id INTEGER, "
                + "service_item_id INTEGER, "
                + "date TEXT, "
                + "description TEXT, "
                + "status TEXT)",

            "CREATE TABLE IF NOT EXISTS work_orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id INTEGER, "
                + "mechanic_id INTEGER, "
                + "status TEXT, "
                + "type TEXT, "
                + "vehicle_id INTEGER, "
                + "description TEXT, "
                + "original_work_order_id INTEGER, "
                + "planned_date TEXT, "
                + "customer_instructions TEXT, "
                + "other_comments TEXT)",

            "CREATE TABLE IF NOT EXISTS booking_service_items ("
                + "booking_id INTEGER NOT NULL, "
                + "service_item_id INTEGER NOT NULL, "
                + "package_name TEXT, "
                + "PRIMARY KEY (booking_id, service_item_id))",

            "CREATE TABLE IF NOT EXISTS service_packages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "description TEXT)",

            "CREATE TABLE IF NOT EXISTS service_package_items ("
                + "package_id INTEGER NOT NULL, "
                + "service_item_id INTEGER NOT NULL, "
                + "PRIMARY KEY (package_id, service_item_id))",

            "CREATE TABLE IF NOT EXISTS booking_mechanics ("
                + "booking_id INTEGER NOT NULL, "
                + "mechanic_id INTEGER NOT NULL, "
                + "PRIMARY KEY (booking_id, mechanic_id))",

            "CREATE TABLE IF NOT EXISTS work_order_service_items ("
                + "work_order_id INTEGER NOT NULL, "
                + "service_item_id INTEGER NOT NULL, "
                + "completed INTEGER NOT NULL DEFAULT 0, "
                + "price REAL, "
                + "package_name TEXT, "
                + "PRIMARY KEY (work_order_id, service_item_id))",

            "CREATE TABLE IF NOT EXISTS invoices ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "work_order_id INTEGER, "
                + "invoice_date TEXT, "
                + "amount REAL, "
                + "discount REAL, "
                + "total_amount REAL, "
                + "paid INTEGER DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS invoice_lines ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "invoice_id INTEGER NOT NULL, "
                + "service_item_id INTEGER, "
                + "service_name TEXT NOT NULL, "
                + "price REAL NOT NULL, "
                + "discount REAL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS payments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "invoice_id INTEGER, "
                + "amount REAL, "
                + "payment_type TEXT, "
                + "payment_date TEXT, "
                + "successful INTEGER DEFAULT 0)",

            // Persistent user settings (key/value). The language choice lives here so it survives
            // a restart; see Settings and AutoCoreApp.restoreLanguage.
            "CREATE TABLE IF NOT EXISTS settings ("
                + "key TEXT PRIMARY KEY, "
                + "value TEXT)"
        };

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            for (String sql : createStatements) {
                statement.executeUpdate(sql);
            }

            // Make sure the completed column exists when migrating.
            try {
                statement.executeUpdate("ALTER TABLE work_order_service_items ADD COLUMN completed INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Make sure the price column exists, so a performed job keeps its price.
            try {
                statement.executeUpdate("ALTER TABLE work_order_service_items ADD COLUMN price REAL");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The requirement hangs on a key: the service says which specialization it needs.
            // Empty means the service can be done by anyone.
            try {
                statement.executeUpdate("ALTER TABLE service_items ADD COLUMN specialization TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The vehicle sits on the work order, so a draft can be created before the booking exists.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN vehicle_id INTEGER");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The customer's own description of the problem, the one thing a draft needs.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN description TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The work order's type: standard, reclamation or internal work.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN type TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The reference a reclamation has to the work order it concerns. Empty for all others.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN original_work_order_id INTEGER");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The planned date on a draft, which has no booking yet to borrow a date from.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN planned_date TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The customer's instructions, filled in when the customer has something to say about the job.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN customer_instructions TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Other comments that belong to the order but not to any of the other fields.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN other_comments TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Make sure the package name exists, so a booking keeps the package it was made from.
            try {
                statement.executeUpdate("ALTER TABLE booking_service_items ADD COLUMN package_name TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // The order inherits the package names from the booking it was created from.
            try {
                statement.executeUpdate("ALTER TABLE work_order_service_items ADD COLUMN package_name TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Orders created before the type existed are ordinary jobs.
            statement.executeUpdate("UPDATE work_orders SET type = 'STANDARD' WHERE type IS NULL OR type = ''");

            // Warranty was renamed to reclamation once the requirement was pinned down.
            statement.executeUpdate("UPDATE work_orders SET type = 'RECLAMATION' WHERE type = 'WARRANTY'");
            // Services created before the requirement existed get theirs here, so an existing
            // database ends up with the same set as a freshly created one.
            statement.executeUpdate("UPDATE service_items SET specialization = 'seed.mechanic.brakes.specialization' "
                    + "WHERE name = 'seed.service.brake_service.name' AND (specialization IS NULL OR specialization = '')");
            statement.executeUpdate("UPDATE service_items SET specialization = 'seed.mechanic.diagnostics.specialization' "
                    + "WHERE name = 'seed.service.diagnostics.name' AND (specialization IS NULL OR specialization = '')");

            // Migrate older bookings into the join table.
            String migrateSql = "INSERT OR IGNORE INTO booking_service_items (booking_id, service_item_id) "
                    + "SELECT id, service_item_id FROM bookings "
                    + "WHERE service_item_id IS NOT NULL AND service_item_id > 0";
            statement.executeUpdate(migrateSql);

            // Migrate the booking's mechanic into the join table. Older bookings only have the one.
            statement.executeUpdate("INSERT OR IGNORE INTO booking_mechanics (booking_id, mechanic_id) "
                    + "SELECT id, mechanic_id FROM bookings "
                    + "WHERE mechanic_id IS NOT NULL AND mechanic_id > 0");

            // Create invoice lines for older invoices.
            // The price comes from the frozen price on the work order line when there is one,
            // otherwise from the catalogue. Without it an old invoice gets today's price instead
            // of the price that applied when the job was done.
            String migrateInvoiceLinesSql = "INSERT INTO invoice_lines "
                    + "(invoice_id, service_item_id, service_name, price, discount) "
                    + "SELECT i.id, s.id, s.name, CASE WHEN w.price IS NOT NULL THEN w.price ELSE s.price END, 0 "
                    + "FROM invoices i "
                    + "JOIN work_order_service_items w ON w.work_order_id = i.work_order_id "
                    + "JOIN service_items s ON s.id = w.service_item_id "
                    + "WHERE NOT EXISTS (SELECT 1 FROM invoice_lines l WHERE l.invoice_id = i.id)";
            statement.executeUpdate(migrateInvoiceLinesSql);

            // Registration numbers in the same shape across the register: capitals and a space
            // between the letters and the digits, so "abc123" and "ABC 123" don't become two cars.
            // Can't be done in SQL, since the space goes in at the right spot in Java.
            // Runs after the connection above, on a connection of its own, so no open cursor locks SQLite.

            System.out.println("Databas redo: " + DATABASE_PATH);

        } catch (SQLException e) {
            // Halfway through means a database that looks whole but is missing columns, and
            // then it is better to stop than to fill in seed data on top of the mess.
            throw new IllegalStateException("Databasen kunde inte förberedas: " + e.getMessage(), e);
        }

        normalizeRegistrationNumbers();
        SeedData.seedIfEmpty();
    }

    /** Lets "the column already exists" through and sends every other error on. */
    private static void rethrowUnlessDuplicateColumn(SQLException e) throws SQLException {
        if (String.valueOf(e.getMessage()).contains("duplicate column name")) {
            return;
        }
        throw e;
    }

    /** Fixes registration numbers saved before the model started normalizing them. */
    private static void normalizeRegistrationNumbers() {
        java.util.List<Integer> ids = new java.util.ArrayList<Integer>();
        java.util.List<String> numbers = new java.util.ArrayList<String>();

        try (java.sql.Connection connection = getConnection()) {
            java.sql.Statement read = connection.createStatement();
            java.sql.ResultSet rows = read.executeQuery(
                    "SELECT id, registration_number FROM vehicles WHERE registration_number IS NOT NULL");
            while (rows.next()) {
                ids.add(Integer.valueOf(rows.getInt(1)));
                numbers.add(rows.getString(2));
            }
            rows.close();
            read.close();

            java.sql.PreparedStatement write = connection.prepareStatement(
                    "UPDATE vehicles SET registration_number = ? WHERE id = ?");
            for (int i = 0; i < ids.size(); i++) {
                String normalized = com.wac.autocore.model.Vehicle.normalizeRegistrationNumber(numbers.get(i));
                if (normalized != null && !normalized.equals(numbers.get(i))) {
                    write.setString(1, normalized);
                    write.setInt(2, ids.get(i).intValue());
                    write.executeUpdate();
                }
            }
            write.close();
        } catch (java.sql.SQLException e) {
            System.out.println("Kunde inte rätta registreringsnumren: " + e.getMessage());
        }
    }
}
