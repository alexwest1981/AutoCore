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

            // Säkerställ att kolumnen completed finns vid migrering.
            try {
                statement.executeUpdate("ALTER TABLE work_order_service_items ADD COLUMN completed INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Säkerställ att kolumnen price finns, så ett utfört arbete behåller sitt pris.
            try {
                statement.executeUpdate("ALTER TABLE work_order_service_items ADD COLUMN price REAL");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Behörigheten hänger på en nyckel: tjänsten säger vilken specialisering den kräver.
            // Tomt betyder att tjänsten kan utföras av alla.
            try {
                statement.executeUpdate("ALTER TABLE service_items ADD COLUMN specialization TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Fordonet ligger på arbetsordern så ett utkast går att skapa innan bokningen finns.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN vehicle_id INTEGER");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Kundens egen beskrivning av problemet, den enda uppgift ett utkast behöver.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN description TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Arbetsorderns typ: standard, reklamation eller internt arbete.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN type TEXT");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Referensen en reklamation har till arbetsordern den gäller. Tom för alla andra.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN original_work_order_id INTEGER");
            } catch (SQLException e) {
                rethrowUnlessDuplicateColumn(e);
            }

            // Det planerade datumet på ett utkast, som ännu inte har någon bokning att låna ett datum ifrån.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN planned_date TEXT");
            } catch (SQLException ignored) {
                // Kolumnen existerar redan
            }

            // Kundens instruktioner, ifyllda när kunden har något att säga om arbetet.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN customer_instructions TEXT");
            } catch (SQLException ignored) {
                // Kolumnen existerar redan
            }

            // Övriga kommentarer som hör till ordern men inte till någon av de andra rutorna.
            try {
                statement.executeUpdate("ALTER TABLE work_orders ADD COLUMN other_comments TEXT");
            } catch (SQLException ignored) {
                // Kolumnen existerar redan
            }

            // Ordrar som skapades innan typen fanns är vanliga arbeten.
            statement.executeUpdate("UPDATE work_orders SET type = 'STANDARD' WHERE type IS NULL OR type = ''");

            // Garanti bytte namn till reklamation när kravet preciserades.
            statement.executeUpdate("UPDATE work_orders SET type = 'RECLAMATION' WHERE type = 'WARRANTY'");
            // Tjänster som skapades innan kravet fanns får sitt krav här, så en befintlig databas
            // får samma uppsättning som en nyskapad.
            statement.executeUpdate("UPDATE service_items SET specialization = 'seed.mechanic.brakes.specialization' "
                    + "WHERE name = 'seed.service.brake_service.name' AND (specialization IS NULL OR specialization = '')");
            statement.executeUpdate("UPDATE service_items SET specialization = 'seed.mechanic.diagnostics.specialization' "
                    + "WHERE name = 'seed.service.diagnostics.name' AND (specialization IS NULL OR specialization = '')");

            // Migrera äldre bokningar till kopplingstabellen.
            String migrateSql = "INSERT OR IGNORE INTO booking_service_items (booking_id, service_item_id) "
                    + "SELECT id, service_item_id FROM bookings "
                    + "WHERE service_item_id IS NOT NULL AND service_item_id > 0";
            statement.executeUpdate(migrateSql);

            // Migrera bokningens mekaniker till kopplingstabellen. Äldre bokningar har bara den ena.
            statement.executeUpdate("INSERT OR IGNORE INTO booking_mechanics (booking_id, mechanic_id) "
                    + "SELECT id, mechanic_id FROM bookings "
                    + "WHERE mechanic_id IS NOT NULL AND mechanic_id > 0");


            // Skapa fakturarader för äldre fakturor.
            // Priset tas från det frysta priset på arbetsorderns rad när det finns, annars
            // från katalogen. Utan det får en gammal faktura dagens pris i stället för
            // priset som gällde när arbetet utfördes.
            String migrateInvoiceLinesSql = "INSERT INTO invoice_lines "
                    + "(invoice_id, service_item_id, service_name, price, discount) "
                    + "SELECT i.id, s.id, s.name, CASE WHEN w.price IS NOT NULL THEN w.price ELSE s.price END, 0 "
                    + "FROM invoices i "
                    + "JOIN work_order_service_items w ON w.work_order_id = i.work_order_id "
                    + "JOIN service_items s ON s.id = w.service_item_id "
                    + "WHERE NOT EXISTS (SELECT 1 FROM invoice_lines l WHERE l.invoice_id = i.id)";
            statement.executeUpdate(migrateInvoiceLinesSql);

            // Registreringsnummer i samma skepnad i hela registret: versaler och mellanslag mellan
            // bokstäverna och siffrorna, så att "abc123" och "ABC 123" inte blir två olika fordon.
            // Går inte att göra i SQL, eftersom mellanslaget sätts in på rätt plats i Java.
            // Körs efter anslutningen ovan, på sin egen, så att ingen öppen kurs låser SQLite.

            System.out.println("Databas redo: " + DATABASE_PATH);

        } catch (SQLException e) {
            // Halvvägs igenom betyder en databas som ser hel ut men saknar kolumner, och
            // då är det bättre att stanna än att fylla i demodata ovanpå röran.
            throw new IllegalStateException("Databasen kunde inte förberedas: " + e.getMessage(), e);
        }

        normalizeRegistrationNumbers();
        SeedData.seedIfEmpty();
    }

    /** Släpper igenom "kolumnen finns redan" och låter alla andra fel gå vidare. */
    private static void rethrowUnlessDuplicateColumn(SQLException e) throws SQLException {
        if (String.valueOf(e.getMessage()).contains("duplicate column name")) {
            return;
        }
        throw e;
    }

/** Rättar registreringsnummer som sparades innan modellen normaliserade dem. */
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
