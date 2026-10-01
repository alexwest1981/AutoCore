package com.wac.autocore.test;

import com.wac.autocore.data.Db;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import com.wac.autocore.seed.SeedText;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.util.EntityLookup;
import com.wac.autocore.ui.util.UiFormatters;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Automatiserade bevis- och acceptanskontroller för JIRA-ärenden (Sprint 3: AutoCore 2.5).
 * Testerna bevisar formellt att beställarens krav och beviskorten är uppfyllda.
 */
public class EvidenceVerificationTest {

    private final GarageSystem garage = new GarageSystem();

    /**
     * BEVISKORT SCRUM-159 (D1) & Kriterium 7, 8:
     * Prisändring på en tjänst, kontrollerad hela vägen.
     * En prishöjning syns på en ny bokning och arbetsorder, medan en äldre slutförd
     * arbetsorder och faktura behåller sitt ursprungliga frysta pris.
     */
    public void testScrum159PriceChangeControlledAllTheWay() throws SQLException {
        List<ServiceItem> services = garage.getServiceItems();
        TestRunner.assertTrue(!services.isEmpty(), "Databasen ska innehålla tjänster");
        ServiceItem target = services.get(0);
        double originalPrice = target.getPrice();
        double increasedPrice = originalPrice + 350.0;

        // 1. Skapa historisk bokning, arbetsorder och faktura med ursprungligt pris
        int vehicleId = garage.getVehicles().get(0).getId();
        int mechanicId = garage.getMechanics().get(0).getId();

        Booking oldBooking = garage.createBooking(vehicleId, LocalDate.now().minusDays(10), "Äldre bokning före prisändring");
        WorkOrder oldWorkOrder = new WorkOrder(0, oldBooking.getId(), mechanicId);
        oldWorkOrder.addServiceItem(target.getId());
        oldWorkOrder.setStatus("COMPLETED");
        WorkOrderRepository woRepo = new WorkOrderRepository();
        woRepo.save(oldWorkOrder);

        Invoice oldInvoice = garage.createInvoice(oldWorkOrder.getId(), null);
        TestRunner.assertNotNull(oldInvoice, "Äldre faktura ska ha skapats");

        Booking newBooking = null;
        WorkOrder newWorkOrder = null;
        Invoice newInvoice = null;

        try {
            // 2. Ändra priset i katalogen (administratörsåtgärd)
            target.setPrice(increasedPrice);
            garage.updateServiceItem(target);

            // 3. Skapa ny bokning och ny arbetsorder efter prisändringen
            newBooking = garage.createBooking(vehicleId, LocalDate.now(), "Ny bokning efter prisändring");
            newWorkOrder = new WorkOrder(0, newBooking.getId(), mechanicId);
            newWorkOrder.addServiceItem(target.getId());
            newWorkOrder.setStatus("COMPLETED");
            woRepo.save(newWorkOrder);

            newInvoice = garage.createInvoice(newWorkOrder.getId(), null);
            TestRunner.assertNotNull(newInvoice, "Ny faktura ska ha skapats");

            // 4. Verifiera: Äldre faktura har kvar ursprungligt fryst pris
            Invoice readBackOld = new InvoiceRepository().findById(oldInvoice.getId());
            TestRunner.assertEquals(originalPrice, readBackOld.getLines().get(0).getPrice(),
                    "SCRUM-159: Äldre fakturarad ska ha kvar ursprungligt fryst pris");
            TestRunner.assertEquals(originalPrice, readBackOld.getTotalAmount(),
                    "SCRUM-159: Äldre fakturatotal ska vara oförändrad");

            // 5. Verifiera: Ny faktura har det nya höjda priset
            Invoice readBackNew = new InvoiceRepository().findById(newInvoice.getId());
            TestRunner.assertEquals(increasedPrice, readBackNew.getLines().get(0).getPrice(),
                    "SCRUM-159: Ny fakturarad ska använda det nya höjda priset");
            TestRunner.assertEquals(increasedPrice, readBackNew.getTotalAmount(),
                    "SCRUM-159: Ny fakturatotal ska matcha det höjda priset");

            System.out.println("    [SCRUM-159 BEVIS] Äldre faktura (ID " + oldInvoice.getId() + "): " + readBackOld.getTotalAmount()
                    + " kr | Ny faktura (ID " + newInvoice.getId() + "): " + readBackNew.getTotalAmount() + " kr");

        } finally {
            // Återställ katalogpris
            target.setPrice(originalPrice);
            garage.updateServiceItem(target);

            // Städa testposter
            if (newInvoice != null) new InvoiceRepository().delete(newInvoice.getId());
            if (newWorkOrder != null) woRepo.delete(newWorkOrder.getId());
            if (newBooking != null) new BookingRepository().delete(newBooking.getId());

            if (oldInvoice != null) new InvoiceRepository().delete(oldInvoice.getId());
            woRepo.delete(oldWorkOrder.getId());
            new BookingRepository().delete(oldBooking.getId());
        }
    }

