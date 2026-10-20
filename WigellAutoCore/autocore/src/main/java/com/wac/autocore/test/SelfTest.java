package com.wac.autocore.test;

import com.wac.autocore.config.DatabasePath;
import com.wac.autocore.data.Db;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.SqliteRepositoryFactory;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.util.Texts;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One run over the nine areas the order lists, against a database of its own. The real file is
 * looked at before and after, and must be the same. Exit 0: all areas hold. Exit 1: an area fell
 * or the real file changed. Exit 2: the proof could not even start.
 */
public final class SelfTest {

    /** One area's check: says what it saw, or throws when the area does not hold. */
    interface Check {
        String run(GarageSystem garage) throws Exception;
    }

    private SelfTest() {}

    public static void main(String[] args) throws Exception {
        // The file the program uses when nobody says otherwise, the one that must be left alone.
        File real = new File(DatabasePath.resolve(new String[0]));
        String before = fingerprint(real);

        File database = File.createTempFile("selftest-", ".db");
        database.deleteOnExit();
        // Nothing to copy: the program builds the database itself.
        database.delete();

        GarageSystem garage;
        try {
            garage = new GarageSystem(new SqliteRepositoryFactory(new Db(database.getAbsolutePath())));
        } catch (Exception cannotStart) {
            // Not an area that failed but a proof that never ran, so it gets an answer of its own.
            System.out.println("Självprovet kunde inte starta: " + cannotStart);
            System.exit(2);
            return;
        }

        Map<String, Check> areas = new LinkedHashMap<String, Check>();
        areas.put("kunder", SelfTest::customers);
        areas.put("fordon", SelfTest::vehicles);
        areas.put("mekaniker", SelfTest::mechanics);
        areas.put("bokningar", SelfTest::bookings);
        areas.put("arbetsordrar", SelfTest::workOrders);
        areas.put("fakturering", SelfTest::invoicing);
        areas.put("rabatter", SelfTest::discounts);
        areas.put("betalningar", SelfTest::payments);
        areas.put("svenska och engelska", SelfTest::languages);

        int failed = 0;
        for (Map.Entry<String, Check> area : areas.entrySet()) {
            // Each area is tried on its own, so one that falls does not hide the rest.
            try {
                System.out.println("OK   " + area.getKey() + ": " + area.getValue().run(garage));
            } catch (Throwable fault) {
                failed++;
                System.out.println("FEL  " + area.getKey() + ": " + fault);
            }
        }

        String after = fingerprint(real);
        boolean untouched = before.equals(after);

        System.out.println();
        System.out.println("riktiga filen (" + real.getPath() + "): " + before + " -> " + after);
        System.out.println("områden som klarade sig: " + (areas.size() - failed) + " av " + areas.size());
        System.out.println("riktiga filen orörd: " + untouched);
        System.exit(failed == 0 && untouched ? 0 : 1);
    }

    private static String customers(GarageSystem garage) {
        Customer made = garage.createCustomer("Prov Kund", "0701234567", "prov@example.com");
        must(garage.findCustomer(made.getId()) != null, "kunden gick inte att hitta");
        return "skapad och hittad, nummer " + made.getId();
    }

    private static String vehicles(GarageSystem garage) {
        int owner = garage.getCustomers().get(0).getId();
        Vehicle made = garage.createVehicle("PRV123", "Volvo", "V70", 2015, owner);
        must(garage.findVehicle(made.getId()) != null, "fordonet gick inte att hitta");
        return "skapat och hittat, " + made.getRegistrationNumber();
    }

    private static String mechanics(GarageSystem garage) {
        Mechanic made = garage.createMechanic("Prov Mekaniker", "0709876543", "");
        must(garage.findMechanic(made.getId()) != null, "mekanikern gick inte att hitta");
        return "skapad och hittad, nummer " + made.getId();
    }

    private static String bookings(GarageSystem garage) {
        Booking made = book(garage, LocalDate.of(2027, 3, 1));
        must(garage.findBooking(made.getId()) != null, "bokningen gick inte att hitta");
        return "skapad " + made.getDate() + " kl " + made.getStartTime();
    }

    private static String workOrders(GarageSystem garage) {
        WorkOrder order = completedOrder(garage, LocalDate.of(2027, 3, 2));
        return "skapad, bekräftad, startad och slutförd, nummer " + order.getId();
    }

    private static String invoicing(GarageSystem garage) {
        Invoice invoice = garage.createInvoice(completedOrder(garage, LocalDate.of(2027, 3, 3)).getId(), "");
        must(invoice.getAmount() > 0, "fakturan saknar belopp");
        return "faktura " + invoice.getId() + " på " + invoice.getAmount() + " öre, "
                + invoice.getLines().size() + " rader";
    }

    private static String discounts(GarageSystem garage) {
        Invoice invoice = garage.createInvoice(completedOrder(garage, LocalDate.of(2027, 3, 4)).getId(), "WELCOME10");
        int expected = (invoice.getAmount() * 10 + 50) / 100;
        must(invoice.getDiscount() == expected, "rabatten blev " + invoice.getDiscount() + ", inte " + expected);
        return "WELCOME10 gav " + invoice.getDiscount() + " öre av " + invoice.getAmount();
    }

    private static String payments(GarageSystem garage) {
        Invoice invoice = garage.createInvoice(completedOrder(garage, LocalDate.of(2027, 3, 5)).getId(), "");
        Payment payment = garage.processPayment(invoice.getId(), "CARD");
        must(payment != null && payment.isSuccessful(), "kortbetalningen gick inte igenom");
        must(garage.findInvoice(invoice.getId()).isPaid(), "fakturan står inte som betald");
        return "kort på " + payment.getAmount() + " öre, fakturan betald";
    }

    private static String languages(GarageSystem garage) {
        Map<String, String> sv = Texts.load(Texts.LANG_SV);
        Map<String, String> en = Texts.load(Texts.LANG_EN);
        must(!sv.isEmpty() && sv.keySet().equals(en.keySet()),
                "nycklarna skiljer: sv " + sv.size() + ", en " + en.size());
        return sv.size() + " nycklar, samma på båda språken";
    }

    /** A booking on its own weekday at ten, so no two areas share a time. */
    private static Booking book(GarageSystem garage, LocalDate day) {
        int vehicleId = garage.getVehicles().get(0).getId();
        Mechanic mechanic = garage.getMechanics().get(0);
        ServiceItem service = garage.getServiceItems().get(0);
        return garage.createBooking(vehicleId, day, "Självprov", LocalTime.of(10, 0),
                mechanic.getId(), service.getId());
    }

    /** A booking taken the whole way: work order, confirmed, started and completed. */
    private static WorkOrder completedOrder(GarageSystem garage, LocalDate day) {
        Booking booking = book(garage, day);
        WorkOrder order = garage.createWorkOrder(booking.getId(), booking.getMechanicId(),
                booking.getServiceItemIds());
        garage.confirmWorkOrder(order.getId());
        garage.startWorkOrder(order.getId());
        garage.completeWorkOrder(order.getId());
        must(garage.findWorkOrder(order.getId()).getStatus() == WorkOrderStatus.COMPLETED,
                "ordern blev inte slutförd");
        return order;
    }

    /** Size and time of last change, or that the file is not there. Equal before and after means untouched. */
    private static String fingerprint(File file) {
        return file.exists() ? file.length() + " byte, ändrad " + file.lastModified() : "finns inte";
    }

    private static void must(boolean holds, String otherwise) {
        if (!holds) {
            throw new IllegalStateException(otherwise);
        }
    }
}