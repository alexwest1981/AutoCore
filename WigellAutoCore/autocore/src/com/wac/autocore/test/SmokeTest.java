package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.service.GarageSystem;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Snabb och robust Smoketest-modul för Wigell AutoCore.
 * Verifierar att kärnkomponenter, databas, UI-klasser, flerspråksresurser
 * och temafiler är intakta och laddas felfritt i JVM.
 */
public class SmokeTest {

    public void testDatabaseSchemaAndTables() throws SQLException {
        Db.initTables();
        Set<String> expectedTables = new HashSet<String>();
        expectedTables.add("customers");
        expectedTables.add("vehicles");
        expectedTables.add("bookings");
        expectedTables.add("mechanics");
        expectedTables.add("service_items");
        expectedTables.add("work_orders");
        expectedTables.add("invoices");
        expectedTables.add("payments");
        expectedTables.add("booking_service_items");
        expectedTables.add("work_order_service_items");
        expectedTables.add("invoice_lines");

        Set<String> actualTables = new HashSet<String>();
        try (Connection conn = Db.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        for (String exp : expectedTables) {
            TestRunner.assertTrue(actualTables.contains(exp),
                    "Smoketest: Tabellen '" + exp + "' måste finnas i databasschemat");
        }
        System.out.println("    [SmokeTest] Databasschema: Alla 11 tabeller verifierade i SQLite.");
    }

    public void testCoreApplicationClassesLoadable() throws ClassNotFoundException {
        String[] coreClasses = {
                "com.wac.autocore.service.GarageSystem",
                "com.wac.autocore.service.CustomerService",
                "com.wac.autocore.service.VehicleService",
                "com.wac.autocore.service.BookingService",
                "com.wac.autocore.service.WorkOrderService",
                "com.wac.autocore.service.BillingService",
                "com.wac.autocore.service.PaymentService",
                "com.wac.autocore.ui.AutoCoreApp",
                "com.wac.autocore.ui.BookingDialogs",
                "com.wac.autocore.theme.ThemeManager",
                "com.wac.autocore.ui.i18n.I18n",
                "Main",
                "ConsoleApp"
        };

        for (String className : coreClasses) {
            Class<?> c = Class.forName(className);
            TestRunner.assertNotNull(c, "Klassen " + className + " ska kunna laddas av klassladdaren");
        }
        System.out.println("    [SmokeTest] Klassladdning: Samtliga " + coreClasses.length + " kärnklasser laddade utan länkfel.");
    }

    public void testI18nAndThemeResourcesAvailable() {
        InputStream svStream = getClass().getResourceAsStream("/com/wac/autocore/i18n/sv.json");
        InputStream enStream = getClass().getResourceAsStream("/com/wac/autocore/i18n/en.json");
        TestRunner.assertNotNull(svStream, "Smoketest: sv.json måste finnas i classpath");
        TestRunner.assertNotNull(enStream, "Smoketest: en.json måste finnas i classpath");

        InputStream cssStream = getClass().getResourceAsStream("/com/wac/autocore/theme/themes/emerald/emerald.css");
        TestRunner.assertNotNull(cssStream, "Smoketest: emerald.css måste finnas i classpath");

        System.out.println("    [SmokeTest] Resurskontroll: Språkfiler (sv.json, en.json) och CSS-teman finns och är läsbara.");
    }

    public void testGarageSystemStartupPerformance() {
        long start = System.currentTimeMillis();
        GarageSystem system = new GarageSystem();
        int customers = system.getCustomers().size();
        int vehicles = system.getVehicles().size();
        int services = system.getServiceItems().size();
        long duration = System.currentTimeMillis() - start;

        TestRunner.assertTrue(customers >= 0, "Kunder ska kunna läsas");
        TestRunner.assertTrue(vehicles >= 0, "Fordon ska kunna läsas");
        TestRunner.assertTrue(services >= 0, "Tjänster ska kunna läsas");
        TestRunner.assertTrue(duration < 2000, "Smoketest: GarageSystem-initiering ska ta under 2 sekunder");

        System.out.println("    [SmokeTest] Prestanda: Systemet bootar och svarar på " + duration + " ms.");
    }
}