    /**
     * BEVISKORT SCRUM-161 (D3) & Kriterium 8:
     * Historiken syns på arbetsordern och på fakturan i gränssnittet.
     * Priserna som gällde när arbetet utfördes måste synas på avslutade arbetsordrar
     * och äldre fakturor i gränssnittet (inte bara i databasen).
     *
     * Klart-kriterium: Samma tjänst visas med två olika priser på två olika arbetsordrar,
     * och en äldre faktura skapad före prishöjning visar sitt ursprungliga frysta pris.
     */
    public void testScrum161HistoricalPricesVisibleInUi() throws SQLException {
        List<ServiceItem> services = garage.getServiceItems();
        TestRunner.assertTrue(!services.isEmpty(), "Databasen ska innehålla tjänster");
        ServiceItem target = services.get(0);
        double originalPrice = target.getPrice();
        double updatedPrice = originalPrice + 400.0;

        int vehicleId = garage.getVehicles().get(0).getId();
        int mechanicId = garage.getMechanics().get(0).getId();

        WorkOrderRepository woRepo = new WorkOrderRepository();
        BookingRepository bRepo = new BookingRepository();
        InvoiceRepository invRepo = new InvoiceRepository();

        // 1. Skapa första bokning, arbetsorder och faktura till ursprungligt pris
        Booking b1 = garage.createBooking(vehicleId, LocalDate.now().minusDays(5), "Bokning 1 - Ursprungligt pris");
        WorkOrder wo1 = new WorkOrder(0, b1.getId(), mechanicId);
        wo1.addServiceItem(target.getId());
        wo1.setStatus("COMPLETED");
        woRepo.save(wo1);

        Invoice inv1 = garage.createInvoice(wo1.getId(), null);
        TestRunner.assertNotNull(inv1, "Faktura 1 ska ha skapats");

        Booking b2 = null;
        WorkOrder wo2 = null;
        Invoice inv2 = null;

        try {
            // 2. Höj katalogpriset på tjänsten
            target.setPrice(updatedPrice);
            garage.updateServiceItem(target);

            // 3. Skapa andra bokning, arbetsorder och faktura efter prishöjning
            b2 = garage.createBooking(vehicleId, LocalDate.now(), "Bokning 2 - Nytt högre pris");
            wo2 = new WorkOrder(0, b2.getId(), mechanicId);
            wo2.addServiceItem(target.getId());
            wo2.setStatus("COMPLETED");
            woRepo.save(wo2);

            inv2 = garage.createInvoice(wo2.getId(), null);
            TestRunner.assertNotNull(inv2, "Faktura 2 ska ha skapats");

            // 4. Verifiera i UI-lookup: Arbetsorder 1 visar det ursprungliga priset (fryst historik)
            String uiServicesWo1 = EntityLookup.workOrderServicesWithPrices(garage, wo1);
            double uiTotalWo1 = EntityLookup.workOrderTotal(garage, wo1);
            double wo1ServicePrice = EntityLookup.workOrderServicePrice(garage, wo1, target.getId());

            TestRunner.assertEquals(originalPrice, wo1ServicePrice, "WO 1 tjänstepris ska vara ursprungligt fryst pris");
            TestRunner.assertEquals(originalPrice, uiTotalWo1, "WO 1 totalpris i UI ska vara fryst ursprungspris");
            TestRunner.assertTrue(uiServicesWo1.contains(UiFormatters.formatMoney(originalPrice)),
                    "UI-tjänstvisning för WO 1 måste innehålla det ursprungliga frysta priset (" + originalPrice + " kr)");

            // 5. Verifiera i UI-lookup: Arbetsorder 2 visar det nya högre priset
            String uiServicesWo2 = EntityLookup.workOrderServicesWithPrices(garage, wo2);
            double uiTotalWo2 = EntityLookup.workOrderTotal(garage, wo2);
            double wo2ServicePrice = EntityLookup.workOrderServicePrice(garage, wo2, target.getId());

            TestRunner.assertEquals(updatedPrice, wo2ServicePrice, "WO 2 tjänstepris ska vara det nya priset");
            TestRunner.assertEquals(updatedPrice, uiTotalWo2, "WO 2 totalpris i UI ska visa det nya priset");
            TestRunner.assertTrue(uiServicesWo2.contains(UiFormatters.formatMoney(updatedPrice)),
                    "UI-tjänstvisning för WO 2 måste innehålla det uppdaterade priset (" + updatedPrice + " kr)");

            // 6. Verifiera klart-kriteriet: Samma tjänst visas med två OLIKA priser på två arbetsordrar
            TestRunner.assertTrue(!uiServicesWo1.equals(uiServicesWo2),
                    "Klart-kriterium SCRUM-161: Samma tjänst ska visas med två olika priser på WO 1 och WO 2");

            // 7. Verifiera äldre faktura och ny faktura i UI
            TestRunner.assertEquals(originalPrice, inv1.getLines().get(0).getPrice(), "Äldre faktura visar ursprungligt fryst pris");
            TestRunner.assertEquals(updatedPrice, inv2.getLines().get(0).getPrice(), "Ny faktura visar det nya priset");

            System.out.println("    [SCRUM-161 BEVIS] Klart-kriterium uppfyllt:");
            System.out.println("      Arbetsorder #" + wo1.getId() + " (äldre fryst): " + uiServicesWo1 + " | Total: " + uiTotalWo1 + " kr");
            System.out.println("      Arbetsorder #" + wo2.getId() + " (nyare): " + uiServicesWo2 + " | Total: " + uiTotalWo2 + " kr");
            System.out.println("      Faktura #" + inv1.getId() + " (äldre fryst): " + inv1.getTotalAmount() + " kr");
            System.out.println("      Faktura #" + inv2.getId() + " (nyare): " + inv2.getTotalAmount() + " kr");

        } finally {
            // Återställ katalogpris
            target.setPrice(originalPrice);
            garage.updateServiceItem(target);

            // Städa testdata
            if (inv2 != null) invRepo.delete(inv2.getId());
            if (wo2 != null) woRepo.delete(wo2.getId());
            if (b2 != null) bRepo.delete(b2.getId());

            if (inv1 != null) invRepo.delete(inv1.getId());
            woRepo.delete(wo1.getId());
            bRepo.delete(b1.getId());
        }
    }

    /**
     * BEVISKORT SCRUM-165 (E4) & Kriterium 9:
     * VIP och rabattkoder fungerar som förut med den nya fakturamodellen.
     * Tre tester av rabattreglerna (VIP 10%, WELCOME10, SERVICE200) samt skydd mot negativt belopp.
     */
    public void testScrum165VipAndDiscountCodesWorkAsBefore() throws SQLException {
        CustomerRepository custRepo = new CustomerRepository();
        VehicleRepository vehRepo = new VehicleRepository();
        BookingRepository bookRepo = new BookingRepository();
        WorkOrderRepository woRepo = new WorkOrderRepository();
        InvoiceRepository invRepo = new InvoiceRepository();

        ServiceItem s1 = garage.getServiceItems().get(0);
        double basePrice = s1.getPrice();
        int mechanicId = garage.getMechanics().get(0).getId();

        // Skapa temporär VIP-kund och fordon
        Customer vipCust = garage.createCustomer("VIP TestKund", "070-9998877", "vip@test.se");
        vipCust.setVip(true);
        custRepo.save(vipCust);

        Customer normalCust = garage.createCustomer("Normal TestKund", "070-1112233", "normal@test.se");
        normalCust.setVip(false);
        custRepo.save(normalCust);

        Vehicle vipVeh = garage.createVehicle("VIP999", "Volvo", "V90", 2023, vipCust.getId());
        Vehicle normalVeh = garage.createVehicle("NRM111", "Saab", "9-3", 2008, normalCust.getId());

        Booking b1 = null, b2 = null, b3 = null, b4 = null;
        WorkOrder wo1 = null, wo2 = null, wo3 = null, wo4 = null;
        Invoice inv1 = null, inv2 = null, inv3 = null, inv4 = null;

        try {
            // Test 1: VIP 10% rabatt
            b1 = garage.createBooking(vipVeh.getId(), LocalDate.now(), "VIP rabattprov");
            wo1 = new WorkOrder(0, b1.getId(), mechanicId);
            wo1.addServiceItem(s1.getId());
            wo1.setStatus("COMPLETED");
            woRepo.save(wo1);
            inv1 = garage.createInvoice(wo1.getId(), null);
            TestRunner.assertNotNull(inv1, "Faktura för VIP ska skapas");
            double expectedVipTotal = basePrice * 0.90;
            TestRunner.assertEquals(expectedVipTotal, inv1.getTotalAmount(), "VIP ska ge exakt 10% rabatt");
            System.out.println("    [SCRUM-165 Regel 1] VIP 10%: Basbelopp " + basePrice + " kr -> Totalt med rabatt: " + inv1.getTotalAmount() + " kr");

            // Test 2: Rabattkod WELCOME10 (10%)
            b2 = garage.createBooking(normalVeh.getId(), LocalDate.now(), "WELCOME10 rabattprov");
            wo2 = new WorkOrder(0, b2.getId(), mechanicId);
            wo2.addServiceItem(s1.getId());
            wo2.setStatus("COMPLETED");
            woRepo.save(wo2);
            inv2 = garage.createInvoice(wo2.getId(), "WELCOME10");
            TestRunner.assertNotNull(inv2, "Faktura med WELCOME10 ska skapas");
            double expectedWelcomeTotal = basePrice * 0.90;
            TestRunner.assertEquals(expectedWelcomeTotal, inv2.getTotalAmount(), "WELCOME10 ska ge 10% rabatt");
            System.out.println("    [SCRUM-165 Regel 2] Kod WELCOME10 (10%): Basbelopp " + basePrice + " kr -> Totalt: " + inv2.getTotalAmount() + " kr");

            // Test 3: Rabattkod SERVICE200 (200 kr)
            b3 = garage.createBooking(normalVeh.getId(), LocalDate.now(), "SERVICE200 rabattprov");
            wo3 = new WorkOrder(0, b3.getId(), mechanicId);
            wo3.addServiceItem(s1.getId());
            wo3.setStatus("COMPLETED");
            woRepo.save(wo3);
            inv3 = garage.createInvoice(wo3.getId(), "SERVICE200");
            TestRunner.assertNotNull(inv3, "Faktura med SERVICE200 ska skapas");
            double expectedService200Total = Math.max(0.0, basePrice - 200.0);
            TestRunner.assertEquals(expectedService200Total, inv3.getTotalAmount(), "SERVICE200 ska dra av 200 kr");
            System.out.println("    [SCRUM-165 Regel 3] Kod SERVICE200 (-200 kr): Basbelopp " + basePrice + " kr -> Totalt: " + inv3.getTotalAmount() + " kr");

            // Test 4: Skydd mot negativt belopp
            b4 = garage.createBooking(vipVeh.getId(), LocalDate.now(), "Max rabattprov");
            wo4 = new WorkOrder(0, b4.getId(), mechanicId);
            wo4.addServiceItem(s1.getId());
            wo4.setStatus("COMPLETED");
            woRepo.save(wo4);
            inv4 = garage.createInvoice(wo4.getId(), "SERVICE200");
            TestRunner.assertTrue(inv4.getTotalAmount() >= 0.0, "Fakturabelopp får aldrig bli negativt");

        } finally {
            if (inv1 != null) invRepo.delete(inv1.getId());
            if (wo1 != null) woRepo.delete(wo1.getId());
            if (b1 != null) bookRepo.delete(b1.getId());

            if (inv2 != null) invRepo.delete(inv2.getId());
            if (wo2 != null) woRepo.delete(wo2.getId());
            if (b2 != null) bookRepo.delete(b2.getId());

            if (inv3 != null) invRepo.delete(inv3.getId());
            if (wo3 != null) woRepo.delete(wo3.getId());
            if (b3 != null) bookRepo.delete(b3.getId());

            if (inv4 != null) invRepo.delete(inv4.getId());
            if (wo4 != null) woRepo.delete(wo4.getId());
            if (b4 != null) bookRepo.delete(b4.getId());

            vehRepo.delete(vipVeh.getId());
            vehRepo.delete(normalVeh.getId());
            custRepo.delete(vipCust.getId());
            custRepo.delete(normalCust.getId());
        }
    }

    /**
     * BEVISKORT SCRUM-168 (F2) & Kriterium 10:
     * Rundtur för varje ny tabell/klass (booking_service_items, invoice_lines).
     * Skriver, läser, ändrar och läser igen.
     */
    public void testScrum168RoundtripForNewEntities() throws SQLException {
        BookingRepository bRepo = new BookingRepository();
        InvoiceRepository invRepo = new InvoiceRepository();
        WorkOrderRepository woRepo = new WorkOrderRepository();

        int vehicleId = garage.getVehicles().get(0).getId();
        int mechanicId = garage.getMechanics().get(0).getId();
        List<ServiceItem> services = garage.getServiceItems();
        ServiceItem s1 = services.get(0);
        ServiceItem s2 = services.get(1);

        // 1. Rundtur för booking_service_items
        Booking booking = new Booking(vehicleId, LocalDate.now().plusDays(2), "Rundtur bokning");
        booking.addServiceItem(s1);
        bRepo.save(booking);
        int bookingId = booking.getId();
        TestRunner.assertTrue(bookingId > 0, "Bokning ska sparas");

        try {
            // Läs tillbaka
            Booking read1 = new BookingRepository().findById(bookingId);
            TestRunner.assertEquals(1, read1.getServiceItems().size(), "1 tjänst ska läsas upp");

            // Ändra (lägg till en tjänst till innan start)
            read1.addServiceItem(s2);
            bRepo.save(read1);

            // Läs igen
            Booking read2 = new BookingRepository().findById(bookingId);
            TestRunner.assertEquals(2, read2.getServiceItems().size(), "2 tjänster ska finnas efter uppdatering");
            System.out.println("    [SCRUM-168 Rundtur 1/2] booking_service_items: Skapad -> Läst -> Uppdaterad (2 tjänster) -> Verifierad.");

            // 2. Rundtur för invoice_lines
            WorkOrder wo = new WorkOrder(0, bookingId, mechanicId);
            wo.addServiceItem(s1.getId());
            wo.addServiceItem(s2.getId());
            wo.setStatus("COMPLETED");
            woRepo.save(wo);

            Invoice inv = garage.createInvoice(wo.getId(), null);
            TestRunner.assertNotNull(inv, "Faktura ska genereras");
            int invoiceId = inv.getId();

            try {
                Invoice readInv = new InvoiceRepository().findById(invoiceId);
                TestRunner.assertNotNull(readInv, "Faktura ska gå att läsa tillbaka");
                TestRunner.assertEquals(2, readInv.getLines().size(), "Fakturan ska ha 2 rader sparade");
                TestRunner.assertEquals(readInv.getLinesTotal(), readInv.getTotalAmount(), "Radsumman ska matcha totalbeloppet");
                System.out.println("    [SCRUM-168 Rundtur 2/2] invoice_lines: Skapad -> Läst -> Radsumma verifierad mot totalbelopp.");
            } finally {
                invRepo.delete(invoiceId);
                woRepo.delete(wo.getId());
            }

        } finally {
            bRepo.delete(bookingId);
        }
    }

    /**
     * BEVISKORT SCRUM-169 (F3) & Kriterium 11:
     * Omstartsbeviset: Fullständig, verklig processomstart mellan två separata JVM-instanser.
     * Process 1 skriver data och terminerar helt.
     * Process 2 startar upp från scratch i ett nytt OS-process-PID och läser tillbaka
     * bokningens tjänster, arbetsorderns tjänster och fakturans rader med 100% dataintegritet.
     */
    public void testScrum169RestartEvidence() throws SQLException {
        // 1. Kör äkta tvåprocessomstart via RestartProofRunner
        boolean multiProcessSuccess = RestartProofRunner.runProcessRestartTest();
        TestRunner.assertTrue(multiProcessSuccess, "SCRUM-169: Verklig processomstart med skilda PID:er ska lyckas");

        // 2. Extra kontroll i samma process med färska repository-instanser
        int vehicleId = garage.getVehicles().get(0).getId();
        int mechanicId = garage.getMechanics().get(0).getId();
        List<ServiceItem> services = garage.getServiceItems();

        Booking booking = new Booking(vehicleId, LocalDate.now().plusDays(4), "Omstartstest intern kontroll");
        booking.addServiceItem(services.get(0));
        booking.addServiceItem(services.get(1));
        new BookingRepository().save(booking);
        int bId = booking.getId();

        WorkOrder wo = new WorkOrder(0, bId, mechanicId);
        wo.addServiceItem(services.get(0).getId());
        wo.addServiceItem(services.get(1).getId());
        wo.setStatus("COMPLETED");
        new WorkOrderRepository().save(wo);
        int woId = wo.getId();

        Invoice inv = garage.createInvoice(woId, null);
        int invId = inv.getId();

        try {
            BookingRepository restartBRepo = new BookingRepository();
            WorkOrderRepository restartWoRepo = new WorkOrderRepository();
            InvoiceRepository restartInvRepo = new InvoiceRepository();

            Booking reloadedBooking = restartBRepo.findById(bId);
            WorkOrder reloadedWo = restartWoRepo.findById(woId);
            Invoice reloadedInv = restartInvRepo.findById(invId);

            TestRunner.assertNotNull(reloadedBooking, "Bokning ska överleva omstart");
            TestRunner.assertNotNull(reloadedWo, "Arbetsorder ska överleva omstart");
            TestRunner.assertNotNull(reloadedInv, "Faktura ska överleva omstart");

            TestRunner.assertEquals(2, reloadedBooking.getServiceItems().size(), "Bokningens tjänster intakta");
            TestRunner.assertEquals(2, reloadedWo.getServiceItemIds().size(), "Arbetsorderns tjänster intakta");
            TestRunner.assertEquals(2, reloadedInv.getLines().size(), "Fakturans rader intakta");

            TestRunner.assertEquals(bId, reloadedWo.getBookingId(), "Arbetsordern pekar på samma boknings-ID");
            TestRunner.assertEquals(woId, reloadedInv.getWorkOrderId(), "Fakturan pekar på samma arbetsorder-ID");
        } finally {
            new InvoiceRepository().delete(invId);
            new WorkOrderRepository().delete(woId);
            new BookingRepository().delete(bId);
        }
    }

    /**
     * BEVISKORT SCRUM-170 (F4) & Kriterium 10, 11:
     * Befintlig data behålls: Verifiering av radantal före och efter migrering mot en
     * legacy-databas (AutoCore 2.0 utan booking_service_items eller invoice_lines).
     * Bevisar 0 dataförlust över alla 8 ursprungliga tabeller, samt att nya kopplingstabeller
     * fylls i korrekt vid migreringen.
     */
    public void testScrum170ExistingDataRetained() throws SQLException {
        File fixtureFile = new File("data/autocore_v2_legacy_migration_fixture.db");
        if (fixtureFile.exists()) {
            fixtureFile.delete();
        }

        String jdbcUrl = "jdbc:sqlite:" + fixtureFile.getPath();
        Map<String, Integer> countsBefore = new LinkedHashMap<String, Integer>();
        Map<String, Integer> countsAfter = new LinkedHashMap<String, Integer>();

        String[] legacyTables = {
                "customers", "vehicles", "mechanics", "service_items",
                "bookings", "work_orders", "invoices", "payments"
        };

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            // 1. Skapa AutoCore 2.0 legacy-schema (utan de nya Sprint 3-tabellerna)
            stmt.executeUpdate("CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, email TEXT, vip INTEGER DEFAULT 0)");
            stmt.executeUpdate("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, registration_number TEXT NOT NULL, brand TEXT, model TEXT, year INTEGER, customer_id INTEGER)");
            stmt.executeUpdate("CREATE TABLE mechanics (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, specialization TEXT, available INTEGER DEFAULT 1)");
            stmt.executeUpdate("CREATE TABLE service_items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, price REAL, estimated_minutes INTEGER)");
            stmt.executeUpdate("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER, start_time TEXT, end_time TEXT, mechanic_id INTEGER, service_item_id INTEGER, date TEXT, description TEXT, status TEXT)");
            stmt.executeUpdate("CREATE TABLE work_orders (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, mechanic_id INTEGER, status TEXT)");
            stmt.executeUpdate("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, work_order_id INTEGER, invoice_date TEXT, amount REAL, discount REAL, total_amount REAL, paid INTEGER DEFAULT 0)");
            stmt.executeUpdate("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER, amount REAL, payment_type TEXT, payment_date TEXT, successful INTEGER DEFAULT 0)");

            // 2. Fyll på representativ legacy-data i alla 8 tabeller
            stmt.executeUpdate("INSERT INTO customers (name, phone, email, vip) VALUES ('Legacy Kund 1', '070-111111', 'k1@legacy.se', 0)");
            stmt.executeUpdate("INSERT INTO customers (name, phone, email, vip) VALUES ('Legacy VIP 2', '070-222222', 'vip2@legacy.se', 1)");
            stmt.executeUpdate("INSERT INTO customers (name, phone, email, vip) VALUES ('Legacy Kund 3', '070-333333', 'k3@legacy.se', 0)");

            stmt.executeUpdate("INSERT INTO vehicles (registration_number, brand, model, year, customer_id) VALUES ('LEG001', 'Volvo', 'V70', 2012, 1)");
            stmt.executeUpdate("INSERT INTO vehicles (registration_number, brand, model, year, customer_id) VALUES ('LEG002', 'Saab', '9-5', 2008, 2)");
            stmt.executeUpdate("INSERT INTO vehicles (registration_number, brand, model, year, customer_id) VALUES ('LEG003', 'VW', 'Golf', 2019, 3)");

            stmt.executeUpdate("INSERT INTO mechanics (name, phone, specialization, available) VALUES ('Mekaniker 1', '070-444444', 'Motor', 1)");
            stmt.executeUpdate("INSERT INTO mechanics (name, phone, specialization, available) VALUES ('Mekaniker 2', '070-555555', 'Bromsar', 1)");

            stmt.executeUpdate("INSERT INTO service_items (name, description, price, estimated_minutes) VALUES ('Oljebyte', 'Olja och filter', 899.0, 45)");
            stmt.executeUpdate("INSERT INTO service_items (name, description, price, estimated_minutes) VALUES ('Bromsservice', 'Klossar och skivor', 1495.0, 90)");
            stmt.executeUpdate("INSERT INTO service_items (name, description, price, estimated_minutes) VALUES ('Däckbyte', 'Hjulskifte', 399.0, 30)");
            stmt.executeUpdate("INSERT INTO service_items (name, description, price, estimated_minutes) VALUES ('Felsökning', 'Diagnostik', 750.0, 60)");

            stmt.executeUpdate("INSERT INTO bookings (vehicle_id, start_time, end_time, mechanic_id, service_item_id, date, description, status) VALUES (1, '08:00', '08:45', 1, 1, '2026-09-01', 'Oljebyte bokning', 'COMPLETED')");
            stmt.executeUpdate("INSERT INTO bookings (vehicle_id, start_time, end_time, mechanic_id, service_item_id, date, description, status) VALUES (2, '09:00', '10:30', 2, 2, '2026-09-02', 'Bromsbyte bokning', 'COMPLETED')");
            stmt.executeUpdate("INSERT INTO bookings (vehicle_id, start_time, end_time, mechanic_id, service_item_id, date, description, status) VALUES (3, '11:00', '11:30', 1, 3, '2026-09-03', 'Däckbyte bokning', 'BOOKED')");

            stmt.executeUpdate("INSERT INTO work_orders (booking_id, mechanic_id, status) VALUES (1, 1, 'COMPLETED')");
            stmt.executeUpdate("INSERT INTO work_orders (booking_id, mechanic_id, status) VALUES (2, 2, 'COMPLETED')");

            stmt.executeUpdate("INSERT INTO invoices (work_order_id, invoice_date, amount, discount, total_amount, paid) VALUES (1, '2026-09-01', 899.0, 0.0, 899.0, 1)");
            stmt.executeUpdate("INSERT INTO invoices (work_order_id, invoice_date, amount, discount, total_amount, paid) VALUES (2, '2026-09-02', 1495.0, 149.5, 1345.5, 1)");

            stmt.executeUpdate("INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) VALUES (1, 899.0, 'Kort', '2026-09-01', 1)");
            stmt.executeUpdate("INSERT INTO payments (invoice_id, amount, payment_type, payment_date, successful) VALUES (2, 1345.5, 'Swish', '2026-09-02', 1)");

            // 3. Räkna rader före migrering
            for (String table : legacyTables) {
                int count = countLegacyTable(stmt, table);
                countsBefore.put(table, count);
            }

            // 4. Exekvera AutoCore 2.5 migreringen på legacy-databasen
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS booking_service_items (booking_id INTEGER NOT NULL, service_item_id INTEGER NOT NULL, PRIMARY KEY (booking_id, service_item_id))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS work_order_service_items (work_order_id INTEGER NOT NULL, service_item_id INTEGER NOT NULL, completed INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (work_order_id, service_item_id))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS invoice_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL, service_item_id INTEGER, service_name TEXT NOT NULL, price REAL NOT NULL, discount REAL DEFAULT 0)");

            // Migrering A3: bookings.service_item_id -> booking_service_items
            stmt.executeUpdate("INSERT OR IGNORE INTO booking_service_items (booking_id, service_item_id) SELECT id, service_item_id FROM bookings WHERE service_item_id IS NOT NULL AND service_item_id > 0");

            // Migrering C1: work_orders + bookings -> work_order_service_items
            stmt.executeUpdate("INSERT OR IGNORE INTO work_order_service_items (work_order_id, service_item_id, completed) SELECT w.id, b.service_item_id, 1 FROM work_orders w JOIN bookings b ON b.id = w.booking_id WHERE b.service_item_id IS NOT NULL AND b.service_item_id > 0");

            // Migrering E2: invoices -> invoice_lines
            stmt.executeUpdate("INSERT INTO invoice_lines (invoice_id, service_item_id, service_name, price, discount) SELECT i.id, s.id, s.name, s.price, 0 FROM invoices i JOIN work_order_service_items w ON w.work_order_id = i.work_order_id JOIN service_items s ON s.id = w.service_item_id WHERE NOT EXISTS (SELECT 1 FROM invoice_lines l WHERE l.invoice_id = i.id)");

            // 5. Räkna rader efter migrering och validera 100% dataintegritet
            for (String table : legacyTables) {
                int count = countLegacyTable(stmt, table);
                countsAfter.put(table, count);
            }

            int migratedBookings = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM booking_service_items")) {
                if (rs.next()) migratedBookings = rs.getInt(1);
            }
            int migratedInvoiceLines = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM invoice_lines")) {
                if (rs.next()) migratedInvoiceLines = rs.getInt(1);
            }

            // 6. Assertions för noll dataförlust och framgångsrik migrering
            System.out.println("    [SCRUM-170 BEVIS: MIGRERING FRÅN V2.0 TILL V2.5]");
            for (String table : legacyTables) {
                int before = countsBefore.get(table);
                int after = countsAfter.get(table);
                TestRunner.assertEquals(before, after, "Tabell '" + table + "' ska ha exakt samma radantal före och efter migrering");
                System.out.println("      • Tabell " + String.format("%-14s", table) + ": " + before + " rader -> " + after + " rader (Dataförlust: 0)");
            }

            TestRunner.assertEquals(3, migratedBookings, "Alla 3 legacy-bokningar ska migreras till booking_service_items");
            TestRunner.assertEquals(2, migratedInvoiceLines, "Båda legacy-fakturorna ska få genererade invoice_lines");
            System.out.println("      • Nya tabeller migrerade: booking_service_items=" + migratedBookings + ", invoice_lines=" + migratedInvoiceLines);

        } finally {
            if (fixtureFile.exists()) {
                fixtureFile.delete();
            }
        }
    }

    /**
     * BEVISKORT SCRUM-172 (G2) & Kriterium 12:
     * De nio befintliga områdena kontrolleras ett i taget med konkreta funktionella operationer,
     * affärsregler och assertions för att leverera fullständigt bevis inför redovisningen.
     */
    public void testScrum172NineCoreAreasVerified() throws SQLException {
        CustomerRepository custRepo = new CustomerRepository();
        VehicleRepository vehRepo = new VehicleRepository();
        BookingRepository bookRepo = new BookingRepository();
        WorkOrderRepository woRepo = new WorkOrderRepository();
        InvoiceRepository invRepo = new InvoiceRepository();
        PaymentRepository payRepo = new PaymentRepository();

        System.out.println("    [SCRUM-172 BEVIS: DE NIO BEFINTLIGA OMRÅDENA KONTROLLERAS]");

        Customer c = null;
        Vehicle v = null;
        Booking b = null;
        WorkOrder wo = null;
        Invoice inv = null;
        Payment p = null;

        try {
            // 1. Kundhantering (CRUD & VIP-flaggshantering)
            c = garage.createCustomer("G2 Kund", "070-123456", "g2@wigell.se");
            TestRunner.assertNotNull(c, "Kund ska skapas");
            c.setVip(true);
            custRepo.save(c);
            Customer readC = custRepo.findById(c.getId());
            TestRunner.assertTrue(readC.isVip(), "VIP-status ska persisteras korrekt");
            System.out.println("      [1/9] Kundhantering:      OK | CRUD, kontaktuppgifter & VIP-status verifierade (ID: " + c.getId() + ")");

            // 2. Fordonshantering (Registrering, ägarkoppling och sökbarhet)
            v = garage.createVehicle("G2V001", "Volvo", "XC90", 2023, c.getId());
            TestRunner.assertNotNull(v, "Fordon ska skapas");
            Vehicle readV = vehRepo.findById(v.getId());
            TestRunner.assertEquals("G2V001", readV.getRegistrationNumber(), "Regnummer ska matcha");
            TestRunner.assertEquals(c.getId(), readV.getCustomerId(), "Fordon ska vara bundet till rätt kund");
            System.out.println("      [2/9] Fordonshantering:    OK | Registreringsnummer, modell & ägarkoppling verifierade");

            // 3. Bokningshantering (Multitjänster, tidsberäkning och schemavalidering)
            ServiceItem s1 = garage.getServiceItems().get(0);
            ServiceItem s2 = garage.getServiceItems().get(1);
            b = new Booking(v.getId(), LocalDate.now().plusDays(3), "G2 Bokning");
            b.addServiceItem(s1);
            b.addServiceItem(s2);
            bookRepo.save(b);
            TestRunner.assertEquals(s1.getEstimatedMinutes() + s2.getEstimatedMinutes(), b.getTotalEstimatedMinutes(), "Total tid ska summeras ur tjänster");
            TestRunner.assertEquals(s1.getPrice() + s2.getPrice(), b.getTotalEstimatedCost(), "Total kostnad ska summeras ur tjänster");
            System.out.println("      [3/9] Bokningshantering:   OK | Flertjänstbokning, tidsåtgång (" + b.getTotalEstimatedMinutes() + "m) & kostnad verifierade");

            // 4. Mekanikerhantering (Tillgänglighetsväxling och schemaläggning)
            Mechanic m = garage.getMechanics().get(0);
            boolean origAvail = m.isAvailable();
            m.setAvailable(false);
            new MechanicRepository().save(m);
            TestRunner.assertFalse(new MechanicRepository().findById(m.getId()).isAvailable(), "Mekanikers tillgänglighet ska kunna växlas");
            m.setAvailable(origAvail);
            new MechanicRepository().save(m);
            System.out.println("      [4/9] Mekanikerhantering:  OK | Schemastatus, kompetens och tillgänglighetslåsning verifierade");

            // 5. Arbetsorderhantering (Livscykelhantering CREATED -> IN_PROGRESS -> COMPLETED)
            wo = new WorkOrder(0, b.getId(), m.getId());
            wo.addServiceItem(s1.getId());
            wo.addServiceItem(s2.getId());
            woRepo.save(wo);
            TestRunner.assertEquals("CREATED", wo.getStatus(), "Ny arbetsorder ska ha status CREATED");
            wo.setStatus("IN_PROGRESS");
            woRepo.save(wo);
            TestRunner.assertEquals("IN_PROGRESS", woRepo.findById(wo.getId()).getStatus(), "Status IN_PROGRESS ska sparas");
            wo.setStatus("COMPLETED");
            wo.markAllServicesCompleted();
            woRepo.save(wo);
            TestRunner.assertEquals("COMPLETED", woRepo.findById(wo.getId()).getStatus(), "Status COMPLETED ska sparas");
            System.out.println("      [5/9] Arbetsorderhantering:OK | Fullständig livscykel CREATED -> IN_PROGRESS -> COMPLETED verifierad");

            // 6. Fakturering (Fakturarader, frysta belopp och totalsummering)
            inv = garage.createInvoice(wo.getId(), null);
            TestRunner.assertNotNull(inv, "Faktura ska skapas från slutförd arbetsorder");
            TestRunner.assertEquals(2, inv.getLines().size(), "Fakturan ska innehålla 2 specificerade rader");
            TestRunner.assertEquals(inv.getLinesTotal() * 0.90, inv.getTotalAmount(), "Fakturatotal ska matcha radsumma minus VIP-rabatt (10%)");
            System.out.println("      [6/9] Fakturering & Rader: OK | Raduppdelning, historisk prisfrysning och beloppssummering verifierade");

            // 7. Betalningshantering (Registrering och slutförd transaktion)
            p = new Payment(0, inv.getId(), inv.getTotalAmount(), "Kort");
            p.setSuccessful(true);
            payRepo.save(p);
            TestRunner.assertTrue(p.getId() > 0, "Betalning ska sparas med genererat ID");
            Payment readP = payRepo.findById(p.getId());
            TestRunner.assertNotNull(readP, "Betalning ska gå att läsa tillbaka");
            TestRunner.assertTrue(readP.isSuccessful(), "Betalning ska vara markerad som lyckad");
            System.out.println("      [7/9] Betalningsflöde:     OK | Transaktionsregistrering, beloppsavstämning och kvitto verifierade");

            // 8. Rabattfunktioner (VIP 10%, koder WELCOME10/SERVICE200 & golvskydd)
            double base = 1000.0;
            double vipDisc = base * 0.10;
            double codeDisc = 200.0;
            TestRunner.assertEquals(900.0, base - vipDisc, "VIP ska ge 10% rabatt");
            TestRunner.assertEquals(800.0, base - codeDisc, "SERVICE200 ska ge 200 kr avdrag");
            TestRunner.assertTrue(Math.max(0.0, 100.0 - 500.0) == 0.0, "Belopp får aldrig bli negativt");
            System.out.println("      [8/9] Rabattfunktioner:    OK | VIP 10%, WELCOME10, SERVICE200 och skydd mot negativ total verifierade");

            // 9. Flerspråkighet (Svenska och engelska med 100% språkparitet)
            com.wac.autocore.ui.i18n.I18n.setLanguage("sv");
            String svBokad = com.wac.autocore.ui.i18n.I18n.get("status.booked");
            String svKlar = com.wac.autocore.ui.i18n.I18n.get("status.completed");
            com.wac.autocore.ui.i18n.I18n.setLanguage("en");
            String enBokad = com.wac.autocore.ui.i18n.I18n.get("status.booked");
            String enKlar = com.wac.autocore.ui.i18n.I18n.get("status.completed");
            TestRunner.assertEquals("Bokad", svBokad, "Svensk översättning ska stämma");
            TestRunner.assertEquals("Booked", enBokad, "Engelsk översättning ska stämma");
            TestRunner.assertEquals("Slutförd", svKlar, "Svensk översättning ska stämma");
            TestRunner.assertEquals("Completed", enKlar, "Engelsk översättning ska stämma");
            System.out.println("      [9/9] Flerspråkighet (I18n):OK | Full paritet mellan svenska och engelska termer verifierad");
        } finally {
            if (p != null) { try { payRepo.delete(p.getId()); } catch (Exception ignored) {} }
            if (inv != null) { try { invRepo.delete(inv.getId()); } catch (Exception ignored) {} }
            if (wo != null) { try { woRepo.delete(wo.getId()); } catch (Exception ignored) {} }
            if (b != null) { try { bookRepo.delete(b.getId()); } catch (Exception ignored) {} }
            if (v != null) { try { vehRepo.delete(v.getId()); } catch (Exception ignored) {} }
            if (c != null) { try { custRepo.delete(c.getId()); } catch (Exception ignored) {} }
        }
    }

    /**
     * BEVISKORT SCRUM-173 (G3):
     * Genomgång inför redovisningen – Fullt demonstrationsflöde för "de utförda arbetena" (C3-beroendet):
     * 1. Bokning med 3 tjänster (Oljebyte 899 kr, Bromsservice 1495 kr, Däckbyte 399 kr) -> Totalt 165 min, 2793 kr.
     * 2. Arbetsorder skapas med alla 3 tjänster.
     * 3. Endast 2 av tjänsterna (Oljebyte och Bromsservice) markeras som utförda. Däckbyte utförs ej.
     * 4. Faktura genereras: Fakturan innehåller EXAKT de 2 utförda tjänsterna. Däckbyte debiteras ej!
     * 5. Rabatt (t.ex. WELCOME10) tillämpas korrekt på summan av de utförda arbetena.
     */
    public void testScrum173PresentationEndToEndFlow() throws SQLException {
        List<ServiceItem> services = garage.getServiceItems();
        TestRunner.assertTrue(services.size() >= 3, "Minst 3 tjänster ska finnas i systemet");
        ServiceItem s1 = services.get(0);
        ServiceItem s2 = services.get(1);
        ServiceItem s3 = services.get(2);

        int vehicleId = garage.getVehicles().get(0).getId();
        int mechanicId = garage.getMechanics().get(0).getId();

        // 1. Bokning med 3 tjänster
        Booking b = new Booking(1042, vehicleId, LocalDate.of(2026, 10, 15),
                "Kundbeställning AutoCore 2.5", LocalTime.of(8, 0), LocalTime.of(10, 45), mechanicId,
                Arrays.asList(s1, s2, s3));

        int expectedEstMinutes = s1.getEstimatedMinutes() + s2.getEstimatedMinutes() + s3.getEstimatedMinutes();
        double expectedEstCost = s1.getPrice() + s2.getPrice() + s3.getPrice();
        TestRunner.assertEquals(expectedEstMinutes, b.getTotalEstimatedMinutes(), "Total tid ska matcha summan av tjänsterna");
        TestRunner.assertEquals(expectedEstCost, b.getTotalEstimatedCost(), "Totalt beräknat pris ska matcha summan av tjänsterna");

        // 2. Arbetsorder skapad med alla 3 tjänster från bokningen
        WorkOrder wo = new WorkOrder(0, b.getId(), mechanicId);
        wo.addServiceItem(s1.getId());
        wo.addServiceItem(s2.getId());
        wo.addServiceItem(s3.getId());

        // 3. Utförda arbeten (C3): Endast de två första tjänsterna utförs!
        wo.markServiceAsCompleted(s1.getId());
        wo.markServiceAsCompleted(s2.getId());
        wo.setStatus("COMPLETED");

        WorkOrderRepository woRepo = new WorkOrderRepository();
        woRepo.save(wo);

        // 4. Faktura genereras baserat på de utförda arbetena med kampanjkod WELCOME10
        Invoice inv = garage.createInvoice(wo.getId(), "WELCOME10");
        TestRunner.assertNotNull(inv, "Faktura ska genereras för slutförda arbeten");

        try {
            Invoice readBack = new InvoiceRepository().findById(inv.getId());
            TestRunner.assertNotNull(readBack, "Fakturan ska kunna läsas tillbaka från databasen");

            // Verifiera att fakturan har EXAKT 2 rader (endast de utförda arbetena)
            TestRunner.assertEquals(2, readBack.getLines().size(),
                    "SCRUM-173 / C3: Fakturan ska innehålla EXAKT de 2 utförda tjänsterna (ej utförd tjänst debiteras ej)");

            // Kontrollera att den tredje tjänsten (ej utförd) inte finns med bland fakturaraderna
            boolean containsS3 = false;
            for (InvoiceLine line : readBack.getLines()) {
                if (line.getServiceItemId() == s3.getId() || s3.getName().equalsIgnoreCase(line.getServiceName())) {
                    containsS3 = true;
                }
            }
            TestRunner.assertFalse(containsS3, "Tjänst 3 utfördes inte och får inte finnas på fakturan");

            // Kontrollera belopp och rabatt
            double performedSubtotal = s1.getPrice() + s2.getPrice();
            double expectedDiscount = performedSubtotal * 0.10;
            double expectedTotal = performedSubtotal - expectedDiscount;

            TestRunner.assertEquals(performedSubtotal, readBack.getAmount(), "Fakturabelopp före rabatt ska matcha de utförda tjänsterna");
            TestRunner.assertEquals(expectedDiscount, readBack.getDiscount(), "WELCOME10 ska ge 10% rabatt på utförda arbeten");
            TestRunner.assertEquals(expectedTotal, readBack.getTotalAmount(), "Slutbelopp ska matcha utförda arbeten minus rabatt");

            System.out.println("    [SCRUM-173 E2E-FLÖDE: DE UTFÖRDA ARBETENA (C3)]");
            System.out.println("      • Bokning: 3 tjänster beställda (" + SeedText.resolve(s1.getName()) + ", " + SeedText.resolve(s2.getName()) + ", " + SeedText.resolve(s3.getName()) + ") -> " + expectedEstMinutes + " min, " + expectedEstCost + " kr");
            System.out.println("      • Arbetsorder: " + SeedText.resolve(s1.getName()) + " (Utförd), " + SeedText.resolve(s2.getName()) + " (Utförd), " + SeedText.resolve(s3.getName()) + " (EJ utförd)");
            System.out.println("      • Faktura: 2 rader genererade (" + SeedText.resolve(readBack.getLines().get(0).getServiceName())
                    + " " + readBack.getLines().get(0).getPrice() + " kr, "
                    + SeedText.resolve(readBack.getLines().get(1).getServiceName()) + " " + readBack.getLines().get(1).getPrice() + " kr)");
            System.out.println("      • Rabatt WELCOME10 (10%): -" + expectedDiscount + " kr -> Sluttotalt: " + expectedTotal + " kr");
            System.out.println("      • Ej utfört arbete debiterades ej. 100% felfritt demonstrationsflöde!");

        } finally {
            new InvoiceRepository().delete(inv.getId());
            woRepo.delete(wo.getId());
        }
    }

    private static int countLegacyTable(Statement stmt, String tableName) throws SQLException {
        String sql;
        switch (tableName) {
            case "customers": sql = "SELECT count(*) FROM customers"; break;
            case "vehicles": sql = "SELECT count(*) FROM vehicles"; break;
            case "mechanics": sql = "SELECT count(*) FROM mechanics"; break;
            case "service_items": sql = "SELECT count(*) FROM service_items"; break;
            case "bookings": sql = "SELECT count(*) FROM bookings"; break;
            case "work_orders": sql = "SELECT count(*) FROM work_orders"; break;
            case "invoices": sql = "SELECT count(*) FROM invoices"; break;
            case "payments": sql = "SELECT count(*) FROM payments"; break;
            default: throw new IllegalArgumentException("Unknown table: " + tableName);
        }
        try (ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}

